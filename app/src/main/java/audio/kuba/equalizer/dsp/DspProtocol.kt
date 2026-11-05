package audio.kuba.equalizer.dsp

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Motor de processamento matemático e montador de pacotes para o DSP Walkplay.
 */
object DspProtocol {

    /**
     * Calcula coeficientes do filtro Peaking.
     */
    fun peak(f: Double, g: Double, q: Double, fs: Double = DspConstants.SAMPLING_RATE_FS): BiquadCoeffs {
        val a = 10.0.pow(g / 40.0)
        val w = 2.0 * Math.PI * f / fs
        val al = sin(w) / (2.0 * q)
        val a0 = 1.0 + al / a
        val b0 = (1.0 + al * a) / a0
        val b1 = -2.0 * cos(w) / a0
        val b2 = (1.0 - al * a) / a0
        val a1 = b1
        val a2 = (1.0 - al / a) / a0
        return BiquadCoeffs(b0, b1, b2, a1, a2)
    }

    /**
     * Calcula coeficientes do filtro Low Shelf.
     */
    fun lowShelf(f: Double, g: Double, q: Double, fs: Double = DspConstants.SAMPLING_RATE_FS): BiquadCoeffs {
        val a = 10.0.pow(g / 40.0)
        val w = 2.0 * Math.PI * f / fs
        val al = sin(w) / (2.0 * q)
        val c = cos(w)
        val s = 2.0 * sqrt(a) * al
        val a0 = (a + 1.0) + (a - 1.0) * c + s
        val b0 = a * ((a + 1.0) - (a - 1.0) * c + s) / a0
        val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * c) / a0
        val b2 = a * ((a + 1.0) - (a - 1.0) * c - s) / a0
        val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * c) / a0
        val a2 = ((a + 1.0) + (a - 1.0) * c - s) / a0
        return BiquadCoeffs(b0, b1, b2, a1, a2)
    }

    fun getCoefficients(band: FilterBand): BiquadCoeffs {
        return when (band.type) {
            FilterType.LOW_SHELF -> lowShelf(band.frequency, band.gainDb, band.q)
            FilterType.PEAK -> peak(band.frequency, band.gainDb, band.q)
        }
    }

    /**
     * Algoritmo CRC16 utilizado pelo firmware (polinômio 0x1021, seed 0xFFFF).
     */
    fun crc16(bytes: ByteArray): Int {
        var c = 0xFFFF
        for (b in bytes) {
            val x = b.toInt() and 0xFF
            c = c xor (x shl 8)
            for (i in 0 until 8) {
                c = if ((c and 0x8000) != 0) {
                    ((c shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (c shl 1) and 0xFFFF
                }
            }
        }
        return c and 0xFFFF
    }

    /**
     * Constrói o blob binário contínuo de 220 bytes contendo cabeçalho, pré-amp, coeficientes e CRC.
     */
    fun buildBlob(bands: List<FilterBand>, preDb: Double): ByteArray {
        require(bands.size == DspConstants.BANDS_COUNT) {
            "São necessárias exatamente ${DspConstants.BANDS_COUNT} bandas de equalização."
        }

        val buffer = ByteBuffer.allocate(DspConstants.BLOB_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        // 1. Cabeçalho fixo (14 bytes)
        buffer.put(DspConstants.HEADER)

        // 2. Pré-amplificação linear em ponto fixo Q23 (4 bytes)
        val linearPre = 10.0.pow(preDb / 20.0)
        val preInt = (linearPre * 8388608.0).roundToLong().toInt()
        buffer.putInt(preInt)

        // 3. 10 Bandas (5 coeficientes de 32-bit por banda = 200 bytes)
        // Ordem dos coeficientes por banda: [b0, b2, -a1, b1, -a2]
        for (band in bands) {
            val c = getCoefficients(band)
            val coeffValues = doubleArrayOf(c.b0, c.b2, -c.a1, c.b1, -c.a2)
            for (v in coeffValues) {
                val n = (v * 134217728.0).roundToLong()
                if (n > Int.MAX_VALUE || n < Int.MIN_VALUE) {
                    throw IllegalArgumentException("Filtro fora da faixa de representação numérica do DSP")
                }
                buffer.putInt(n.toInt())
            }
        }

        // 4. Cálculo do CRC16 sobre os 218 bytes anteriores
        val rawBytes = buffer.array()
        val bytesForCrc = ByteArray(218)
        System.arraycopy(rawBytes, 0, bytesForCrc, 0, 218)
        val crc = crc16(bytesForCrc)

        // Append CRC16 em Little Endian (2 bytes)
        buffer.put((crc and 0xFF).toByte())
        buffer.put(((crc shr 8) and 0xFF).toByte())

        return buffer.array()
    }

    /**
     * Fragmenta o blob em 9 pacotes de 31 bytes com índice do pacote no byte 0.
     */
    fun buildPackets(bands: List<FilterBand>, preDb: Double): List<ByteArray> {
        val blob = buildBlob(bands, preDb)
        val packets = ArrayList<ByteArray>(DspConstants.TOTAL_PACKETS)

        for (i in 0 until DspConstants.TOTAL_PACKETS) {
            val packet = ByteArray(DspConstants.PACKET_SIZE)
            packet[0] = i.toByte()

            val start = i * 30
            val available = (blob.size - start).coerceAtLeast(0)
            val chunkLen = available.coerceAtMost(30)

            if (chunkLen > 0) {
                System.arraycopy(blob, start, packet, 1, chunkLen)
            }
            packets.add(packet)
        }

        return packets
    }

    /**
     * Calcula o ganho em dB de resposta de frequência para um ponto em Hz.
     * Utilizado para traçar a curva de EQ na interface visual.
     */
    fun calculateResponseDb(bands: List<FilterBand>, f: Double): Double {
        var db = 0.0
        val w = 2.0 * Math.PI * f / DspConstants.SAMPLING_RATE_FS
        val c1 = cos(w)
        val s1 = sin(w)
        val c2 = cos(2.0 * w)
        val s2 = sin(2.0 * w)

        for (b in bands) {
            val k = getCoefficients(b)
            val nr = k.b0 + k.b1 * c1 + k.b2 * c2
            val ni = -(k.b1 * s1 + k.b2 * s2)
            val dr = 1.0 + k.a1 * c1 + k.a2 * c2
            val di = -(k.a1 * s1 + k.a2 * s2)

            val numerator = nr * nr + ni * ni
            val denominator = dr * dr + di * di
            if (denominator > 0.0) {
                db += 10.0 * log10(numerator / denominator)
            }
        }
        return db
    }
}

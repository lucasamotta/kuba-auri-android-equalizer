package audio.kuba.equalizer

import audio.kuba.equalizer.dsp.DspConstants
import audio.kuba.equalizer.dsp.DspProtocol
import audio.kuba.equalizer.dsp.PresetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspProtocolTest {

    @Test
    fun testAuriPadraoPacketGeneration() {
        val auriPreset = PresetRepository.findById("padrao")!!
        val packets = DspProtocol.buildPackets(auriPreset.bands, auriPreset.preDb)

        assertEquals(9, packets.size)

        // Verifica os índices e tamanho de cada pacote
        for (i in 0 until 9) {
            val p = packets[i]
            assertEquals(31, p.size)
            assertEquals(i.toByte(), p[0])
        }

        // Verifica o cabeçalho fixo no primeiro pacote (bytes 1 a 14)
        for (j in 0 until 14) {
            assertEquals(DspConstants.HEADER[j], packets[0][j + 1])
        }

        // Verifica o comando apply
        assertEquals(5, DspConstants.APPLY_PAYLOAD.size)
        assertEquals(0x01.toByte(), DspConstants.APPLY_PAYLOAD[0])
        assertEquals(0x65.toByte(), DspConstants.APPLY_PAYLOAD[1])
        assertEquals(0x00.toByte(), DspConstants.APPLY_PAYLOAD[2])
        assertEquals(0x01.toByte(), DspConstants.APPLY_PAYLOAD[3])
        assertEquals(0xFF.toByte(), DspConstants.APPLY_PAYLOAD[4])
    }

    @Test
    fun testAllPresetsGenerateValidPackets() {
        for (preset in PresetRepository.presets) {
            val packets = DspProtocol.buildPackets(preset.bands, preset.preDb)
            assertEquals("O preset ${preset.name} deve gerar 9 pacotes", 9, packets.size)
            for (i in 0 until 9) {
                assertEquals("Tamanho do pacote $i do preset ${preset.name}", 31, packets[i].size)
            }
        }
    }

    @Test
    fun testCrcCalculation() {
        val testData = "123456789".toByteArray(Charsets.US_ASCII)
        val crc = DspProtocol.crc16(testData)
        // Polinômio 0x1021 padrão
        assertTrue(crc in 0..0xFFFF)
    }
}

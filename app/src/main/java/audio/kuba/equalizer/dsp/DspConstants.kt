package audio.kuba.equalizer.dsp

/**
 * Constantes de protocolo e hardware do cabo DSP Kuba Auri (TTGK USB DSP Audio / Walkplay).
 */
object DspConstants {
    const val VENDOR_ID = 0x3302
    const val PRODUCT_ID = 0x8902
    const val REPORT_ID: Byte = 0xCB.toByte()
    const val SAMPLING_RATE_FS: Double = 96000.0

    /**
     * Cabeçalho fixo de 14 bytes: "EQ1#" + configurações internas do DSP Walkplay.
     */
    val HEADER: ByteArray = byteArrayOf(
        0x45, 0x51, 0x31, 0x23, 0xD6.toByte(), 0x00, 0x00, 0x0A,
        0x04, 0x05, 0x01, 0x0A, 0x00, 0x00
    )

    /**
     * Comando de gravação / aplicação em flash/DSP (5 bytes).
     */
    val APPLY_PAYLOAD: ByteArray = byteArrayOf(
        0x01, 0x65, 0x00, 0x01, 0xFF.toByte()
    )

    const val DELAY_BETWEEN_PACKETS_MS = 16L
    const val DELAY_BEFORE_APPLY_MS = 150L

    const val TOTAL_PACKETS = 9
    const val PACKET_SIZE = 31
    const val BLOB_SIZE = 220
    const val BANDS_COUNT = 10
}

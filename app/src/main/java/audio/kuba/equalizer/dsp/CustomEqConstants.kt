package audio.kuba.equalizer.dsp

import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary
import androidx.compose.ui.graphics.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Frequências padrão ISO para o equalizador paramétrico de 10 bandas do Kuba Auri.
 * O hardware DSP Walkplay suporta exatamente 10 bandas biquad.
 */
object CustomEqConstants {
    val ISO_FREQUENCIES = doubleArrayOf(
        31.5,
        63.0,
        125.0,
        250.0,
        500.0,
        1000.0,
        2000.0,
        4000.0,
        8000.0,
        16000.0
    )

    val FREQUENCY_LABELS = listOf(
        "31",
        "63",
        "125",
        "250",
        "500",
        "1k",
        "2k",
        "4k",
        "8k",
        "16k"
    )

    // Fator Q padrão para equalizador gráfico de 1 oitava (10 bandas cobrindo 20Hz-20kHz)
    const val DEFAULT_Q = 1.414

    const val MIN_GAIN_DB = -12.0
    const val MAX_GAIN_DB = 12.0

    /**
     * Cria 10 bandas paramétricas para os ganhos especificados.
     * Primeira banda como LowShelf para controle natural de sub-graves,
     * e as 9 seguintes como filtros Peaking (PK).
     */
    fun createBands(gains: DoubleArray): List<FilterBand> {
        require(gains.size == 10) { "São necessários exatamente 10 ganhos de frequência." }
        return listOf(
            FilterBand.lowShelf(ISO_FREQUENCIES[0], gains[0], 0.707),
            FilterBand.peak(ISO_FREQUENCIES[1], gains[1], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[2], gains[2], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[3], gains[3], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[4], gains[4], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[5], gains[5], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[6], gains[6], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[7], gains[7], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[8], gains[8], DEFAULT_Q),
            FilterBand.peak(ISO_FREQUENCIES[9], gains[9], 0.707)
        )
    }

    /**
     * Calcula o pré-amp de segurança para evitar clipping digital no DSP.
     * Reduz o volume se algum ganho for positivo.
     */
    fun calculateAutoPreDb(gains: DoubleArray): Double {
        val maxGain = gains.maxOrNull() ?: 0.0
        return if (maxGain > 0.0) {
            -maxGain.coerceAtMost(10.0)
        } else {
            0.0
        }
    }
}

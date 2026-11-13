package audio.kuba.equalizer.dsp

enum class FilterType(val label: String, val shortName: String, val hasGain: Boolean) {
    PEAK("Sino (Peaking)", "PK", true),
    LOW_SHELF("Low Shelf", "LS", true),
    HIGH_SHELF("High Shelf", "HS", true),
    LOW_PASS("Passa-Baixa (LP)", "LP", false),
    HIGH_PASS("Passa-Alta (HP)", "HP", false),
    NOTCH("Rejeita-Faixa (Notch)", "NC", false)
}

/**
 * Representa uma banda de equalização paramétrica.
 * @param type Tipo de filtro (PEAK, LOW_SHELF, HIGH_SHELF, LOW_PASS, HIGH_PASS, NOTCH).
 * @param frequency Frequência central / corte em Hertz.
 * @param gainDb Ganho da banda em decibéis.
 * @param q Fator de qualidade Q do filtro.
 * @param enabled Se o filtro está ativo ou em bypass (flat).
 */
data class FilterBand(
    val type: FilterType,
    val frequency: Double,
    val gainDb: Double,
    val q: Double,
    val enabled: Boolean = true
) {
    companion object {
        fun peak(f: Double, g: Double, q: Double, enabled: Boolean = true) = FilterBand(FilterType.PEAK, f, g, q, enabled)
        fun lowShelf(f: Double, g: Double, q: Double, enabled: Boolean = true) = FilterBand(FilterType.LOW_SHELF, f, g, q, enabled)
        fun highShelf(f: Double, g: Double, q: Double, enabled: Boolean = true) = FilterBand(FilterType.HIGH_SHELF, f, g, q, enabled)
        fun lowPass(f: Double, q: Double, enabled: Boolean = true) = FilterBand(FilterType.LOW_PASS, f, 0.0, q, enabled)
        fun highPass(f: Double, q: Double, enabled: Boolean = true) = FilterBand(FilterType.HIGH_PASS, f, 0.0, q, enabled)
        fun notch(f: Double, q: Double, enabled: Boolean = true) = FilterBand(FilterType.NOTCH, f, 0.0, q, enabled)
    }
}

/**
 * Coeficientes do filtro biquad normalizados (onde a0 = 1).
 */
data class BiquadCoeffs(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double
)

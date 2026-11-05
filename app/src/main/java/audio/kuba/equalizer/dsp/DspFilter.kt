package audio.kuba.equalizer.dsp

enum class FilterType {
    PEAK,
    LOW_SHELF
}

/**
 * Representa uma banda de equalização paramétrica.
 * @param type Tipo de filtro (PEAK ou LOW_SHELF).
 * @param frequency Frequência central / corte em Hertz.
 * @param gainDb Ganho da banda em decibéis.
 * @param q Fator de qualidade Q do filtro.
 */
data class FilterBand(
    val type: FilterType,
    val frequency: Double,
    val gainDb: Double,
    val q: Double
) {
    companion object {
        fun peak(f: Double, g: Double, q: Double) = FilterBand(FilterType.PEAK, f, g, q)
        fun lowShelf(f: Double, g: Double, q: Double) = FilterBand(FilterType.LOW_SHELF, f, g, q)
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

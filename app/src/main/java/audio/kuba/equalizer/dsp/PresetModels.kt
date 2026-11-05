package audio.kuba.equalizer.dsp

enum class PresetCategory(val title: String) {
    PREFERENCE("Curvas de preferência"),
    REFERENCE("Curvas de referência"),
    CUSTOM("Curvas personalizadas")
}

data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val category: PresetCategory,
    val target: String,
    val preDb: Double,
    val bands: List<FilterBand>,
    val showBands: List<FilterBand>? = null
)

object PresetRepository {

    private fun pk(f: Double, g: Double, q: Double) = FilterBand.peak(f, g, q)
    private fun ls(f: Double, g: Double, q: Double) = FilterBand.lowShelf(f, g, q)

    val presets: List<Preset> = listOf(
        // === Curvas de Preferência ===
        Preset(
            id = "padrao",
            name = "Auri",
            description = "A assinatura do Auri, inspirada pela comunidade.",
            category = PresetCategory.PREFERENCE,
            target = "JM-1",
            preDb = -2.8,
            bands = listOf(
                pk(25.0, -0.8, 0.3),
                pk(150.0, -3.0, 0.69),
                pk(2237.0, -10.0, 1.41),
                pk(4348.0, -6.1, 3.0),
                pk(4958.0, -8.3, 2.72),
                pk(7178.0, -10.0, 3.0),
                pk(11063.0, -10.0, 3.0),
                pk(520.0, 3.1, 0.81),
                pk(3046.0, 5.7, 1.51),
                pk(5995.0, 3.4, 0.59)
            )
        ),
        Preset(
            id = "pref6",
            name = "Natural",
            description = "Subgrave presente, médios limpos e agudos arejados.",
            category = PresetCategory.PREFERENCE,
            target = "JM-1",
            preDb = -3.0,
            showBands = listOf(
                pk(25.0, -2.9, 0.5),
                pk(157.0, -5.2, 0.5),
                pk(1077.0, -1.5, 2.47),
                pk(1837.0, -4.3, 1.67),
                pk(2272.0, -7.3, 2.05),
                pk(4326.0, -8.0, 2.73),
                pk(4970.0, -7.5, 3.0),
                pk(7130.0, -7.3, 3.0),
                pk(424.0, 3.9, 0.54),
                pk(3334.0, 7.0, 0.78)
            ),
            bands = listOf(
                pk(25.0, -2.0, 0.4),
                pk(167.0, -6.2, 0.53),
                pk(2560.0, -8.0, 1.03),
                pk(4911.0, -8.0, 4.0),
                pk(7838.0, -6.7, 4.0),
                pk(11523.0, -6.5, 4.0),
                pk(270.0, 0.1, 4.0),
                pk(404.0, 4.1, 0.48),
                pk(2930.0, 7.0, 2.94),
                pk(3537.0, 4.5, 4.0)
            )
        ),
        Preset(
            id = "emv",
            name = "Em V",
            description = "Alvo com graves e agudos levantados.",
            category = PresetCategory.PREFERENCE,
            target = "JM-1",
            preDb = -7.8,
            bands = listOf(
                pk(1084.0, -1.1, 3.0),
                pk(1930.0, -1.9, 2.39),
                pk(2334.0, -8.0, 1.93),
                pk(4454.0, -8.0, 2.63),
                pk(5568.0, -4.2, 1.55),
                pk(7504.0, -7.2, 3.0),
                pk(107.0, 0.7, 0.5),
                pk(473.0, 2.4, 0.5),
                pk(6814.0, 7.7, 1.48),
                pk(8862.0, 8.0, 3.0)
            )
        ),
        Preset(
            id = "basshead",
            name = "Basshead",
            description = "Para os amantes de graves.",
            category = PresetCategory.PREFERENCE,
            target = "Basshead",
            preDb = -5.7,
            bands = listOf(
                pk(4613.0, -12.0, 2.5),
                pk(2246.0, -12.0, 1.31),
                pk(6746.0, -7.9, 2.5),
                pk(1122.0, -2.6, 1.73),
                pk(10302.0, -6.1, 2.5),
                pk(20.0, -5.0, 0.42),
                pk(223.0, -4.9, 0.84),
                pk(3184.0, 2.5, 2.5),
                pk(3116.0, 9.2, 0.3),
                ls(286.0, 8.2, 0.47)
            )
        ),
        Preset(
            id = "energico",
            name = "Enérgico",
            description = "Sonoridade intensa, pensada para rock.",
            category = PresetCategory.PREFERENCE,
            target = "JM-1",
            preDb = -3.3,
            bands = listOf(
                pk(25.0, -3.1, 0.5),
                pk(97.0, -1.4, 0.68),
                pk(2445.0, -8.0, 0.87),
                pk(4725.0, -8.0, 3.0),
                pk(6920.0, -8.0, 3.0),
                pk(12439.0, -8.0, 1.96),
                pk(1838.0, 5.0, 0.92),
                pk(2904.0, 4.5, 2.62),
                pk(3665.0, 3.7, 3.0),
                pk(8376.0, 8.0, 2.42)
            )
        ),
        Preset(
            id = "musical",
            name = "Musical",
            description = "Graves fortes, médios comedidos e agudos delicados.",
            category = PresetCategory.PREFERENCE,
            target = "JM-1",
            preDb = -2.3,
            bands = listOf(
                pk(24.0, -3.2, 0.1),
                pk(2277.0, -8.0, 1.69),
                pk(4588.0, -9.4, 2.8),
                pk(5979.0, -3.8, 2.0),
                pk(7184.0, -3.2, 3.0),
                pk(10638.0, -10.4, 3.0),
                pk(840.0, 2.2, 1.54),
                pk(1393.0, 2.0, 1.85),
                pk(3224.0, 1.1, 3.0),
                pk(15808.0, 2.2, 2.5)
            )
        ),
        Preset(
            id = "analitico",
            name = "Analítico",
            description = "Para quem prioriza detalhamento e clareza.",
            category = PresetCategory.PREFERENCE,
            target = "Analítico",
            preDb = -0.8,
            bands = listOf(
                ls(120.0, -9.3, 0.44),
                pk(7007.0, -9.1, 2.5),
                pk(147.0, -3.5, 0.48),
                pk(4561.0, -12.0, 2.5),
                pk(2198.0, -12.0, 1.42),
                pk(255.0, -0.4, 1.78),
                pk(11291.0, -7.8, 2.5),
                pk(1093.0, -2.7, 1.17),
                pk(3226.0, 6.2, 0.3),
                pk(3028.0, 1.8, 2.5)
            )
        ),

        // === Curvas de Referência ===
        Preset(
            id = "referencia",
            name = "JM-1",
            description = "Sonoridade fiel à curva JM-1.",
            category = PresetCategory.REFERENCE,
            target = "Referência",
            preDb = -4.0,
            bands = listOf(
                ls(297.0, -3.4, 0.81),
                pk(11107.0, -4.8, 2.5),
                pk(1027.0, -1.7, 1.98),
                pk(20.0, -3.6, 0.57),
                pk(7055.0, -3.8, 2.5),
                pk(4588.0, -8.0, 2.5),
                pk(2208.0, -8.2, 1.83),
                pk(595.0, 3.5, 0.42),
                pk(3144.0, 5.6, 1.69),
                pk(3031.0, 0.5, 2.5)
            )
        ),
        Preset(
            id = "harman",
            name = "Harman IE 2019",
            description = "A curva de preferência mais estudada do mundo.",
            category = PresetCategory.REFERENCE,
            target = "Harman",
            preDb = -0.5,
            bands = listOf(
                ls(199.0, -5.6, 0.94),
                pk(399.0, -1.4, 1.62),
                pk(2271.0, -8.0, 1.32),
                pk(256.0, -4.2, 1.17),
                pk(7566.0, -7.6, 0.43),
                pk(4662.0, -6.8, 2.5),
                pk(9138.0, 3.8, 2.5),
                pk(3318.0, 7.8, 1.32),
                pk(5767.0, 5.8, 2.5),
                pk(3209.0, 1.1, 1.33)
            )
        ),
        Preset(
            id = "vdsf",
            name = "VDSF",
            description = "Meio-termo entre campo difuso e Harman, com graves discretos.",
            category = PresetCategory.REFERENCE,
            target = "JM-1",
            preDb = -2.2,
            bands = listOf(
                pk(25.0, -8.0, 0.5),
                pk(80.0, -5.0, 0.52),
                pk(210.0, -2.0, 0.79),
                pk(1286.0, -5.9, 1.34),
                pk(1978.0, -7.4, 1.67),
                pk(2375.0, -5.4, 2.4),
                pk(4384.0, -4.7, 3.0),
                pk(4833.0, -6.0, 3.0),
                pk(7252.0, -7.6, 3.0),
                pk(1718.0, 8.0, 0.5)
            )
        ),
        Preset(
            id = "etymotic",
            name = "Etymotic",
            description = "Graves contidos e muita definição nos médios e agudos.",
            category = PresetCategory.REFERENCE,
            target = "JM-1",
            preDb = -2.2,
            bands = listOf(
                pk(25.0, -8.0, 0.97),
                pk(45.0, -8.0, 0.5),
                pk(105.0, -5.3, 0.51),
                pk(226.0, -1.4, 0.91),
                pk(2190.0, -8.0, 1.39),
                pk(4276.0, -5.0, 3.0),
                pk(4847.0, -7.2, 3.0),
                pk(7260.0, -6.8, 3.0),
                pk(785.0, 1.8, 1.42),
                pk(3120.0, 6.5, 1.01)
            )
        )
    )

    fun findById(id: String): Preset? = presets.find { it.id == id }
    val defaultPreset: Preset = presets.first()
}

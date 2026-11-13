package audio.kuba.equalizer.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import audio.kuba.equalizer.dsp.CustomEqConstants
import audio.kuba.equalizer.dsp.FilterBand
import audio.kuba.equalizer.dsp.FilterType
import audio.kuba.equalizer.dsp.Preset
import audio.kuba.equalizer.dsp.PresetCategory
import audio.kuba.equalizer.dsp.PresetRepository
import audio.kuba.equalizer.usb.KubaUsbManager
import audio.kuba.equalizer.usb.UsbConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ScreenTab(val title: String) {
    PRESETS("Oficiais"),
    CUSTOM_EQ("Equalizador"),
    PARAMETRIC_EQ("Avançado")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("kuba_equalizer_prefs", Context.MODE_PRIVATE)

    val usbManager = KubaUsbManager(application)
    val connectionState: StateFlow<UsbConnectionState> = usbManager.connectionState
    val isLiveSyncing: StateFlow<Boolean> = usbManager.isLiveSyncing

    // Aba ativa atual
    private val _currentTab = MutableStateFlow(ScreenTab.PRESETS)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Presets customizados salvos pelo usuário
    private val _customPresets = MutableStateFlow<List<Preset>>(emptyList())
    val customPresets: StateFlow<List<Preset>> = _customPresets.asStateFlow()

    // Preset selecionado atualmente para visualização/aplicação
    private val _selectedPreset = MutableStateFlow<Preset>(PresetRepository.defaultPreset)
    val selectedPreset: StateFlow<Preset> = _selectedPreset.asStateFlow()

    // Ganhos das 10 bandas do equalizador simples (-12.0 a +12.0 dB)
    private val _customGains = MutableStateFlow(DoubleArray(10) { 0.0 })
    val customGains: StateFlow<DoubleArray> = _customGains.asStateFlow()

    // --- Equalizador Paramétrico Avançado (10 Bandas com Tipo, Frequência, Ganho, Q e Bypass) ---
    val defaultParametricBands = listOf(
        FilterBand.lowShelf(31.5, 0.0, 0.707),
        FilterBand.peak(63.0, 0.0, 1.414),
        FilterBand.peak(125.0, 0.0, 1.414),
        FilterBand.peak(250.0, 0.0, 1.414),
        FilterBand.peak(500.0, 0.0, 1.414),
        FilterBand.peak(1000.0, 0.0, 1.414),
        FilterBand.peak(2000.0, 0.0, 1.414),
        FilterBand.peak(4000.0, 0.0, 1.414),
        FilterBand.peak(8000.0, 0.0, 1.414),
        FilterBand.highShelf(16000.0, 0.0, 0.707)
    )

    private val _parametricBands = MutableStateFlow(defaultParametricBands)
    val parametricBands: StateFlow<List<FilterBand>> = _parametricBands.asStateFlow()

    private val _selectedParametricBandIndex = MutableStateFlow(0)
    val selectedParametricBandIndex: StateFlow<Int> = _selectedParametricBandIndex.asStateFlow()

    // ID do preset que está gravado na memória flash do cabo
    private val _appliedPresetId = MutableStateFlow<String?>(
        prefs.getString("applied_preset_id", "padrao")
    )
    val appliedPresetId: StateFlow<String?> = _appliedPresetId.asStateFlow()

    // Diálogo de confirmação de gravação USB
    private val _showConfirmDialog = MutableStateFlow<Preset?>(null)
    val showConfirmDialog: StateFlow<Preset?> = _showConfirmDialog.asStateFlow()

    // Diálogo de salvar preset personalizado
    private val _showSavePresetDialog = MutableStateFlow(false)
    val showSavePresetDialog: StateFlow<Boolean> = _showSavePresetDialog.asStateFlow()

    init {
        usbManager.startMonitoring()
        loadCustomPresets()

        // Restaura preset selecionado salvo anteriormente
        val savedId = prefs.getString("selected_preset_id", "padrao")
        val found = findAnyPreset(savedId ?: "padrao") ?: PresetRepository.defaultPreset
        _selectedPreset.value = found
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
        when (tab) {
            ScreenTab.CUSTOM_EQ -> updateCustomLivePreset()
            ScreenTab.PARAMETRIC_EQ -> updateParametricLivePreset()
            ScreenTab.PRESETS -> {}
        }
    }

    fun selectPreset(preset: Preset) {
        _selectedPreset.value = preset
        prefs.edit().putString("selected_preset_id", preset.id).apply()

        // Se o preset selecionado tiver bandas, sincroniza com os equalizadores
        if (preset.bands.size == 10) {
            _parametricBands.value = preset.bands
            val gains = DoubleArray(10) { i -> preset.bands[i].gainDb }
            _customGains.value = gains
        }
    }

    // === Operações do Equalizador Simples (10 Bandas) ===

    fun updateCustomBandGain(index: Int, gainDb: Double) {
        if (index !in 0 until 10) return
        val updated = _customGains.value.copyOf()
        updated[index] = gainDb
        _customGains.value = updated
        updateCustomLivePreset()
    }

    fun resetCustomEq() {
        _customGains.value = DoubleArray(10) { 0.0 }
        updateCustomLivePreset()
    }

    private fun updateCustomLivePreset() {
        val gains = _customGains.value
        val bands = CustomEqConstants.createBands(gains)
        val pre = CustomEqConstants.calculateAutoPreDb(gains)

        val livePreset = Preset(
            id = "custom_live",
            name = "Personalizado",
            description = "Equalização 10 bandas",
            category = PresetCategory.CUSTOM,
            target = "JM-1",
            preDb = pre,
            bands = bands
        )
        _selectedPreset.value = livePreset
    }

    // === Operações do Equalizador Avançado (Paramétrico Estilo Voicemeeter) ===

    fun setParametricBandIndex(index: Int) {
        if (index in 0 until 10) {
            _selectedParametricBandIndex.value = index
        }
    }

    fun updateParametricBand(index: Int, band: FilterBand) {
        if (index !in 0 until 10) return
        val currentList = _parametricBands.value.toMutableList()
        currentList[index] = band
        _parametricBands.value = currentList
        updateParametricLivePreset()
    }

    fun updateParametricBandType(index: Int, type: FilterType) {
        if (index !in 0 until 10) return
        val b = _parametricBands.value[index]
        updateParametricBand(index, b.copy(type = type))
    }

    fun updateParametricBandFrequency(index: Int, frequency: Double) {
        if (index !in 0 until 10) return
        val b = _parametricBands.value[index]
        updateParametricBand(index, b.copy(frequency = frequency.coerceIn(20.0, 20000.0)))
    }

    fun updateParametricBandGain(index: Int, gainDb: Double) {
        if (index !in 0 until 10) return
        val b = _parametricBands.value[index]
        updateParametricBand(index, b.copy(gainDb = gainDb.coerceIn(-15.0, 15.0)))
    }

    fun updateParametricBandQ(index: Int, q: Double) {
        if (index !in 0 until 10) return
        val b = _parametricBands.value[index]
        updateParametricBand(index, b.copy(q = q.coerceIn(0.1, 15.0)))
    }

    fun toggleParametricBandEnabled(index: Int) {
        if (index !in 0 until 10) return
        val b = _parametricBands.value[index]
        updateParametricBand(index, b.copy(enabled = !b.enabled))
    }

    fun resetParametricEq() {
        _parametricBands.value = defaultParametricBands
        updateParametricLivePreset()
    }

    private fun updateParametricLivePreset() {
        val bands = _parametricBands.value
        val maxPositiveGain = bands.filter { it.enabled && it.type.hasGain }.maxOfOrNull { it.gainDb } ?: 0.0
        val pre = if (maxPositiveGain > 0.0) -maxPositiveGain.coerceAtMost(12.0) else 0.0

        val livePreset = Preset(
            id = "param_live",
            name = "Avançado",
            description = "Equalizador paramétrico personalizado",
            category = PresetCategory.CUSTOM,
            target = "JM-1",
            preDb = pre,
            bands = bands
        )
        _selectedPreset.value = livePreset
    }

    // === Diálogos e Persistência de Presets ===

    fun openSavePresetDialog() {
        _showSavePresetDialog.value = true
    }

    fun dismissSavePresetDialog() {
        _showSavePresetDialog.value = false
    }

    fun saveCustomPreset(name: String, description: String = "") {
        val trimmedName = name.trim().ifEmpty { "Personalizado ${_customPresets.value.size + 1}" }
        val trimmedDesc = description.trim().ifEmpty { "Equalização criada pelo usuário" }

        val isParametric = _currentTab.value == ScreenTab.PARAMETRIC_EQ
        val bands = if (isParametric) {
            _parametricBands.value
        } else {
            CustomEqConstants.createBands(_customGains.value)
        }

        val maxGain = bands.filter { it.enabled && it.type.hasGain }.maxOfOrNull { it.gainDb } ?: 0.0
        val pre = if (maxGain > 0.0) -maxGain.coerceAtMost(12.0) else 0.0

        val newId = "custom_${System.currentTimeMillis()}"
        val newPreset = Preset(
            id = newId,
            name = trimmedName,
            description = trimmedDesc,
            category = PresetCategory.CUSTOM,
            target = "JM-1",
            preDb = pre,
            bands = bands
        )

        val updatedList = _customPresets.value + newPreset
        _customPresets.value = updatedList
        saveCustomPresetsToPrefs(updatedList)
        _showSavePresetDialog.value = false

        selectPreset(newPreset)
    }

    fun deleteCustomPreset(preset: Preset) {
        val updatedList = _customPresets.value.filter { it.id != preset.id }
        _customPresets.value = updatedList
        saveCustomPresetsToPrefs(updatedList)

        if (_selectedPreset.value.id == preset.id) {
            selectPreset(PresetRepository.defaultPreset)
        }
    }

    fun requestConnect() {
        usbManager.checkExistingConnections()
        usbManager.requestPermission()
    }

    fun onApplyClicked() {
        _showConfirmDialog.value = _selectedPreset.value
    }

    fun dismissConfirmDialog() {
        _showConfirmDialog.value = null
    }

    fun confirmApply() {
        val targetPreset = _showConfirmDialog.value ?: _selectedPreset.value
        _showConfirmDialog.value = null

        viewModelScope.launch {
            val result = usbManager.applyPreset(targetPreset)
            if (result.isSuccess) {
                _appliedPresetId.value = targetPreset.id
                prefs.edit().putString("applied_preset_id", targetPreset.id).apply()
            }
        }
    }

    private fun findAnyPreset(id: String): Preset? {
        return PresetRepository.findById(id) ?: _customPresets.value.find { it.id == id }
    }

    private fun loadCustomPresets() {
        val jsonStr = prefs.getString("saved_custom_presets_json", null) ?: return
        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Preset>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val desc = obj.optString("desc", "Personalizada")
                val pre = obj.getDouble("pre")
                val bandsJson = obj.getJSONArray("bands")

                val bands = mutableListOf<FilterBand>()
                for (j in 0 until bandsJson.length()) {
                    val bObj = bandsJson.getJSONObject(j)
                    val tStr = bObj.getString("t")
                    val type = when (tStr) {
                        "LS", "LOW_SHELF" -> FilterType.LOW_SHELF
                        "HS", "HIGH_SHELF" -> FilterType.HIGH_SHELF
                        "LP", "LOW_PASS" -> FilterType.LOW_PASS
                        "HP", "HIGH_PASS" -> FilterType.HIGH_PASS
                        "NC", "NOTCH" -> FilterType.NOTCH
                        else -> FilterType.PEAK
                    }
                    val f = bObj.getDouble("f")
                    val g = bObj.getDouble("g")
                    val q = bObj.getDouble("q")
                    val en = bObj.optBoolean("en", true)
                    bands.add(FilterBand(type, f, g, q, en))
                }

                list.add(
                    Preset(
                        id = id,
                        name = name,
                        description = desc,
                        category = PresetCategory.CUSTOM,
                        target = "JM-1",
                        preDb = pre,
                        bands = bands
                    )
                )
            }
            _customPresets.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCustomPresetsToPrefs(list: List<Preset>) {
        try {
            val jsonArray = JSONArray()
            for (p in list) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("desc", p.description)
                obj.put("pre", p.preDb)

                val bandsArray = JSONArray()
                for (b in p.bands) {
                    val bObj = JSONObject()
                    bObj.put("t", b.type.shortName)
                    bObj.put("f", b.frequency)
                    bObj.put("g", b.gainDb)
                    bObj.put("q", b.q)
                    bObj.put("en", b.enabled)
                    bandsArray.put(bObj)
                }
                obj.put("bands", bandsArray)
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_custom_presets_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        usbManager.stopMonitoring()
    }
}

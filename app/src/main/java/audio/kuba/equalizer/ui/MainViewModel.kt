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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ScreenTab {
    PRESETS, // Curvas oficiais
    CUSTOM_EQ // Equalizador personalizado 10 bandas
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("kuba_equalizer_prefs", Context.MODE_PRIVATE)

    val usbManager = KubaUsbManager(application)
    val connectionState: StateFlow<UsbConnectionState> = usbManager.connectionState

    // Aba ativa atual
    private val _currentTab = MutableStateFlow(ScreenTab.PRESETS)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Presets customizados salvos pelo usuário
    private val _customPresets = MutableStateFlow<List<Preset>>(emptyList())
    val customPresets: StateFlow<List<Preset>> = _customPresets.asStateFlow()

    // Preset selecionado atualmente para visualização/aplicação
    private val _selectedPreset = MutableStateFlow<Preset>(PresetRepository.defaultPreset)
    val selectedPreset: StateFlow<Preset> = _selectedPreset.asStateFlow()

    // Ganhos das 10 bandas do equalizador personalizado em tempo real (-12.0 a +12.0 dB)
    private val _customGains = MutableStateFlow(DoubleArray(10) { 0.0 })
    val customGains: StateFlow<DoubleArray> = _customGains.asStateFlow()

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
        if (tab == ScreenTab.CUSTOM_EQ) {
            updateCustomLivePreset()
        }
    }

    fun selectPreset(preset: Preset) {
        _selectedPreset.value = preset
        prefs.edit().putString("selected_preset_id", preset.id).apply()
    }

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
            description = "Equalização paramétrica em tempo real",
            category = PresetCategory.CUSTOM,
            target = "Custom",
            preDb = pre,
            bands = bands
        )
        _selectedPreset.value = livePreset
    }

    fun openSavePresetDialog() {
        _showSavePresetDialog.value = true
    }

    fun dismissSavePresetDialog() {
        _showSavePresetDialog.value = false
    }

    fun saveCustomPreset(name: String) {
        val trimmed = name.trim().ifEmpty { "Personalizado ${_customPresets.value.size + 1}" }
        val gains = _customGains.value.copyOf()
        val bands = CustomEqConstants.createBands(gains)
        val pre = CustomEqConstants.calculateAutoPreDb(gains)

        val newId = "custom_${System.currentTimeMillis()}"
        val newPreset = Preset(
            id = newId,
            name = trimmed,
            description = "Equalização criada pelo usuário",
            category = PresetCategory.CUSTOM,
            target = "Custom",
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
                    val type = if (bObj.getString("t") == "LS") FilterType.LOW_SHELF else FilterType.PEAK
                    val f = bObj.getDouble("f")
                    val g = bObj.getDouble("g")
                    val q = bObj.getDouble("q")
                    bands.add(FilterBand(type, f, g, q))
                }

                list.add(
                    Preset(
                        id = id,
                        name = name,
                        description = desc,
                        category = PresetCategory.CUSTOM,
                        target = "Custom",
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
                    bObj.put("t", if (b.type == FilterType.LOW_SHELF) "LS" else "PK")
                    bObj.put("f", b.frequency)
                    bObj.put("g", b.gainDb)
                    bObj.put("q", b.q)
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

package audio.kuba.equalizer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.kuba.equalizer.dsp.FilterBand
import audio.kuba.equalizer.dsp.Preset
import audio.kuba.equalizer.dsp.PresetCategory
import audio.kuba.equalizer.dsp.PresetRepository
import audio.kuba.equalizer.ui.components.ConnectionStatusCard
import audio.kuba.equalizer.ui.components.CustomBandSliders
import audio.kuba.equalizer.ui.components.FrequencyGraph
import audio.kuba.equalizer.ui.components.HorizontalPresetSelector
import audio.kuba.equalizer.ui.components.ParametricEqControls
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBg
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaStatusWarnBg
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaSurfaceSecondary
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary
import audio.kuba.equalizer.usb.UsbConnectionState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val appliedPresetId by viewModel.appliedPresetId.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val showSavePresetDialog by viewModel.showSavePresetDialog.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val customGains by viewModel.customGains.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()

    val parametricBands by viewModel.parametricBands.collectAsState()
    val selectedParametricBandIndex by viewModel.selectedParametricBandIndex.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(connectionState) {
        when (val state = connectionState) {
            is UsbConnectionState.Error -> {
                snackbarHostState.showSnackbar("Erro: ${state.message}")
            }
            is UsbConnectionState.Success -> {
                snackbarHostState.showSnackbar("Equalização \"${state.presetName}\" gravada com sucesso no cabo!")
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(KubaBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = KubaBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomApplyBar(
                selectedName = selectedPreset.name,
                isWriting = connectionState is UsbConnectionState.Writing,
                onApplyClick = { viewModel.onApplyClicked() }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth > 600.dp

            if (isLandscape) {
                // === LAYOUT HORIZONTAL (LANDSCAPE / TABLETS EM MODO PAISAGEM) ===
                LandscapeLayout(
                    connectionState = connectionState,
                    selectedPreset = selectedPreset,
                    appliedPresetId = appliedPresetId,
                    currentTab = currentTab,
                    customGains = customGains,
                    customPresets = customPresets,
                    parametricBands = parametricBands,
                    selectedParametricBandIndex = selectedParametricBandIndex,
                    viewModel = viewModel
                )
            } else {
                // === LAYOUT VERTICAL (PORTRAIT / TABLETS EM MODO RETRATO) ===
                PortraitLayout(
                    connectionState = connectionState,
                    selectedPreset = selectedPreset,
                    appliedPresetId = appliedPresetId,
                    currentTab = currentTab,
                    customGains = customGains,
                    customPresets = customPresets,
                    parametricBands = parametricBands,
                    selectedParametricBandIndex = selectedParametricBandIndex,
                    viewModel = viewModel
                )
            }
        }
    }

    // Modal de Gravação no Cabo USB
    if (showConfirmDialog != null) {
        val preset = showConfirmDialog!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            containerColor = KubaStatusWarnBg,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Gravar no cabo DSP?",
                    style = MaterialTheme.typography.titleLarge,
                    color = KubaAmber
                )
            },
            text = {
                Text(
                    text = "Deseja gravar o perfil \"${preset.name}\" no cabo do Kuba Auri? A memória flash do chip DSP será atualizada permanentemente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KubaTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmApply() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KubaAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Gravar no cabo",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissConfirmDialog() }
                ) {
                    Text(
                        text = "Cancelar",
                        color = KubaTextMuted
                    )
                }
            }
        )
    }

    // Modal de Salvar Equalização Personalizada (com nome e descrição)
    if (showSavePresetDialog) {
        var presetNameInput by remember { mutableStateOf("") }
        var presetDescInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.dismissSavePresetDialog() },
            containerColor = KubaSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Salvar Equalização",
                    style = MaterialTheme.typography.titleLarge,
                    color = KubaAmber
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Identifique este perfil de equalização no app:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KubaTextPrimary
                    )

                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        singleLine = true,
                        label = { Text("Nome da curva", color = KubaTextMuted) },
                        placeholder = { Text("Ex: Meu Rock, Bass Boost...", color = KubaTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KubaAmber,
                            unfocusedBorderColor = KubaBorderLine,
                            focusedTextColor = KubaTextPrimary,
                            unfocusedTextColor = KubaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = presetDescInput,
                        onValueChange = { presetDescInput = it },
                        singleLine = true,
                        label = { Text("Breve descrição (opcional)", color = KubaTextMuted) },
                        placeholder = { Text("Padrão: Equalização criada pelo usuário", color = KubaTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KubaAmber,
                            unfocusedBorderColor = KubaBorderLine,
                            focusedTextColor = KubaTextPrimary,
                            unfocusedTextColor = KubaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveCustomPreset(presetNameInput, presetDescInput) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KubaAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Salvar",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissSavePresetDialog() }
                ) {
                    Text(
                        text = "Cancelar",
                        color = KubaTextMuted
                    )
                }
            }
        )
    }
}

@Composable
private fun PortraitLayout(
    connectionState: UsbConnectionState,
    selectedPreset: Preset,
    appliedPresetId: String?,
    currentTab: ScreenTab,
    customGains: DoubleArray,
    customPresets: List<Preset>,
    parametricBands: List<FilterBand>,
    selectedParametricBandIndex: Int,
    viewModel: MainViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
    ) {
        // 1. Cabeçalho da Marca & Status USB
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "KUBA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = KubaAmber
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(width = 1.dp, height = 18.dp)
                        .background(KubaBorderLine)
                )
                Text(
                    text = "AURI DSP",
                    style = MaterialTheme.typography.labelSmall,
                    color = KubaTextMuted
                )
            }

            ConnectionStatusCard(state = connectionState)
        }

        // 2. Gráfico de Resposta de Frequência (Compacto para não ocupar tela excessiva)
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            FrequencyGraph(
                preset = selectedPreset,
                modifier = Modifier.fillMaxWidth(),
                isCompact = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Barra de Abas (Oficiais / Equalizador / Avançado)
        TabSelectorBar(
            currentTab = currentTab,
            onTabSelected = { viewModel.setTab(it) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Conteúdo das Abas
        when (currentTab) {
            ScreenTab.PRESETS -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val prefPresets = PresetRepository.presets.filter { it.category == PresetCategory.PREFERENCE }
                    val refPresets = PresetRepository.presets.filter { it.category == PresetCategory.REFERENCE }

                    HorizontalPresetSelector(
                        title = "Curvas de Preferência",
                        presets = prefPresets,
                        selectedPresetId = selectedPreset.id,
                        appliedPresetId = appliedPresetId,
                        onSelectPreset = { viewModel.selectPreset(it) }
                    )

                    HorizontalPresetSelector(
                        title = "Curvas de Referência",
                        presets = refPresets,
                        selectedPresetId = selectedPreset.id,
                        appliedPresetId = appliedPresetId,
                        onSelectPreset = { viewModel.selectPreset(it) }
                    )

                    if (customPresets.isNotEmpty()) {
                        HorizontalPresetSelector(
                            title = "Meus Presets Salvos",
                            presets = customPresets,
                            selectedPresetId = selectedPreset.id,
                            appliedPresetId = appliedPresetId,
                            onSelectPreset = { viewModel.selectPreset(it) },
                            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            ScreenTab.CUSTOM_EQ -> {
                // Equalizador Simples 10 bandas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetCustomEq() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KubaTextMuted),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KubaBorderLine)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Zerar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zerar", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { viewModel.openSavePresetDialog() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KubaSurfaceSecondary,
                                contentColor = KubaAmber
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Salvar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar Preset", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    CustomBandSliders(
                        gains = customGains,
                        onGainChanged = { index, gain ->
                            viewModel.updateCustomBandGain(index, gain)
                        }
                    )

                    if (customPresets.isNotEmpty()) {
                        HorizontalPresetSelector(
                            title = "Minhas Equalizações Salvas",
                            presets = customPresets,
                            selectedPresetId = selectedPreset.id,
                            appliedPresetId = appliedPresetId,
                            onSelectPreset = { preset ->
                                viewModel.selectPreset(preset)
                                for (i in 0 until 10.coerceAtMost(preset.bands.size)) {
                                    viewModel.updateCustomBandGain(i, preset.bands[i].gainDb)
                                }
                            },
                            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            ScreenTab.PARAMETRIC_EQ -> {
                // Equalizador Paramétrico Avançado (Voicemeeter style)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParametricEqControls(
                        bands = parametricBands,
                        selectedBandIndex = selectedParametricBandIndex,
                        onBandSelected = { viewModel.setParametricBandIndex(it) },
                        onBandTypeChanged = { idx, type -> viewModel.updateParametricBandType(idx, type) },
                        onFrequencyChanged = { idx, freq -> viewModel.updateParametricBandFrequency(idx, freq) },
                        onGainChanged = { idx, gain -> viewModel.updateParametricBandGain(idx, gain) },
                        onQChanged = { idx, q -> viewModel.updateParametricBandQ(idx, q) },
                        onToggleBandEnabled = { idx -> viewModel.toggleParametricBandEnabled(idx) },
                        onResetAll = { viewModel.resetParametricEq() },
                        onSavePreset = { viewModel.openSavePresetDialog() },
                        onApplyToCable = { viewModel.onApplyClicked() },
                        isWriting = connectionState is UsbConnectionState.Writing,
                        customPresets = customPresets,
                        selectedPresetId = selectedPreset.id,
                        appliedPresetId = appliedPresetId,
                        onSelectCustomPreset = { preset -> viewModel.selectPreset(preset) },
                        onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LandscapeLayout(
    connectionState: UsbConnectionState,
    selectedPreset: Preset,
    appliedPresetId: String?,
    currentTab: ScreenTab,
    customGains: DoubleArray,
    customPresets: List<Preset>,
    parametricBands: List<FilterBand>,
    selectedParametricBandIndex: Int,
    viewModel: MainViewModel
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // === COLUNA ESQUERDA: MARCA, STATUS E GRÁFICO ===
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KUBA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = KubaAmber
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(width = 1.dp, height = 18.dp)
                            .background(KubaBorderLine)
                    )
                    Text(
                        text = "AURI DSP",
                        style = MaterialTheme.typography.labelSmall,
                        color = KubaTextMuted
                    )
                }

                ConnectionStatusCard(state = connectionState)
            }

            FrequencyGraph(
                preset = selectedPreset,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                isCompact = false
            )
        }

        // === COLUNA DIREITA: ABAS E CONTROLES DE OPERAÇÃO ===
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabSelectorBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )

            when (currentTab) {
                ScreenTab.PRESETS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val prefPresets = PresetRepository.presets.filter { it.category == PresetCategory.PREFERENCE }
                        val refPresets = PresetRepository.presets.filter { it.category == PresetCategory.REFERENCE }

                        HorizontalPresetSelector(
                            title = "Curvas de Preferência",
                            presets = prefPresets,
                            selectedPresetId = selectedPreset.id,
                            appliedPresetId = appliedPresetId,
                            onSelectPreset = { viewModel.selectPreset(it) }
                        )

                        HorizontalPresetSelector(
                            title = "Curvas de Referência",
                            presets = refPresets,
                            selectedPresetId = selectedPreset.id,
                            appliedPresetId = appliedPresetId,
                            onSelectPreset = { viewModel.selectPreset(it) }
                        )

                        if (customPresets.isNotEmpty()) {
                            HorizontalPresetSelector(
                                title = "Meus Presets Salvos",
                                presets = customPresets,
                                selectedPresetId = selectedPreset.id,
                                appliedPresetId = appliedPresetId,
                                onSelectPreset = { viewModel.selectPreset(it) },
                                onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) }
                            )
                        }
                    }
                }

                ScreenTab.CUSTOM_EQ -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetCustomEq() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KubaTextMuted),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KubaBorderLine)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Zerar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Zerar", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { viewModel.openSavePresetDialog() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KubaSurfaceSecondary,
                                    contentColor = KubaAmber
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Salvar", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar Preset", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        CustomBandSliders(
                            gains = customGains,
                            onGainChanged = { index, gain ->
                                viewModel.updateCustomBandGain(index, gain)
                            }
                        )

                        if (customPresets.isNotEmpty()) {
                            HorizontalPresetSelector(
                                title = "Minhas Equalizações Salvas",
                                presets = customPresets,
                                selectedPresetId = selectedPreset.id,
                                appliedPresetId = appliedPresetId,
                                onSelectPreset = { preset ->
                                    viewModel.selectPreset(preset)
                                    for (i in 0 until 10.coerceAtMost(preset.bands.size)) {
                                        viewModel.updateCustomBandGain(i, preset.bands[i].gainDb)
                                    }
                                },
                                onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) }
                            )
                        }
                    }
                }

                ScreenTab.PARAMETRIC_EQ -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ParametricEqControls(
                            bands = parametricBands,
                            selectedBandIndex = selectedParametricBandIndex,
                            onBandSelected = { viewModel.setParametricBandIndex(it) },
                            onBandTypeChanged = { idx, type -> viewModel.updateParametricBandType(idx, type) },
                            onFrequencyChanged = { idx, freq -> viewModel.updateParametricBandFrequency(idx, freq) },
                            onGainChanged = { idx, gain -> viewModel.updateParametricBandGain(idx, gain) },
                            onQChanged = { idx, q -> viewModel.updateParametricBandQ(idx, q) },
                            onToggleBandEnabled = { idx -> viewModel.toggleParametricBandEnabled(idx) },
                            onResetAll = { viewModel.resetParametricEq() },
                            onSavePreset = { viewModel.openSavePresetDialog() },
                            onApplyToCable = { viewModel.onApplyClicked() },
                            isWriting = connectionState is UsbConnectionState.Writing,
                            customPresets = customPresets,
                            selectedPresetId = selectedPreset.id,
                            appliedPresetId = appliedPresetId,
                            onSelectCustomPreset = { preset -> viewModel.selectPreset(preset) },
                            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabSelectorBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, KubaBorderLine, RoundedCornerShape(12.dp))
            .background(KubaSurfaceSecondary, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabItem(
            title = "OFICIAIS",
            isSelected = currentTab == ScreenTab.PRESETS,
            onClick = { onTabSelected(ScreenTab.PRESETS) },
            modifier = Modifier.weight(1f)
        )

        TabItem(
            title = "EQUALIZADOR",
            isSelected = currentTab == ScreenTab.CUSTOM_EQ,
            onClick = { onTabSelected(ScreenTab.CUSTOM_EQ) },
            modifier = Modifier.weight(1.15f)
        )

        TabItem(
            title = "AVANÇADO",
            isSelected = currentTab == ScreenTab.PARAMETRIC_EQ,
            onClick = { onTabSelected(ScreenTab.PARAMETRIC_EQ) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) KubaAmber else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp,
                fontSize = 11.sp
            ),
            color = if (isSelected) Color.Black else KubaTextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun BottomApplyBar(
    selectedName: String,
    isWriting: Boolean,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = KubaBorderLine, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xE60F0E0B))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SELECIONADO",
                    style = MaterialTheme.typography.labelSmall,
                    color = KubaTextMuted
                )
                Text(
                    text = selectedName.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = KubaTextPrimary,
                    maxLines = 1
                )
            }

            Button(
                onClick = onApplyClick,
                enabled = !isWriting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubaAmber,
                    contentColor = Color.Black,
                    disabledContainerColor = KubaAmber.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                if (isWriting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gravando…",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                } else {
                    Text(
                        text = "Gravar no cabo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

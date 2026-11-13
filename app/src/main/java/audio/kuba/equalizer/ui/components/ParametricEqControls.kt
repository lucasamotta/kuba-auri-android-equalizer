package audio.kuba.equalizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.kuba.equalizer.dsp.FilterBand
import audio.kuba.equalizer.dsp.FilterType
import audio.kuba.equalizer.dsp.Preset
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaSurfaceSecondary
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParametricEqControls(
    bands: List<FilterBand>,
    selectedBandIndex: Int,
    onBandSelected: (Int) -> Unit,
    onBandTypeChanged: (Int, FilterType) -> Unit,
    onFrequencyChanged: (Int, Double) -> Unit,
    onGainChanged: (Int, Double) -> Unit,
    onQChanged: (Int, Double) -> Unit,
    onToggleBandEnabled: (Int) -> Unit,
    onResetAll: () -> Unit,
    onSavePreset: () -> Unit,
    onApplyToCable: () -> Unit,
    isWriting: Boolean = false,
    customPresets: List<Preset> = emptyList(),
    selectedPresetId: String = "",
    appliedPresetId: String? = null,
    onSelectCustomPreset: (Preset) -> Unit = {},
    onDeleteCustomPreset: (Preset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentBand = bands.getOrNull(selectedBandIndex) ?: bands[0]

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Barra de Ações Rápidas (Zerar, Salvar e Gravar no Cabo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onResetAll,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KubaTextMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, KubaBorderLine),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(0.9f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Zerar", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Zerar", fontSize = 12.sp)
            }

            Button(
                onClick = onSavePreset,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubaSurfaceSecondary,
                    contentColor = KubaAmber
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Salvar", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salvar Curva", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onApplyToCable,
                enabled = !isWriting,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubaAmber,
                    contentColor = Color.Black,
                    disabledContainerColor = KubaAmber.copy(alpha = 0.5f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                if (isWriting) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gravando…", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Gravar", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gravar no Cabo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Seletor de Faixas (Bandas 1 a 10 estilo Voicemeeter / DSP rack)
        val bandScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(bandScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bands.forEachIndexed { index, band ->
                val isSelected = index == selectedBandIndex
                val borderCol = if (isSelected) KubaAmber else KubaBorderLine
                val bgCol = if (isSelected) KubaSurfaceSecondary else KubaSurface

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgCol)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable { onBandSelected(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (band.enabled) Color(0xFF22C55E) else Color(0xFF6B7280))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "B${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) KubaAmber else KubaTextPrimary
                            )
                        }
                        Text(
                            text = formatFrequency(band.frequency),
                            fontSize = 10.sp,
                            color = KubaTextMuted
                        )
                        Text(
                            text = band.type.shortName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) KubaAmber else KubaTextMuted
                        )
                    }
                }
            }
        }

        // 3. Painel de Controle da Banda Selecionada
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KubaSurface)
                .border(1.dp, KubaBorderLine, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabeçalho da Banda: Número + Switch Ativo/Bypass
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Banda ${selectedBandIndex + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = KubaAmber
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentBand.enabled) "ATIVA" else "BYPASS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBand.enabled) Color(0xFF22C55E) else KubaTextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (currentBand.enabled) Color(0x2222C55E) else Color(0x226B7280))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Button(
                    onClick = { onToggleBandEnabled(selectedBandIndex) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentBand.enabled) Color(0xFF22C55E) else Color(0xFF374151),
                        contentColor = if (currentBand.enabled) Color.Black else Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (currentBand.enabled) "Ligado" else "Desligado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Seletor de Formato de Curva (Filter Type)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tipo de Filtro:",
                    fontSize = 11.sp,
                    color = KubaTextMuted,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterType.values().forEach { type ->
                        val isCurrentType = currentBand.type == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrentType) KubaAmber else KubaSurfaceSecondary)
                                .clickable { onBandTypeChanged(selectedBandIndex, type) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${type.shortName} · ${type.label}",
                                fontSize = 11.sp,
                                fontWeight = if (isCurrentType) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentType) Color.Black else KubaTextPrimary
                            )
                        }
                    }
                }
            }

            // Controle 1: Frequência (Hz) com escala logarítmica (20 Hz - 20000 Hz)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Frequência Central", fontSize = 12.sp, color = KubaTextMuted)
                    Text(
                        text = "${formatFrequency(currentBand.frequency)} (${currentBand.frequency.roundToInt()} Hz)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubaTextPrimary
                    )
                }

                // Slider logarítmico
                val logMin = ln(20.0)
                val logMax = ln(20000.0)
                val currentLog = ln(currentBand.frequency.coerceIn(20.0, 20000.0))
                val sliderPos = ((currentLog - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)

                Slider(
                    value = sliderPos,
                    onValueChange = { pos ->
                        val newFreq = exp(logMin + pos * (logMax - logMin))
                        onFrequencyChanged(selectedBandIndex, newFreq)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = KubaAmber,
                        activeTrackColor = KubaAmber,
                        inactiveTrackColor = KubaSurfaceSecondary
                    ),
                    modifier = Modifier.height(28.dp)
                )

                // Atalhos de ajuste fino de frequência
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(-20.0, -5.0, 5.0, 20.0).forEach { deltaPercent ->
                        val label = if (deltaPercent > 0) "+${deltaPercent.toInt()}%" else "${deltaPercent.toInt()}%"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(KubaSurfaceSecondary)
                                .clickable {
                                    val factor = 1.0 + deltaPercent / 100.0
                                    onFrequencyChanged(selectedBandIndex, currentBand.frequency * factor)
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(label, fontSize = 10.sp, color = KubaTextMuted)
                        }
                    }
                }
            }

            // Controle 2: Ganho (dB)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Ganho", fontSize = 12.sp, color = KubaTextMuted)
                    if (currentBand.type.hasGain) {
                        Text(
                            text = String.format("%+.1f dB", currentBand.gainDb),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentBand.gainDb != 0.0) KubaAmber else KubaTextPrimary
                        )
                    } else {
                        Text(
                            text = "N/A (Filtro de corte)",
                            fontSize = 11.sp,
                            color = KubaTextMuted
                        )
                    }
                }

                if (currentBand.type.hasGain) {
                    Slider(
                        value = currentBand.gainDb.toFloat(),
                        onValueChange = { onGainChanged(selectedBandIndex, it.toDouble()) },
                        valueRange = -15f..15f,
                        colors = SliderDefaults.colors(
                            thumbColor = KubaAmber,
                            activeTrackColor = KubaAmber,
                            inactiveTrackColor = KubaSurfaceSecondary
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(-3.0, -1.0, 0.0, 1.0, 3.0).forEach { delta ->
                            val label = if (delta == 0.0) "0 dB" else String.format("%+.0f", delta)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(KubaSurfaceSecondary)
                                    .clickable {
                                        if (delta == 0.0) {
                                            onGainChanged(selectedBandIndex, 0.0)
                                        } else {
                                            onGainChanged(selectedBandIndex, currentBand.gainDb + delta)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = KubaTextMuted)
                            }
                        }
                    }
                }
            }

            // Controle 3: Fator Q (Largura de banda / Ressonância)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Fator Q (Largura de banda)", fontSize = 12.sp, color = KubaTextMuted)
                    Text(
                        text = String.format("%.2f Q", currentBand.q),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubaTextPrimary
                    )
                }

                Slider(
                    value = currentBand.q.toFloat(),
                    onValueChange = { onQChanged(selectedBandIndex, it.toDouble()) },
                    valueRange = 0.2f..10.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = KubaAmber,
                        activeTrackColor = KubaAmber,
                        inactiveTrackColor = KubaSurfaceSecondary
                    ),
                    modifier = Modifier.height(28.dp)
                )

                // Presets rápidos de Q (Padrões de áudio: Butterworth 0.707, 1/1 oitava 1.414, 1/3 oitava 4.3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        0.5 to "Largo",
                        0.707 to "0.71 (Suave)",
                        1.414 to "1.41 (1 Oit)",
                        3.0 to "Estreito",
                        6.0 to "Pico"
                    ).forEach { (presetQ, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(KubaSurfaceSecondary)
                                .clickable { onQChanged(selectedBandIndex, presetQ) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(label, fontSize = 9.sp, color = KubaTextMuted)
                        }
                    }
                }
            }
        }

        // 4. Presets Salvos pelo Usuário (se houver)
        if (customPresets.isNotEmpty()) {
            HorizontalPresetSelector(
                title = "Minhas Equalizações Salvas",
                presets = customPresets,
                selectedPresetId = selectedPresetId,
                appliedPresetId = appliedPresetId,
                onSelectPreset = onSelectCustomPreset,
                onDeleteCustomPreset = onDeleteCustomPreset
            )
        }
    }
}

private fun formatFrequency(hz: Double): String {
    return if (hz >= 1000.0) {
        val khz = hz / 1000.0
        if (khz >= 10.0 || (khz * 10).roundToInt() % 10 == 0) {
            "${khz.roundToInt()}k"
        } else {
            String.format("%.1fk", khz)
        }
    } else {
        "${hz.roundToInt()}"
    }
}

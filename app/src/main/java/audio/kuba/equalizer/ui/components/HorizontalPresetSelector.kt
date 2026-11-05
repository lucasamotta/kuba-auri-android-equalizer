package audio.kuba.equalizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.kuba.equalizer.dsp.Preset
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBg
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaStatusOk
import audio.kuba.equalizer.ui.theme.KubaStatusWarn
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaSurfaceSecondary
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary

@Composable
fun HorizontalPresetSelector(
    title: String,
    presets: List<Preset>,
    selectedPresetId: String,
    appliedPresetId: String?,
    onSelectPreset: (Preset) -> Unit,
    onDeleteCustomPreset: ((Preset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = KubaTextMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = "${presets.size} CURVAS",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = KubaAmber
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets, key = { it.id }) { preset ->
                val isSelected = preset.id == selectedPresetId
                val isApplied = preset.id == appliedPresetId

                CompactPresetChip(
                    preset = preset,
                    isSelected = isSelected,
                    isApplied = isApplied,
                    onClick = { onSelectPreset(preset) },
                    onDelete = if (onDeleteCustomPreset != null) {
                        { onDeleteCustomPreset(preset) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun CompactPresetChip(
    preset: Preset,
    isSelected: Boolean,
    isApplied: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) KubaAmber else KubaSurface,
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) KubaAmber else if (isApplied) KubaStatusOk else KubaBorderLine,
        label = "chipBorder"
    )

    val titleColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else KubaTextPrimary,
        label = "chipTitle"
    )

    val subtitleColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xCC000000) else KubaTextMuted,
        label = "chipSubtitle"
    )

    Box(
        modifier = modifier
            .width(136.dp)
            .height(86.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nome do preset
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Botão de deletar preset personalizado (se aplicável)
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir preset",
                            tint = if (isSelected) Color.Black else KubaStatusWarn,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (isApplied) {
                    // Tag / bolinha verde de aplicado
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.Black else KubaStatusOk)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Descrição curta
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Alvo ou Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isApplied) "APLICADO" else preset.target.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isSelected) Color.Black else if (isApplied) KubaStatusOk else KubaAmber
                )

                Text(
                    text = "${preset.preDb} dB",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = subtitleColor
                )
            }
        }
    }
}

package audio.kuba.equalizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.kuba.equalizer.dsp.Preset
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBg
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaStatusOk
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary

@Composable
fun PresetItemCard(
    preset: Preset,
    isSelected: Boolean,
    isApplied: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) KubaAmber else KubaSurface,
        label = "presetBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) KubaAmber else KubaBorderLine,
        label = "presetBorder"
    )

    val titleColor by animateColorAsState(
        targetValue = if (isSelected) KubaBg else KubaTextPrimary,
        label = "presetTitle"
    )

    val descColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF3A2F0A) else KubaTextMuted,
        label = "presetDesc"
    )

    val appliedColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1F1A0A) else KubaStatusOk,
        label = "appliedColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    color = titleColor,
                    modifier = Modifier.weight(1f)
                )

                if (isApplied) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0x331F1A0A) else Color(0x268FC79F),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(appliedColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "APLICADO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = appliedColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = descColor
            )
        }
    }
}

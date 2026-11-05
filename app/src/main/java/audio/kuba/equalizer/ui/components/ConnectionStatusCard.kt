package audio.kuba.equalizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaStatusOk
import audio.kuba.equalizer.ui.theme.KubaStatusWarn
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.usb.UsbConnectionState

@Composable
fun ConnectionStatusCard(
    state: UsbConnectionState,
    onRequestConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state is UsbConnectionState.Connected ||
            state is UsbConnectionState.Writing ||
            state is UsbConnectionState.Success

    val isWriting = state is UsbConnectionState.Writing

    val dotColor by animateColorAsState(
        targetValue = when (state) {
            is UsbConnectionState.Connected, is UsbConnectionState.Success -> KubaStatusOk
            is UsbConnectionState.Writing -> KubaAmber
            is UsbConnectionState.PermissionDenied, is UsbConnectionState.Error -> KubaStatusWarn
            else -> Color(0xFF5A5240)
        },
        label = "dotColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWriting) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .border(1.dp, KubaBorderLine, RoundedCornerShape(999.dp))
            .background(KubaSurface, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador de status (Dot)
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Rótulo de status
        val labelText = when (state) {
            is UsbConnectionState.Connected -> "${state.productName} conectado"
            is UsbConnectionState.Writing -> "Gravando ${state.presetName} (${state.currentPacket}/${state.totalPackets})…"
            is UsbConnectionState.Success -> "Gravado no cabo com sucesso!"
            is UsbConnectionState.DeviceDetected -> "Cabo detectado (solicitando permissão)"
            is UsbConnectionState.PermissionDenied -> "Permissão USB negada"
            is UsbConnectionState.Error -> "Erro USB: ${state.message}"
            is UsbConnectionState.Disconnected -> "Cabo desconectado"
        }

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall,
            color = if (isConnected) KubaStatusOk else KubaTextMuted
        )

        if (!isConnected) {
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onRequestConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubaAmber,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(999.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.size(height = 30.dp, width = 120.dp)
            ) {
                Text(
                    text = "Conectar",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black
                )
            }
        }
    }
}

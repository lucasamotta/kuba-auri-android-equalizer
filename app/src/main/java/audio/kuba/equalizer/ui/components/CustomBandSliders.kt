package audio.kuba.equalizer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.kuba.equalizer.dsp.CustomEqConstants
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaSurfaceSecondary
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary
import kotlin.math.roundToInt

@Composable
fun CustomBandSliders(
    gains: DoubleArray,
    onGainChanged: (index: Int, newGain: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, KubaBorderLine, RoundedCornerShape(16.dp))
            .background(KubaSurface, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "10 BANDAS (DSP WALKPLAY)",
                    style = MaterialTheme.typography.labelSmall,
                    color = KubaAmber,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "FAIXA: -12 dB a +12 dB",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = KubaTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 10 Sliders verticais lado a lado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0 until 10) {
                    val label = CustomEqConstants.FREQUENCY_LABELS[i]
                    val gain = gains[i]
                    VerticalBandSlider(
                        label = label,
                        gainDb = gain,
                        onGainChanged = { newG -> onGainChanged(i, newG) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalBandSlider(
    label: String,
    gainDb: Double,
    onGainChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val minGain = CustomEqConstants.MIN_GAIN_DB
    val maxGain = CustomEqConstants.MAX_GAIN_DB

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Valor do ganho em dB (ex: +3.5 ou -2)
        val formattedGain = if (gainDb > 0.0) "+%.1f".format(gainDb) else "%.1f".format(gainDb)
        Text(
            text = formattedGain,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (gainDb != 0.0) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (gainDb > 0.0) KubaAmber else if (gainDb < 0.0) KubaTextPrimary else KubaTextMuted,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Trilho interativo do slider vertical
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        fun updateGain(yPos: Float, h: Float) {
                            val clampedY = yPos.coerceIn(0f, h)
                            // y=0 -> maxGain (+12), y=h -> minGain (-12)
                            val ratio = 1.0 - (clampedY / h).toDouble()
                            val newGain = minGain + ratio * (maxGain - minGain)
                            // Arredonda para 0.5 dB para ajuste tátil preciso
                            val stepped = (newGain * 2.0).roundToInt() / 2.0
                            onGainChanged(stepped.coerceIn(minGain, maxGain))
                        }

                        detectTapGestures { offset ->
                            updateGain(offset.y, size.height.toFloat())
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val clampedY = change.position.y.coerceIn(0f, size.height.toFloat())
                            val ratio = 1.0 - (clampedY / size.height).toDouble()
                            val newGain = minGain + ratio * (maxGain - minGain)
                            val stepped = (newGain * 2.0).roundToInt() / 2.0
                            onGainChanged(stepped.coerceIn(minGain, maxGain))
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val trackWidth = 4.dp.toPx()
                val trackX = (w - trackWidth) / 2f
                val zeroY = h / 2f

                // 1. Fundo do trilho
                drawRoundRect(
                    color = KubaSurfaceSecondary,
                    topLeft = Offset(trackX, 0f),
                    size = Size(trackWidth, h),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // 2. Linha do meio (0 dB)
                drawLine(
                    color = Color(0x3DF5F0E6),
                    start = Offset(trackX - 3.dp.toPx(), zeroY),
                    end = Offset(trackX + trackWidth + 3.dp.toPx(), zeroY),
                    strokeWidth = 1.dp.toPx()
                )

                // 3. Posição atual do thumb
                val ratio = (gainDb - minGain) / (maxGain - minGain)
                val thumbY = (h - ratio * h).toFloat()

                // Barra colorida do 0 dB até o valor atual
                val fillTop = if (thumbY < zeroY) thumbY else zeroY
                val fillHeight = kotlin.math.abs(thumbY - zeroY)
                if (fillHeight > 0f) {
                    drawRoundRect(
                        color = KubaAmber.copy(alpha = 0.8f),
                        topLeft = Offset(trackX, fillTop),
                        size = Size(trackWidth, fillHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Thumb do slider (pílula horizontal / cápsula âmbar)
                val thumbWidth = 16.dp.toPx()
                val thumbHeight = 10.dp.toPx()
                val thumbX = (w - thumbWidth) / 2f
                drawRoundRect(
                    color = KubaAmber,
                    topLeft = Offset(thumbX, thumbY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Rótulo da frequência (ex: "31", "1k", "16k")
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = KubaTextMuted,
            maxLines = 1
        )
    }
}

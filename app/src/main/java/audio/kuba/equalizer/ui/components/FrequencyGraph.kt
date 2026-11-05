package audio.kuba.equalizer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.kuba.equalizer.dsp.KubaMeasurement
import audio.kuba.equalizer.dsp.Preset
import audio.kuba.equalizer.dsp.PresetCategory
import audio.kuba.equalizer.ui.theme.KubaAmber
import audio.kuba.equalizer.ui.theme.KubaBorderLine
import audio.kuba.equalizer.ui.theme.KubaSurface
import audio.kuba.equalizer.ui.theme.KubaTextMuted
import audio.kuba.equalizer.ui.theme.KubaTextPrimary
import kotlin.math.ln

@Composable
fun FrequencyGraph(
    preset: Preset,
    modifier: Modifier = Modifier
) {
    val fMin = 20.0
    val fMax = 20000.0
    val logMin = ln(fMin)
    val logMax = ln(fMax)

    // Calcula as curvas idênticas ao web-app da Kuba
    val targetCurve = remember(preset) {
        KubaMeasurement.getTargetCurve(preset)
    }

    val resultCurve = remember(preset) {
        KubaMeasurement.calculateResultCurve(preset)
    }

    val yRange = remember(preset) {
        KubaMeasurement.calculateYRange(resultCurve, targetCurve)
    }

    val frequencies = KubaMeasurement.frequencies

    val targetLabel = if (preset.category == PresetCategory.PREFERENCE) {
        "Alvo JM-1"
    } else {
        if (preset.id == "harman") "Alvo Harman" else "Alvo ${preset.target}"
    }

    Box(
        modifier = modifier
            .border(1.dp, KubaBorderLine, RoundedCornerShape(20.dp))
            .background(KubaSurface, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name.uppercase(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                        color = KubaTextPrimary
                    )
                    Text(
                        text = "${preset.description} (Pré: ${preset.preDb} dB)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KubaTextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = targetLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = KubaAmber
                    )
                    Text(
                        text = "RESPOSTA + ALVO",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = KubaTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas de Resposta de Frequência
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                val totalW = size.width
                val totalH = size.height

                // Margens idênticas ao layout do web-app da Kuba
                val leftPad = 36.dp.toPx()
                val rightPad = 8.dp.toPx()
                val topPad = 12.dp.toPx()
                val bottomPad = 24.dp.toPx()

                val chartW = totalW - leftPad - rightPad
                val chartH = totalH - topPad - bottomPad

                val (loDb, hiDb) = yRange
                val dbSpan = hiDb - loDb

                fun xOf(f: Double): Float {
                    val ratio = (ln(f) - logMin) / (logMax - logMin)
                    return leftPad + (ratio * chartW).toFloat()
                }

                fun yOf(db: Double): Float {
                    val ratio = (hiDb - db) / dbSpan
                    return topPad + (ratio * chartH).toFloat()
                }

                // 1. Grade horizontal de decibéis (passo de 5 dB) com labels à esquerda
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#A8A092")
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                var dbStep = loDb.toInt()
                while (dbStep <= hiDb.toInt()) {
                    val y = yOf(dbStep.toDouble())
                    if (y in topPad..topPad + chartH) {
                        drawLine(
                            color = Color(0x14F5F0E6),
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + chartW, y),
                            strokeWidth = 1f
                        )
                        val label = if (dbStep > 0) "+$dbStep" else "$dbStep"
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            leftPad - 6.dp.toPx(),
                            y + 3.dp.toPx(),
                            textPaint
                        )
                    }
                    dbStep += 5
                }

                // 2. Grade vertical de frequências (estilo logarítmico profissional)
                val gridFreqs = listOf(
                    20.0 to "20", 50.0 to "50", 100.0 to "100", 200.0 to "200",
                    500.0 to "500", 1000.0 to "1k", 2000.0 to "2k",
                    5000.0 to "5k", 10000.0 to "10k", 20000.0 to "20k"
                )
                val majorFreqs = setOf(20.0, 100.0, 1000.0, 10000.0, 20000.0)

                gridFreqs.forEach { (f, label) ->
                    val x = xOf(f)
                    val isMajor = majorFreqs.contains(f)
                    drawLine(
                        color = if (isMajor) Color(0x24F5F0E6) else Color(0x0FF5F0E6),
                        start = Offset(x, topPad),
                        end = Offset(x, topPad + chartH),
                        strokeWidth = 1f
                    )

                    val labelPaint = android.graphics.Paint().apply {
                        color = if (isMajor) android.graphics.Color.parseColor("#F5F0E6") else android.graphics.Color.parseColor("#A8A092")
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        topPad + chartH + 16.dp.toPx(),
                        labelPaint
                    )
                }

                // 3. Área de desenho das curvas recortada para a área interna do gráfico
                clipRect(left = leftPad, top = topPad, right = leftPad + chartW, bottom = topPad + chartH) {
                    // Curva Alvo / Referência (linha cinza claro grossa e arredondada no fundo)
                    val targetPath = Path()
                    for (i in frequencies.indices) {
                        val x = xOf(frequencies[i])
                        val y = yOf(targetCurve[i])
                        if (i == 0) targetPath.moveTo(x, y) else targetPath.lineTo(x, y)
                    }

                    drawPath(
                        path = targetPath,
                        color = Color(0xFF423D33), // Cinza quente/suave do web-app (--target)
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Curva Resultante (fone medido + equalização)
                    val resultPath = Path()
                    val fillPath = Path()

                    for (i in frequencies.indices) {
                        val x = xOf(frequencies[i])
                        val y = yOf(resultCurve[i])
                        if (i == 0) {
                            resultPath.moveTo(x, y)
                            fillPath.moveTo(x, topPad + chartH)
                            fillPath.lineTo(x, y)
                        } else {
                            resultPath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    fillPath.lineTo(leftPad + chartW, topPad + chartH)
                    fillPath.close()

                    // Preenchimento gradiente âmbar sutil
                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(
                            KubaAmber.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        startY = topPad,
                        endY = topPad + chartH
                    )
                    drawPath(fillPath, fillBrush)

                    // Linha âmbar principal nítida da curva resultante
                    drawPath(
                        path = resultPath,
                        color = KubaAmber,
                        style = Stroke(
                            width = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

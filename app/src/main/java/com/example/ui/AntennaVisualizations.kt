package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.domain.AntennaCalculationResult
import com.example.domain.AntennaType
import com.example.domain.ImpedancePoint
import com.example.domain.PolarPatternPoint
import com.example.domain.SdrSpectrumFrame
import com.example.domain.formatFrequencyDisplay
import com.example.domain.formatMetricLength
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 1. Interactive Vector CAD Blueprint Canvas showing the geometry, dimensions, feedpoint, and mast
 * with pinch-to-zoom, drag-pan, and 2D rotation.
 */
@Composable
fun AntennaBlueprintCanvas(
    calc: AntennaCalculationResult,
    modifier: Modifier = Modifier
) {
    var scale2D by remember { mutableFloatStateOf(1f) }
    var offsetX2D by remember { mutableFloatStateOf(0f) }
    var offsetY2D by remember { mutableFloatStateOf(0f) }
    var rotation2D by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { scale2D = (scale2D * 1.25f).coerceAtMost(3.5f) },
                label = { Text("Зум +", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = { scale2D = (scale2D / 1.25f).coerceAtLeast(0.6f) },
                label = { Text("Зум −", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = { rotation2D = (rotation2D + 90f) % 360f },
                label = { Text("Повернуть 90°", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    scale2D = 1f
                    offsetX2D = 0f
                    offsetY2D = 0f
                    rotation2D = 0f
                },
                label = { Text("Сброс 2D", style = MaterialTheme.typography.labelMedium) }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF071120))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        scale2D = (scale2D * zoom).coerceIn(0.6f, 3.5f)
                        offsetX2D = (offsetX2D + pan.x).coerceIn(-size.width * 0.6f, size.width * 0.6f)
                        offsetY2D = (offsetY2D + pan.y).coerceIn(-size.height * 0.6f, size.height * 0.6f)
                        rotation2D = (rotation2D + rotation) % 360f
                    }
                }
                .padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale2D,
                        scaleY = scale2D,
                        translationX = offsetX2D,
                        translationY = offsetY2D,
                        rotationZ = rotation2D
                    )
            ) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

            // Draw CAD engineering grid
            val gridStep = 28.dp.toPx()
            val gridColor = Color(0xFF132744)
            var gx = gridStep
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = gridStep
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                gy += gridStep
            }

            val cyan = Color(0xFF00E5FF)
            val amber = Color(0xFFFFB300)
            val emerald = Color(0xFF10B981)
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 27f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            }
            val subTextPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#00E5FF")
                textSize = 24f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
            }

            when (calc.antennaType) {
                AntennaType.DIPOLE, AntennaType.EFHW, AntennaType.OCFD_WINDOM, AntennaType.G5RV_ZS6BKW, AntennaType.T2FD_BEVERAGE -> {
                    val span = w * 0.74f
                    val leftX = cx - span / 2f
                    val rightX = cx + span / 2f
                    val wireY = cy - 24f
                    val feedX = when (calc.antennaType) {
                        AntennaType.EFHW -> leftX + 20f
                        AntennaType.OCFD_WINDOM -> leftX + span * 0.33f
                        else -> cx
                    }

                    // Mast & ground reflection plane
                    drawLine(Color(0xFF475569), Offset(feedX, wireY + 10f), Offset(feedX, h - 22f), strokeWidth = 4f, pathEffect = dash)
                    drawLine(emerald, Offset(w * 0.12f, h - 20f), Offset(w * 0.88f, h - 20f), strokeWidth = 3f)

                    // Metallic radiator arms + ceramic end insulators
                    drawLine(cyan, Offset(leftX, wireY), Offset(feedX - 10f, wireY), strokeWidth = 6.5f)
                    drawLine(cyan, Offset(feedX + 10f, wireY), Offset(rightX, wireY), strokeWidth = 6.5f)
                    drawCircle(Color.White, radius = 4.5f, center = Offset(leftX, wireY))
                    drawCircle(Color.White, radius = 4.5f, center = Offset(rightX, wireY))

                    if (calc.antennaType == AntennaType.T2FD_BEVERAGE) {
                        drawLine(cyan, Offset(leftX, wireY + 26f), Offset(rightX, wireY + 26f), strokeWidth = 4.5f)
                        drawLine(amber, Offset(leftX, wireY), Offset(leftX, wireY + 26f), strokeWidth = 4f)
                        drawLine(amber, Offset(rightX, wireY), Offset(rightX, wireY + 26f), strokeWidth = 4f)
                    } else if (calc.antennaType == AntennaType.G5RV_ZS6BKW) {
                        // 450 Ohm ladder line
                        drawLine(amber, Offset(cx - 7f, wireY), Offset(cx - 7f, wireY + 62f), strokeWidth = 3f)
                        drawLine(amber, Offset(cx + 7f, wireY), Offset(cx + 7f, wireY + 62f), strokeWidth = 3f)
                    }

                    // Feedpoint Balun / UnUn
                    drawCircle(amber, radius = 9f, center = Offset(feedX, wireY))

                    // Dimension arrow line above
                    val dimY = wireY - 36f
                    drawLine(amber, Offset(leftX, dimY), Offset(rightX, dimY), strokeWidth = 2.5f)
                    drawLine(amber, Offset(leftX, dimY - 8f), Offset(leftX, wireY - 6f), strokeWidth = 2f)
                    drawLine(amber, Offset(rightX, dimY - 8f), Offset(rightX, wireY - 6f), strokeWidth = 2f)

                    val totalLenStr = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    val armStr = calc.dimensions.getOrNull(1)?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.apply {
                        drawText("${calc.antennaType.code}: L = $totalLenStr", leftX + 8f, dimY - 10f, textPaint)
                        drawText("Узел2: $armStr • H = ${formatMetricLength(calc.mastHeightMeters)}", leftX + 8f, h - 32f, subTextPaint)
                    }
                }

                AntennaType.INVERTED_V, AntennaType.DELTA_LOOP -> {
                    val apexY = 36f
                    val baseLeftX = w * 0.16f
                    val baseRightX = w * 0.84f
                    val baseY = h - 38f
                    drawLine(Color(0xFF475569), Offset(cx, apexY), Offset(cx, h - 18f), strokeWidth = 4.5f, pathEffect = dash)
                    drawLine(cyan, Offset(cx, apexY), Offset(baseLeftX, baseY), strokeWidth = 6.5f)
                    drawLine(cyan, Offset(cx, apexY), Offset(baseRightX, baseY), strokeWidth = 6.5f)
                    if (calc.antennaType == AntennaType.DELTA_LOOP) {
                        drawLine(cyan, Offset(baseLeftX, baseY), Offset(baseRightX, baseY), strokeWidth = 6f)
                    }
                    drawCircle(amber, radius = 9f, center = Offset(cx, apexY))
                    val d0 = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.apply {
                        drawText("${calc.antennaType.titleRu.take(22)}: $d0", 18f, 28f, textPaint)
                        drawText("Мачта H = ${formatMetricLength(calc.mastHeightMeters)}", cx + 12f, h - 22f, subTextPaint)
                    }
                }

                AntennaType.GROUND_PLANE, AntennaType.VERTICAL_5_8, AntennaType.COLLINEAR_COAX, AntennaType.J_POLE -> {
                    val baseX = cx
                    val baseY = cy + 32f
                    val topY = 28f
                    drawLine(cyan, Offset(baseX, topY), Offset(baseX, baseY), strokeWidth = 7.5f)
                    if (calc.antennaType == AntennaType.J_POLE) {
                        val stubX = baseX + 42f
                        val stubTopY = baseY - (baseY - topY) * 0.35f
                        drawLine(amber, Offset(stubX, stubTopY), Offset(stubX, baseY), strokeWidth = 6f)
                        drawLine(amber, Offset(baseX, baseY), Offset(stubX, baseY), strokeWidth = 6f)
                    } else {
                        drawLine(amber, Offset(baseX, baseY), Offset(baseX - w * 0.28f, h - 20f), strokeWidth = 5f)
                        drawLine(amber, Offset(baseX, baseY), Offset(baseX + w * 0.28f, h - 20f), strokeWidth = 5f)
                        drawLine(amber, Offset(baseX, baseY), Offset(baseX - w * 0.14f, h - 14f), strokeWidth = 3.5f)
                        drawLine(amber, Offset(baseX, baseY), Offset(baseX + w * 0.14f, h - 14f), strokeWidth = 3.5f)
                        if (calc.antennaType == AntennaType.COLLINEAR_COAX) {
                            drawCircle(amber, radius = 6f, center = Offset(baseX, (topY + baseY) * 0.42f))
                            drawCircle(amber, radius = 6f, center = Offset(baseX, (topY + baseY) * 0.72f))
                        }
                    }
                    drawCircle(emerald, radius = 8.5f, center = Offset(baseX, baseY))
                    val mainLen = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    val subLen = calc.dimensions.getOrNull(1)?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.apply {
                        drawText("Штырь: $mainLen", baseX + 18f, topY + 32f, textPaint)
                        drawText("Радиалы/шлейф: $subLen", 18f, h - 24f, subTextPaint)
                    }
                }

                AntennaType.YAGI, AntennaType.LPDA, AntennaType.HB9CV -> {
                    val boomLeft = w * 0.12f
                    val boomRight = w * 0.88f
                    drawLine(amber, Offset(boomLeft, cy), Offset(boomRight, cy), strokeWidth = 5.5f)
                    val elems = calc.dimensions.filter { !it.code.contains("BOOM") }
                    val n = elems.size.coerceAtLeast(2)
                    elems.forEachIndexed { idx, item ->
                        val x = boomLeft + (boomRight - boomLeft) * (idx.toFloat() / (n - 1).coerceAtLeast(1))
                        val halfLen = (h * 0.36f * (1f - idx * 0.055f)).coerceAtLeast(26f)
                        val elemColor = if (idx == 1 && calc.antennaType == AntennaType.YAGI) emerald else cyan
                        drawLine(elemColor, Offset(x, cy - halfLen), Offset(x, cy + halfLen), strokeWidth = 5.5f)
                        drawContext.canvas.nativeCanvas.drawText(
                            item.code,
                            x - 18f,
                            cy - halfLen - 10f,
                            subTextPaint
                        )
                    }
                    val boomDim = calc.dimensions.lastOrNull()?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.drawText(
                        "Бум: $boomDim • Элементов: $n",
                        boomLeft,
                        h - 16f,
                        textPaint
                    )
                }

                AntennaType.MOXON, AntennaType.HEXBEAM, AntennaType.SPIDERBEAM -> {
                    val rectW = w * 0.56f
                    val rectH = h * 0.48f
                    val left = cx - rectW / 2f
                    val right = cx + rectW / 2f
                    val top = cy - rectH / 2f
                    val bot = cy + rectH / 2f
                    // Driven element (top) with inward tails
                    drawLine(cyan, Offset(left, top), Offset(right, top), strokeWidth = 6.5f)
                    drawLine(cyan, Offset(left, top), Offset(left, cy - 12f), strokeWidth = 5.5f)
                    drawLine(cyan, Offset(right, top), Offset(right, cy - 12f), strokeWidth = 5.5f)
                    // Reflector element (bottom) with upward tails
                    drawLine(amber, Offset(left, bot), Offset(right, bot), strokeWidth = 6f)
                    drawLine(amber, Offset(left, bot), Offset(left, cy + 12f), strokeWidth = 5f)
                    drawLine(amber, Offset(right, bot), Offset(right, cy + 12f), strokeWidth = 5f)
                    drawCircle(emerald, radius = 8f, center = Offset(cx, top))
                    val d0 = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.drawText("${calc.antennaType.titleRu.take(22)}: $d0", 18f, h - 16f, textPaint)
                }

                AntennaType.DISCONE, AntennaType.BICONE_BOWTIE -> {
                    val apexY = cy - 22f
                    val discHalf = w * 0.24f
                    if (calc.antennaType == AntennaType.BICONE_BOWTIE) {
                        drawLine(cyan, Offset(cx, apexY - 6f), Offset(cx - discHalf, 26f), strokeWidth = 5.5f)
                        drawLine(cyan, Offset(cx, apexY - 6f), Offset(cx + discHalf, 26f), strokeWidth = 5.5f)
                        drawLine(cyan, Offset(cx - discHalf, 26f), Offset(cx + discHalf, 26f), strokeWidth = 2.5f, pathEffect = dash)
                    } else {
                        drawLine(cyan, Offset(cx - discHalf, apexY - 8f), Offset(cx + discHalf, apexY - 8f), strokeWidth = 6.5f)
                    }
                    val coneHalf = w * 0.32f
                    val coneBotY = h - 28f
                    drawLine(amber, Offset(cx, apexY), Offset(cx - coneHalf, coneBotY), strokeWidth = 5.5f)
                    drawLine(amber, Offset(cx, apexY), Offset(cx + coneHalf, coneBotY), strokeWidth = 5.5f)
                    drawLine(amber, Offset(cx, apexY), Offset(cx - coneHalf * 0.45f, coneBotY), strokeWidth = 3.5f)
                    drawLine(amber, Offset(cx, apexY), Offset(cx + coneHalf * 0.45f, coneBotY), strokeWidth = 3.5f)
                    drawLine(amber, Offset(cx - coneHalf, coneBotY), Offset(cx + coneHalf, coneBotY), strokeWidth = 2f, pathEffect = dash)
                    val dDisc = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    val lCone = calc.dimensions.getOrNull(1)?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.apply {
                        drawText("Верх: $dDisc", 18f, 32f, textPaint)
                        drawText("Конус L = $lCone", 18f, h - 16f, subTextPaint)
                    }
                }

                AntennaType.MAG_LOOP, AntennaType.FRACTAL_KOCH -> {
                    val outerR = min(w, h) * 0.34f
                    val innerR = outerR * 0.25f
                    drawCircle(cyan, radius = outerR, center = Offset(cx, cy), style = Stroke(width = 7f))
                    drawCircle(amber, radius = innerR, center = Offset(cx, cy + outerR - innerR - 6f), style = Stroke(width = 4.5f))
                    drawRect(
                        color = emerald,
                        topLeft = Offset(cx - 18f, cy - outerR - 10f),
                        size = Size(36f, 20f)
                    )
                    val dLoop = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    val capStr = if (calc.tuningCapacitorPf != null) {
                        String.format("КПЕ: %.1f пФ (%.0f В)", calc.tuningCapacitorPf, calc.loopCapacitorVoltageV ?: 0.0)
                    } else {
                        calc.dimensions.getOrNull(1)?.formattedLength() ?: ""
                    }
                    drawContext.canvas.nativeCanvas.apply {
                        drawText("${calc.antennaType.code}: $dLoop", 18f, 34f, textPaint)
                        drawText(capStr, 18f, h - 18f, subTextPaint)
                    }
                }

                AntennaType.CUBICAL_QUAD, AntennaType.QFH_SATELLITE, AntennaType.HELICAL_PATCH, AntennaType.HORN_SHF, AntennaType.PARABOLIC_DISH -> {
                    val boxSize = min(w, h) * 0.56f
                    if (calc.antennaType == AntennaType.PARABOLIC_DISH) {
                        drawCircle(amber, radius = boxSize * 0.65f, center = Offset(cx, cy), style = Stroke(width = 5f))
                        drawCircle(cyan, radius = 10f, center = Offset(cx, cy))
                        drawLine(emerald, Offset(cx - boxSize * 0.5f, cy - boxSize * 0.4f), Offset(cx, cy), strokeWidth = 3f)
                        drawLine(emerald, Offset(cx + boxSize * 0.5f, cy - boxSize * 0.4f), Offset(cx, cy), strokeWidth = 3f)
                        drawLine(emerald, Offset(cx, cy + boxSize * 0.62f), Offset(cx, cy), strokeWidth = 3f)
                    } else {
                        drawRect(
                            color = amber,
                            topLeft = Offset(cx - boxSize * 0.58f, cy - boxSize * 0.52f),
                            size = Size(boxSize, boxSize),
                            style = Stroke(width = 4f)
                        )
                        drawRect(
                            color = cyan,
                            topLeft = Offset(cx - boxSize * 0.38f, cy - boxSize * 0.38f),
                            size = Size(boxSize * 0.92f, boxSize * 0.92f),
                            style = Stroke(width = 6f)
                        )
                    }
                    val p0 = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                    drawContext.canvas.nativeCanvas.drawText(
                        "${calc.antennaType.titleRu.take(26)}: $p0",
                        18f,
                        h - 16f,
                        textPaint
                    )
                }
            }
        }
        }
    }
}

/**
 * 2. Smith Chart (Диаграмма Вольперта-Смита) showing constant resistance/reactance circles,
 * SWR <= 2.0 circle, and measured vs optimized impedance locus Z(f).
 */
@Composable
fun SmithChartCanvas(
    primarySweep: List<ImpedancePoint>,
    secondarySweep: List<ImpedancePoint>? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF071120))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(size.width, size.height) * 0.42f

            val gridStroke = Stroke(width = 1.4f)
            val gridColor = Color(0xFF1E3A5F)

            // Outer |Gamma| = 1.0 boundary
            drawCircle(Color(0xFF38BDF8), radius = radius, center = Offset(cx, cy), style = Stroke(width = 2.5f))
            // Horizontal real axis
            drawLine(gridColor, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 1.5f)

            // Constant normalized resistance circles r = 0.5, 1.0, 2.0
            listOf(0.5f, 1.0f, 2.0f).forEach { rNorm ->
                val cRadius = radius / (rNorm + 1f)
                val cCenterX = cx + radius * (rNorm / (rNorm + 1f))
                drawCircle(gridColor, radius = cRadius, center = Offset(cCenterX, cy), style = gridStroke)
            }

            // Constant SWR = 2.0 circle (|Gamma| = 1/3)
            drawCircle(
                color = Color(0x5510B981),
                radius = radius * (1f / 3f),
                center = Offset(cx, cy),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
            )

            // Secondary sweep (e.g., raw VNA before optimization) in Amber
            if (!secondarySweep.isNullOrEmpty()) {
                val secPath = Path()
                secondarySweep.forEachIndexed { idx, pt ->
                    val px = cx + (pt.gammaReal.toFloat() * radius)
                    val py = cy - (pt.gammaImag.toFloat() * radius)
                    if (idx == 0) secPath.moveTo(px, py) else secPath.lineTo(px, py)
                }
                drawPath(secPath, color = Color(0xFFFFB300), style = Stroke(width = 3.5f))
            }

            // Primary sweep in glowing Cyan
            if (primarySweep.isNotEmpty()) {
                val primPath = Path()
                primarySweep.forEachIndexed { idx, pt ->
                    val px = cx + (pt.gammaReal.toFloat() * radius)
                    val py = cy - (pt.gammaImag.toFloat() * radius)
                    if (idx == 0) primPath.moveTo(px, py) else primPath.lineTo(px, py)
                }
                drawPath(primPath, color = Color(0xFF00E5FF), style = Stroke(width = 4f))

                val mid = primarySweep[primarySweep.size / 2]
                val mPx = cx + (mid.gammaReal.toFloat() * radius)
                val mPy = cy - (mid.gammaImag.toFloat() * radius)
                drawCircle(Color(0xFF10B981), radius = 8f, center = Offset(mPx, mPy))
            }

            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 24f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText("Диаграмма Смита (Z0=50 Ом)", 14f, 28f, labelPaint)
                drawText("Зелёный пунктир: КСВ=2.0", 14f, size.height - 14f, labelPaint)
            }
        }
    }
}

/**
 * 3. Polar Radiation Pattern Diagram (Азимутальная H-plane и угломестная E-plane ДН).
 */
@Composable
fun PolarPatternCanvas(
    pattern: List<PolarPatternPoint>,
    gainDbi: Double,
    fbDb: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF071120))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = min(size.width, size.height) * 0.41f
            val gridColor = Color(0xFF1E3A5F)

            // Concentric dB rings (0 dB, -10 dB, -20 dB, -30 dB)
            listOf(1.0f, 0.75f, 0.5f, 0.25f).forEach { frac ->
                drawCircle(gridColor, radius = maxR * frac, center = Offset(cx, cy), style = Stroke(width = 1.2f))
            }
            for (angle in 0 until 360 step 30) {
                val rad = angle * PI / 180.0
                drawLine(
                    gridColor,
                    Offset(cx, cy),
                    Offset(cx + maxR * cos(rad).toFloat(), cy + maxR * sin(rad).toFloat()),
                    strokeWidth = 1f
                )
            }

            if (pattern.isNotEmpty()) {
                val azPath = Path()
                val elPath = Path()
                pattern.forEachIndexed { idx, pt ->
                    val rad = (pt.angleDeg - 90) * PI / 180.0
                    val azNorm = ((pt.azimuthGainNormalizedDb + 38.0).coerceIn(0.0, 38.0) / 38.0).toFloat()
                    val elNorm = ((pt.elevationGainNormalizedDb + 38.0).coerceIn(0.0, 38.0) / 38.0).toFloat()
                    val ax = cx + maxR * azNorm * cos(rad).toFloat()
                    val ay = cy + maxR * azNorm * sin(rad).toFloat()
                    val ex = cx + maxR * elNorm * cos(rad).toFloat()
                    val ey = cy + maxR * elNorm * sin(rad).toFloat()
                    if (idx == 0) {
                        azPath.moveTo(ax, ay)
                        elPath.moveTo(ex, ey)
                    } else {
                        azPath.lineTo(ax, ay)
                        elPath.lineTo(ex, ey)
                    }
                }
                azPath.close()
                elPath.close()

                drawPath(elPath, color = Color(0xFFFFB300), style = Stroke(width = 2.5f))
                drawPath(azPath, color = Color(0xFF00E5FF), style = Stroke(width = 4f))
            }

            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 24f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText(String.format("G = %.2f dBi | F/B = %.1f дБ", gainDbi, fbDb), 14f, 28f, labelPaint)
                drawText("Голубой: Азимут H | Янтарный: Зенит E", 14f, size.height - 14f, labelPaint)
            }
        }
    }
}

/**
 * 4. SWR(f) Curve Chart with optional Measured vs Optimized comparison overlay.
 */
@Composable
fun SwrFrequencyChartCanvas(
    primarySweep: List<ImpedancePoint>,
    secondarySweep: List<ImpedancePoint>? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF071120))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad = 48f
            val bottomPad = 32f
            val topPad = 24f
            val plotW = size.width - leftPad - 16f
            val plotH = size.height - topPad - bottomPad

            val gridColor = Color(0xFF1E3A5F)
            // Horizontal SWR lines: 1.0, 1.5, 2.0, 3.0, 5.0
            listOf(1.0, 1.5, 2.0, 3.0, 5.0).forEach { swrLevel ->
                val norm = ((swrLevel - 1.0) / 4.0).toFloat().coerceIn(0f, 1f)
                val y = topPad + plotH * (1f - norm)
                val lineCol = if (swrLevel == 2.0) Color(0x6610B981) else gridColor
                drawLine(lineCol, Offset(leftPad, y), Offset(leftPad + plotW, y), strokeWidth = 1.5f)
            }

            if (!secondarySweep.isNullOrEmpty()) {
                val secPath = Path()
                secondarySweep.forEachIndexed { idx, pt ->
                    val x = leftPad + plotW * (idx.toFloat() / (secondarySweep.size - 1).coerceAtLeast(1))
                    val norm = ((pt.swr50.coerceIn(1.0, 5.0) - 1.0) / 4.0).toFloat()
                    val y = topPad + plotH * (1f - norm)
                    if (idx == 0) secPath.moveTo(x, y) else secPath.lineTo(x, y)
                }
                drawPath(secPath, color = Color(0xFFFFB300), style = Stroke(width = 3.2f))
            }

            if (primarySweep.isNotEmpty()) {
                val primPath = Path()
                primarySweep.forEachIndexed { idx, pt ->
                    val x = leftPad + plotW * (idx.toFloat() / (primarySweep.size - 1).coerceAtLeast(1))
                    val norm = ((pt.swr50.coerceIn(1.0, 5.0) - 1.0) / 4.0).toFloat()
                    val y = topPad + plotH * (1f - norm)
                    if (idx == 0) primPath.moveTo(x, y) else primPath.lineTo(x, y)
                }
                drawPath(primPath, color = Color(0xFF00E5FF), style = Stroke(width = 4f))
            }

            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 22f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            val fStart = primarySweep.firstOrNull()?.freqMhz ?: 0.0
            val fEnd = primarySweep.lastOrNull()?.freqMhz ?: 0.0
            drawContext.canvas.nativeCanvas.apply {
                drawText("КСВ 5.0", 2f, topPad + 8f, labelPaint)
                drawText("КСВ 2.0", 2f, topPad + plotH * 0.75f, labelPaint)
                drawText("КСВ 1.0", 2f, topPad + plotH, labelPaint)
                drawText(formatFrequencyDisplay(fStart), leftPad, size.height - 4f, labelPaint)
                drawText(formatFrequencyDisplay(fEnd), size.width - 175f, size.height - 4f, labelPaint)
            }
        }
    }
}

/**
 * 5. Real-Time SDR Spectrum & Color Waterfall Analyzer Canvas (for Arinst V2D, Малахит DSP, NanoVNA).
 */
@Composable
fun SdrSpectrumWaterfallCanvas(
    frame: SdrSpectrumFrame,
    waterfallHistory: List<List<Float>>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(275.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF050C18))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val specH = size.height * 0.52f
            val wfTop = specH + 8f
            val wfH = size.height - wfTop

            // Draw Spectrum grid
            val gridColor = Color(0xFF162B49)
            for (i in 1..3) {
                val y = specH * (i / 4f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            drawLine(Color(0x66FFB300), Offset(w / 2f, 0f), Offset(w / 2f, size.height), strokeWidth = 1.5f)

            // Draw real-time FFT spectrum trace
            val bins = frame.binsDbm
            if (bins.isNotEmpty()) {
                val tracePath = Path()
                bins.forEachIndexed { idx, dbm ->
                    val x = w * (idx.toFloat() / (bins.size - 1).coerceAtLeast(1))
                    val norm = ((dbm + 125f) / 105f).coerceIn(0f, 1f)
                    val y = specH * (1f - norm)
                    if (idx == 0) tracePath.moveTo(x, y) else tracePath.lineTo(x, y)
                }
                drawPath(tracePath, color = Color(0xFF00E5FF), style = Stroke(width = 3.2f))
            }

            // Draw Color Waterfall Matrix
            val rows = waterfallHistory.size.coerceAtLeast(1)
            val rowH = wfH / 24f
            waterfallHistory.forEachIndexed { rowIdx, rowBins ->
                val colW = w / rowBins.size.coerceAtLeast(1)
                val y = wfTop + rowIdx * rowH
                rowBins.forEachIndexed { colIdx, dbm ->
                    val norm = ((dbm + 122f) / 95f).coerceIn(0f, 1f)
                    val cellColor = when {
                        norm > 0.78f -> Color(0xFFFF5252)
                        norm > 0.58f -> Color(0xFFFFB300)
                        norm > 0.35f -> Color(0xFF00E5FF)
                        norm > 0.18f -> Color(0xFF0D47A1)
                        else -> Color(0xFF08162B)
                    }
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(colIdx * colW, y),
                        size = Size(colW + 1f, rowH + 1f)
                    )
                }
            }

            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 23f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    String.format("Пик: %.1f дБм | SNR: +%.1f дБ | %s", frame.peakPowerDbm, frame.snrDb, frame.sMeterLabel),
                    12f,
                    24f,
                    labelPaint
                )
            }
        }
    }
}

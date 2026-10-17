package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AntennaCalculationResult
import com.example.domain.AntennaGeneticSynthesizer
import com.example.domain.Point3D
import com.example.domain.WireRole
import com.example.domain.WireSegment3D
import com.example.domain.formatFrequencyDisplay
import com.example.domain.formatMetricLength
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

private data class ProjectedPoint3D(
    val screen: Offset,
    val depthCam: Float,
    val perspectiveScale: Float
)

private data class DepthSortedSegment(
    val seg: WireSegment3D,
    val p1: ProjectedPoint3D,
    val p2: ProjectedPoint3D,
    val avgDepth: Float
)

/**
 * Full 6-DOF Interactive 3D CAD Viewport supporting unrestricted 360° rotation around ALL 3 AXES:
 * - Yaw (Axis Z / Азимут 0°..360°)
 * - Pitch (Axis X / Тангаж 0°..360° — full vertical tumble including bottom-up view)
 * - Roll (Axis Y / Крен 0°..360° — axial twist via two-finger rotation, outer-ring trackball drag, or Roll mode/slider)
 * Plus pinch-to-zoom, 2D camera pan, depth-sorted Painter's rendering, 3D RF Current Standing Wave I(z),
 * 3D Dimension Callouts, 3D X/Y/Z Orientation Gizmo, and Expandable Large Viewport.
 */
@Composable
fun Interactive3DAntennaViewport(
    calc: AntennaCalculationResult,
    customSegmentsOverride: List<WireSegment3D>? = null,
    customTitleOverride: String? = null,
    modifier: Modifier = Modifier
) {
    // Unrestricted 0..360 deg angles on all 3 axes (Z = Yaw, X = Pitch, Y = Roll)
    var yawZDeg by remember { mutableFloatStateOf(38f) }
    var pitchXDeg by remember { mutableFloatStateOf(24f) }
    var rollYDeg by remember { mutableFloatStateOf(0f) }

    var zoomFactor by remember { mutableFloatStateOf(1.18f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Touch drag behavior: false = Trackball (center XY -> Yaw/Pitch, outer ring -> Roll + two-finger twist),
    // true = Direct Roll/Pitch drag
    var dragRollMode by remember { mutableStateOf(false) }
    var panMode by remember { mutableStateOf(false) }

    // Visual overlays & CAD tools
    var showRadiationDome3D by remember { mutableStateOf(true) }
    var showRfCurrentWave by remember { mutableStateOf(true) }
    var show3DDimensions by remember { mutableStateOf(true) }
    var show3AxisSliders by remember { mutableStateOf(true) }
    var isExpandedHeight by remember { mutableStateOf(false) }

    // Auto-rotation modes: 0 = Off, 1 = Turntable Z, 2 = 3-Axis Gyroscope (X + Y + Z)
    var autoRotateMode by remember { mutableStateOf(0) }

    LaunchedEffect(autoRotateMode) {
        while (autoRotateMode != 0) {
            if (autoRotateMode == 1) {
                yawZDeg = (yawZDeg + 1.8f) % 360f
            } else if (autoRotateMode == 2) {
                yawZDeg = (yawZDeg + 1.6f) % 360f
                pitchXDeg = (pitchXDeg + 0.9f) % 360f
                rollYDeg = (rollYDeg + 0.6f) % 360f
            }
            delay(30L)
        }
    }

    val segments = remember(calc, customSegmentsOverride) {
        customSegmentsOverride ?: AntennaGeneticSynthesizer.build3DSegmentsForAntenna(calc)
    }

    fun normalizeAngle360(deg: Float): Float {
        val m = deg % 360f
        return if (m < 0f) m + 360f else m
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Row 1: Interactive 3D Overlays, Multi-Axis Gyroscope & Viewport Expansion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = autoRotateMode == 2,
                onClick = { autoRotateMode = if (autoRotateMode == 2) 0 else 2 },
                label = {
                    Text(
                        if (autoRotateMode == 2) "Стоп 3D-Гироскоп" else "3D-Вращение (X+Y+Z)",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.testTag("btn_3d_autorotate")
            )
            FilterChip(
                selected = autoRotateMode == 1,
                onClick = { autoRotateMode = if (autoRotateMode == 1) 0 else 1 },
                label = { Text("Турникет Z", style = MaterialTheme.typography.labelMedium) }
            )
            FilterChip(
                selected = dragRollMode,
                onClick = {
                    dragRollMode = !dragRollMode
                    if (dragRollMode) panMode = false
                },
                label = { Text("Ось Y (Крен/Roll)", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("btn_3d_roll_mode")
            )
            FilterChip(
                selected = panMode,
                onClick = {
                    panMode = !panMode
                    if (panMode) dragRollMode = false
                },
                label = { Text("Сдвиг (Pan)", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("btn_3d_pan_mode")
            )
            FilterChip(
                selected = showRfCurrentWave,
                onClick = { showRfCurrentWave = !showRfCurrentWave },
                label = { Text("Ток ВЧ I(z)", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("btn_3d_rf_current")
            )
            FilterChip(
                selected = showRadiationDome3D,
                onClick = { showRadiationDome3D = !showRadiationDome3D },
                label = { Text("3D Купол ДН", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("btn_3d_radiation_dome")
            )
            FilterChip(
                selected = show3DDimensions,
                onClick = { show3DDimensions = !show3DDimensions },
                label = { Text("Размеры 3D", style = MaterialTheme.typography.labelMedium) }
            )
            FilterChip(
                selected = isExpandedHeight,
                onClick = { isExpandedHeight = !isExpandedHeight },
                label = { Text(if (isExpandedHeight) "Обычный вид" else "Крупный чертёж", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.testTag("btn_3d_expand_size")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: 7 Camera Orientation Presets (Isometric, Front, Back, Left/Right, Top, Bottom, Reset)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 38f
                    pitchXDeg = 24f
                    rollYDeg = 0f
                    zoomFactor = 1.18f
                    panX = 0f
                    panY = 0f
                },
                label = { Text("Изометрия", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 0f
                    pitchXDeg = 0f
                    rollYDeg = 0f
                },
                label = { Text("Спереди (+X)", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 180f
                    pitchXDeg = 0f
                    rollYDeg = 0f
                },
                label = { Text("Сзади (-X)", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 90f
                    pitchXDeg = 0f
                    rollYDeg = 0f
                },
                label = { Text("Сбоку (+Y)", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 0f
                    pitchXDeg = 90f
                    rollYDeg = 0f
                },
                label = { Text("Сверху (+Z)", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 0f
                    pitchXDeg = 270f
                    rollYDeg = 0f
                },
                label = { Text("Снизу (-Z)", style = MaterialTheme.typography.labelMedium) }
            )
            AssistChip(
                onClick = {
                    autoRotateMode = 0
                    yawZDeg = 45f
                    pitchXDeg = 35f
                    rollYDeg = 30f
                },
                label = { Text("Диагональ X+Y+Z", style = MaterialTheme.typography.labelMedium) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main 3D CAD Canvas with Full 3-Axis Virtual Trackball & Multi-Touch Transform Gestures
        val viewportHeight = if (isExpandedHeight) 500.dp else 350.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF030812), Color(0xFF0A182D), Color(0xFF050F1E))
                    )
                )
                .pointerInput(dragRollMode, panMode) {
                    detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
                        autoRotateMode = 0
                        // 1. Pinch-to-zoom
                        if (abs(zoom - 1f) > 0.004f) {
                            zoomFactor = (zoomFactor * zoom).coerceIn(0.45f, 3.5f)
                        }
                        // 2. Two-finger twist directly rotates Roll (3rd axis Y)
                        if (abs(rotation) > 0.08f) {
                            rollYDeg = normalizeAngle360(rollYDeg + rotation)
                        }
                        // 3. Single-finger or two-finger drag
                        if (panMode) {
                            panX = (panX + pan.x).coerceIn(-size.width * 0.45f, size.width * 0.45f)
                            panY = (panY + pan.y).coerceIn(-size.height * 0.45f, size.height * 0.45f)
                        } else if (dragRollMode) {
                            // Dedicated 3rd-axis Roll + Pitch control
                            rollYDeg = normalizeAngle360(rollYDeg + pan.x * 0.48f)
                            pitchXDeg = normalizeAngle360(pitchXDeg + pan.y * 0.48f)
                        } else {
                            // Virtual 3D Trackball:
                            // Dragging near the outer border of the viewport smoothly rotates Roll (3rd axis),
                            // while dragging inside the sphere rotates Yaw (Z) and unrestricted 360° Pitch (X)!
                            val cx = size.width * 0.5f
                            val cy = size.height * 0.5f
                            val radius = min(size.width, size.height) * 0.42f
                            val distFromCenter = hypot(centroid.x - cx, centroid.y - cy)

                            if (distFromCenter > radius * 0.88f) {
                                // Outer trackball rim -> tangential drag controls Roll (3rd axis) + slight Yaw/Pitch
                                val cross = ((centroid.x - cx) * pan.y - (centroid.y - cy) * pan.x) / (radius.coerceAtLeast(1f))
                                rollYDeg = normalizeAngle360(rollYDeg + cross * 0.38f)
                                yawZDeg = normalizeAngle360(yawZDeg + pan.x * 0.25f)
                                pitchXDeg = normalizeAngle360(pitchXDeg + pan.y * 0.25f)
                            } else {
                                // Full 360° unrestricted Yaw & Pitch (invert Yaw direction when upside-down for natural feel)
                                val upsideDownSign = if (pitchXDeg in 90f..270f) -1f else 1f
                                yawZDeg = normalizeAngle360(yawZDeg + pan.x * 0.48f * upsideDownSign)
                                pitchXDeg = normalizeAngle360(pitchXDeg + pan.y * 0.48f)
                            }
                        }
                    }
                }
                .testTag("interactive_3d_cad_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f + panX
                val cy = h * 0.54f + panY
                val baseScale = min(w, h) * 0.44f * zoomFactor

                // Full 3-Axis Rotation Matrix: R = R_roll(Y) * R_pitch(X) * R_yaw(Z)
                val yawRad = yawZDeg * PI / 180.0
                val pitchRad = pitchXDeg * PI / 180.0
                val rollRad = rollYDeg * PI / 180.0

                val cz = cos(yawRad).toFloat()
                val sz = sin(yawRad).toFloat()
                val cxRot = cos(pitchRad).toFloat()
                val sxRot = sin(pitchRad).toFloat()
                val cyRot = cos(rollRad).toFloat()
                val syRot = sin(rollRad).toFloat()

                fun transform3D(pt: Point3D): Point3D {
                    // 1. Rotate around Z axis (Yaw / Azimuth)
                    val x1 = pt.x * cz - pt.y * sz
                    val y1 = pt.x * sz + pt.y * cz
                    val z1 = pt.z

                    // 2. Rotate around X axis (Pitch / Elevation 360°)
                    val x2 = x1
                    val y2 = y1 * cxRot - z1 * sxRot
                    val z2 = y1 * sxRot + z1 * cxRot

                    // 3. Rotate around Ycam axis (Roll / Axial Twist 360°)
                    val x3 = x2 * cyRot + z2 * syRot
                    val y3 = y2
                    val z3 = -x2 * syRot + z2 * cyRot
                    return Point3D(x3, y3, z3)
                }

                fun project3D(pt: Point3D): ProjectedPoint3D {
                    val cam = transform3D(pt)
                    // Camera looks along -x3; screen horizontal = y3, screen vertical = z3
                    val persp = (2.8f / (2.8f - cam.x * 0.48f)).coerceIn(0.55f, 1.75f)
                    return ProjectedPoint3D(
                        screen = Offset(
                            x = cx + cam.y * baseScale * persp,
                            y = cy - cam.z * baseScale * persp
                        ),
                        depthCam = cam.x,
                        perspectiveScale = persp
                    )
                }

                // Subtle Virtual Trackball Ring guide
                drawCircle(
                    color = Color(0x2238BDF8),
                    radius = min(w, h) * 0.42f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f))
                )

                // 1. Draw 3D Ground Plane Grid at z = -0.48f
                val groundZ = -0.48f
                val gridColor = Color(0xFF143154)
                for (i in -4..4) {
                    val g = i * 0.16f
                    val pStart1 = project3D(Point3D(-0.64f, g, groundZ)).screen
                    val pEnd1 = project3D(Point3D(0.64f, g, groundZ)).screen
                    drawLine(gridColor, pStart1, pEnd1, strokeWidth = 1.2f)

                    val pStart2 = project3D(Point3D(g, -0.64f, groundZ)).screen
                    val pEnd2 = project3D(Point3D(g, 0.64f, groundZ)).screen
                    drawLine(gridColor, pStart2, pEnd2, strokeWidth = 1.2f)
                }

                // 2. Draw Mast Support & Main Beam Direction Arrow (+X)
                val originGround = project3D(Point3D(0f, 0f, groundZ)).screen
                val mastTop = project3D(Point3D(0f, 0f, 0f)).screen
                drawLine(
                    color = Color(0xFF64748B),
                    start = originGround,
                    end = mastTop,
                    strokeWidth = 4.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                val xAxisEnd = project3D(Point3D(0.72f, 0f, groundZ)).screen
                drawLine(Color(0xBB10B981), originGround, xAxisEnd, strokeWidth = 3f)

                // 3. Optional 3D Volumetric Radiation Lobe Wireframe (Azimuth + Elevation 3D Shells)
                if (showRadiationDome3D && calc.polarPattern.isNotEmpty()) {
                    for (elevStep in listOf(-0.14f, 0.08f, 0.26f)) {
                        val lobePath = Path()
                        calc.polarPattern.forEachIndexed { idx, pt ->
                            val rad = pt.angleDeg * PI / 180.0
                            val rNorm = ((pt.azimuthGainNormalizedDb + 38.0).coerceIn(0.0, 38.0) / 38.0).toFloat() * 0.58f
                            val lx = rNorm * cos(rad).toFloat()
                            val ly = rNorm * sin(rad).toFloat()
                            val proj = project3D(Point3D(lx, ly, elevStep * rNorm)).screen
                            if (idx == 0) lobePath.moveTo(proj.x, proj.y) else lobePath.lineTo(proj.x, proj.y)
                        }
                        lobePath.close()
                        drawPath(
                            path = lobePath,
                            color = if (elevStep > 0f) Color(0x4000E5FF) else Color(0x2838BDF8),
                            style = Stroke(width = 1.8f)
                        )
                    }
                }

                // 4. Depth-Sorted 3D Metallic Antenna Elements (Painter's Algorithm back-to-front)
                val sortedSegments = segments.map { seg ->
                    val p1 = project3D(seg.start)
                    val p2 = project3D(seg.end)
                    DepthSortedSegment(
                        seg = seg,
                        p1 = p1,
                        p2 = p2,
                        avgDepth = (p1.depthCam + p2.depthCam) * 0.5f
                    )
                }.sortedBy { it.avgDepth }

                val labelPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    textSize = 23f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                }
                val dimPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#FFD54F")
                    textSize = 21f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                }

                sortedSegments.forEach { item ->
                    val seg = item.seg
                    val p1 = item.p1.screen
                    val p2 = item.p2.screen
                    val perspScale = ((item.p1.perspectiveScale + item.p2.perspectiveScale) * 0.5f).coerceIn(0.7f, 1.45f)

                    val (outerColor, coreColor, baseWidthPx) = when (seg.role) {
                        WireRole.DRIVEN -> Triple(Color(0xFF00E5FF), Color(0xFFE0FFFF), 8.0f)
                        WireRole.REFLECTOR -> Triple(Color(0xFFFFB300), Color(0xFFFFF3CD), 6.8f)
                        WireRole.DIRECTOR -> Triple(Color(0xFF38BDF8), Color(0xFFE0F2FE), 6.2f)
                        WireRole.RADIAL_OR_BOOM -> Triple(Color(0xFF94A3B8), Color(0xFFE2E8F0), 5.2f)
                        WireRole.FEEDPOINT_COIL -> Triple(Color(0xFF10B981), Color(0xFFD1FAE5), 5.8f)
                    }
                    val widthPx = baseWidthPx * perspScale

                    // Metallic cylinder shadow + outer body + specular core highlight
                    drawLine(
                        color = Color(0x88000000),
                        start = Offset(p1.x + 2f, p1.y + 3f),
                        end = Offset(p2.x + 2f, p2.y + 3f),
                        strokeWidth = widthPx + 2f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = outerColor,
                        start = p1,
                        end = p2,
                        strokeWidth = widthPx,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = coreColor,
                        start = p1,
                        end = p2,
                        strokeWidth = widthPx * 0.36f,
                        cap = StrokeCap.Round
                    )

                    // Optional RF Standing Wave Current Envelope I(z) on Driven & Director/Reflector elements
                    if (showRfCurrentWave && seg.role in listOf(WireRole.DRIVEN, WireRole.REFLECTOR, WireRole.DIRECTOR)) {
                        val steps = 12
                        val wavePath = Path()
                        for (s in 0..steps) {
                            val t = s.toFloat() / steps
                            val wx = seg.start.x + (seg.end.x - seg.start.x) * t
                            val wy = seg.start.y + (seg.end.y - seg.start.y) * t
                            val wz = seg.start.z + (seg.end.z - seg.start.z) * t
                            // Sinusoidal standing wave peaking in center of half-wave element
                            val amp = sin(t * PI).toFloat() * (if (seg.role == WireRole.DRIVEN) 0.085f else 0.05f)
                            val wPt = project3D(Point3D(wx, wy, wz + amp)).screen
                            if (s == 0) wavePath.moveTo(wPt.x, wPt.y) else wavePath.lineTo(wPt.x, wPt.y)
                        }
                        drawPath(
                            path = wavePath,
                            color = if (seg.role == WireRole.DRIVEN) Color(0xCCFF5252) else Color(0x99FFB300),
                            style = Stroke(width = 2.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f))
                        )
                    }

                    // Ceramic insulator end-caps
                    drawCircle(color = outerColor, radius = 4.4f * perspScale, center = p1)
                    drawCircle(color = outerColor, radius = 4.4f * perspScale, center = p2)

                    if (seg.label.isNotEmpty()) {
                        val midX = (p1.x + p2.x) * 0.5f
                        val midY = (p1.y + p2.y) * 0.5f
                        drawContext.canvas.nativeCanvas.drawText(seg.label, midX + 8f, midY - 8f, labelPaint)
                    }
                }

                // 5. Optional 3D Dimension Callouts in 3D space
                if (show3DDimensions && calc.dimensions.isNotEmpty()) {
                    val mainDim = calc.dimensions.first()
                    val dLeft = project3D(Point3D(0f, -0.44f, 0.36f)).screen
                    val dRight = project3D(Point3D(0f, 0.44f, 0.36f)).screen
                    drawLine(
                        color = Color(0xCCFFD54F),
                        start = dLeft,
                        end = dRight,
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    )
                    drawCircle(Color(0xFFFFD54F), radius = 3.5f, center = dLeft)
                    drawCircle(Color(0xFFFFD54F), radius = 3.5f, center = dRight)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${mainDim.code}: ${mainDim.formattedLength()}",
                        (dLeft.x + dRight.x) * 0.5f - 65f,
                        (dLeft.y + dRight.y) * 0.5f - 8f,
                        dimPaint
                    )
                }

                // 6. Feedpoint Balun Beacon at center
                val feedCenter = project3D(Point3D(0f, 0f, 0f)).screen
                drawCircle(Color(0xFFFF5252), radius = 9f, center = feedCenter)
                drawCircle(Color.White, radius = 3.8f, center = feedCenter)

                // 7. 3D Coordinate Orientation Gizmo (X/Y/Z Triad in top-right corner)
                val gizmoOrigin = Offset(w - 54f, 56f)
                val gizmoLen = 34f
                val gxDir = transform3D(Point3D(1f, 0f, 0f))
                val gyDir = transform3D(Point3D(0f, 1f, 0f))
                val gzDir = transform3D(Point3D(0f, 0f, 1f))

                val gxEnd = Offset(gizmoOrigin.x + gxDir.y * gizmoLen, gizmoOrigin.y - gxDir.z * gizmoLen)
                val gyEnd = Offset(gizmoOrigin.x + gyDir.y * gizmoLen, gizmoOrigin.y - gyDir.z * gizmoLen)
                val gzEnd = Offset(gizmoOrigin.x + gzDir.y * gizmoLen, gizmoOrigin.y - gzDir.z * gizmoLen)

                drawCircle(Color(0x660F172A), radius = 42f, center = gizmoOrigin)
                drawLine(Color(0xFFFF5252), gizmoOrigin, gxEnd, strokeWidth = 3.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFF10B981), gizmoOrigin, gyEnd, strokeWidth = 3.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFF38BDF8), gizmoOrigin, gzEnd, strokeWidth = 3.5f, cap = StrokeCap.Round)

                val gizmoPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 19f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.apply {
                    gizmoPaint.color = android.graphics.Color.parseColor("#FF5252")
                    drawText("X", gxEnd.x + 3f, gxEnd.y + 5f, gizmoPaint)
                    gizmoPaint.color = android.graphics.Color.parseColor("#10B981")
                    drawText("Y", gyEnd.x + 3f, gyEnd.y + 5f, gizmoPaint)
                    gizmoPaint.color = android.graphics.Color.parseColor("#38BDF8")
                    drawText("Z", gzEnd.x + 3f, gzEnd.y + 5f, gizmoPaint)
                }

                // 8. HUD Engineering Readout Overlay
                val hudPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#B8F4FF")
                    textSize = 21f
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val titleText = customTitleOverride ?: calc.antennaType.titleRu
                drawContext.canvas.nativeCanvas.apply {
                    drawText("3D САПР (6-DOF): $titleText", 14f, 26f, labelPaint)
                    drawText(
                        String.format(
                            "Оси X:%.0f° Y(Крен):%.0f° Z:%.0f° | Масштаб: %.2fx",
                            pitchXDeg,
                            rollYDeg,
                            yawZDeg,
                            zoomFactor
                        ),
                        14f,
                        50f,
                        hudPaint
                    )
                    drawText(
                        "F0=${formatFrequencyDisplay(calc.frequencyMhz)} • H=${formatMetricLength(calc.mastHeightMeters)} • 1 палец: 3D-шар, 2 пальца: зум/крен/сдвиг",
                        14f,
                        h - 14f,
                        hudPaint
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Direct 3-Axis Precision Sliders (Z, X, Y) + Zoom Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Точное управление по 3 осям (X / Y / Z на 360°) и Масштаб",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AssistChip(
                        onClick = { show3AxisSliders = !show3AxisSliders },
                        label = { Text(if (show3AxisSliders) "Свернуть оси" else "Показать 3 оси", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                if (show3AxisSliders) {
                    // Axis Z (Yaw / Azimuth)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = String.format("Ось Z (Горизонт): %3.0f°", yawZDeg),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(142.dp)
                        )
                        Slider(
                            value = yawZDeg,
                            onValueChange = {
                                autoRotateMode = 0
                                yawZDeg = it
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("slider_3d_yaw_z")
                        )
                    }

                    // Axis X (Pitch / Full 360 Vertical Tumble)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = String.format("Ось X (Наклон):   %3.0f°", pitchXDeg),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(142.dp)
                        )
                        Slider(
                            value = pitchXDeg,
                            onValueChange = {
                                autoRotateMode = 0
                                pitchXDeg = it
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("slider_3d_pitch_x")
                        )
                    }

                    // Axis Y (Roll / 3rd Axis Twist 360)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = String.format("Ось Y (Крен):     %3.0f°", rollYDeg),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(142.dp)
                        )
                        Slider(
                            value = rollYDeg,
                            onValueChange = {
                                autoRotateMode = 0
                                rollYDeg = it
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .testTag("slider_3d_roll_y")
                        )
                    }
                }

                // Zoom Slider
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = String.format("Масштаб (Зум):   %.2fx", zoomFactor),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(142.dp)
                    )
                    Slider(
                        value = zoomFactor,
                        onValueChange = { zoomFactor = it },
                        valueRange = 0.45f..3.5f,
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("slider_3d_zoom")
                    )
                }
            }
        }
    }
}

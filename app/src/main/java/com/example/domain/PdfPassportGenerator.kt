package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class PdfExportOutcome(
    val file: File,
    val fileName: String,
    val sizeBytes: Long
)

object PdfPassportGenerator {

    /**
     * Generates a 2-page professional RF Engineering PDF Passport containing:
     * Page 1: Title block, key electrical parameters, full dimension table, and vector blueprint.
     * Page 2: SWR & Impedance graph, Polar radiation pattern diagram, and step-by-step assembly & VNA tuning guide.
     */
    fun generatePassportPdf(
        context: Context,
        calc: AntennaCalculationResult,
        vnaOpt: VnaOptimizationResult?
    ): Result<PdfExportOutcome> {
        return try {
            val doc = PdfDocument()
            val pageWidth = 595 // A4 width in points (72 dpi)
            val pageHeight = 842 // A4 height in points

            // --- PAGE 1: BLUEPRINT & DIMENSIONS ---
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = doc.startPage(pageInfo1)
            drawPage1BlueprintAndSpecs(page1.canvas, pageWidth, pageHeight, calc)
            doc.finishPage(page1)

            // --- PAGE 2: SWR CURVE, POLAR PATTERN & ASSEMBLY INSTRUCTIONS ---
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = doc.startPage(pageInfo2)
            drawPage2PlotsAndAssembly(page2.canvas, pageWidth, pageHeight, calc, vnaOpt)
            doc.finishPage(page2)

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeCode = calc.antennaType.code.lowercase(Locale.ROOT)
            val fileName = "RF_Antenna_Passport_${safeCode}_${String.format(Locale.US, "%.3fMHz", calc.frequencyMhz)}.pdf"
            val outFile = File(exportDir, fileName)
            FileOutputStream(outFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()

            Result.success(
                PdfExportOutcome(
                    file = outFile,
                    fileName = fileName,
                    sizeBytes = outFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Инженерный паспорт антенны — ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Экспорт PDF-паспорта антенны").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun drawPage1BlueprintAndSpecs(
        canvas: Canvas,
        width: Int,
        height: Int,
        calc: AntennaCalculationResult
    ) {
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Engineering border frame (GOST / IEEE style)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1528")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

        // Header banner
        val headerBg = Paint().apply { color = Color.parseColor("#0B1528") }
        canvas.drawRect(24f, 24f, width - 24f, 92f, headerBg)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("RF ANTENNA LAB • ИНЖЕНЕРНЫЙ ПАСПОРТ И ЧЕРТЁЖ АНТЕННЫ", 38f, 52f, titlePaint)
        canvas.drawText(
            "${calc.antennaType.titleRu}  |  F0 = ${formatFrequencyDisplay(calc.frequencyMhz)}  |  λ = ${formatMetricLength(calc.wavelengthMeters)}  |  $dateStr",
            38f,
            74f,
            subPaint
        )

        // Section 1: Electrical & Physical Summary Box
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1528")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        var y = 116f
        canvas.drawText("1. РАСЧЁТНЫЕ ЭЛЕКТРОДИНАМИЧЕСКИЕ ПАРАМЕТРЫ", 38f, y, sectionTitlePaint)
        y += 16f

        val specs = listOf(
            "Рабочая частота (F0): ${formatFrequencyDisplay(calc.frequencyMhz)}",
            "Длина волны в вакууме (λ): ${formatMetricLength(calc.wavelengthMeters)}",
            String.format("Входной импеданс (Zin): %.1f %+.1fj Ом", calc.centerImpedanceR, calc.centerImpedanceX),
            String.format("КСВ в тракте 50 Ом: %.2f (Полоса по КСВ≤2: %.1f кГц)", calc.centerSwr50, calc.bandwidthSwr2Khz),
            String.format("Коэффициент усиления: %.2f dBi (F/B: %.1f дБ)", calc.gainDbi, calc.frontToBackDb),
            String.format("Высота подвеса / мачты: %.2f м (Угол излучения: %.1f°)", calc.mastHeightMeters, calc.takeOffAngleDeg),
            String.format("Диаметр проводника: %.2f мм  |  КУ: %.2f  |  КПД: %.1f%%", calc.wireDiameterMm, calc.velocityFactor, calc.radiationEfficiencyPct)
        )
        specs.forEach { line ->
            canvas.drawText("• $line", 44f, y, bodyPaint)
            y += 15f
        }

        // Section 2: Vector Engineering Blueprint Box
        y += 10f
        canvas.drawText("2. СБОРОЧНЫЙ ЭСКИЗ И ГЕОМЕТРИЧЕСКАЯ СХЕМА (МАСШТАБНЫЙ ЧЕРТЁЖ)", 38f, y, sectionTitlePaint)
        y += 10f
        val blueprintRect = RectF(38f, y, width - 38f, y + 210f)
        val blueprintBg = Paint().apply { color = Color.parseColor("#0B1528") }
        canvas.drawRoundRect(blueprintRect, 8f, 8f, blueprintBg)

        drawPdfAntennaSchematic(canvas, blueprintRect, calc)
        y += 228f

        // Section 3: Table of Dimensions
        canvas.drawText("3. СПЕЦИФИКАЦИЯ РАЗМЕРОВ ЭЛЕМЕНТОВ", 38f, y, sectionTitlePaint)
        y += 14f

        val rowStroke = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        canvas.drawText("Код", 42f, y, monoPaint)
        canvas.drawText("Наименование узла / элемента", 95f, y, monoPaint)
        canvas.drawText("Размер (м / мм)", 315f, y, monoPaint)
        canvas.drawText("Позиция / Примечание", 435f, y, monoPaint)
        y += 6f
        canvas.drawLine(38f, y, width - 38f, y, rowStroke)
        y += 14f

        calc.dimensions.take(12).forEach { dim ->
            canvas.drawText(dim.code, 42f, y, monoPaint)
            canvas.drawText(dim.labelRu.take(36), 95f, y, bodyPaint)
            canvas.drawText(dim.formattedLength(), 315f, y, monoPaint)
            val noteText = if (dim.positionMeters > 0.0) "X=${dim.formattedPosition()}" else dim.noteRu.take(24)
            canvas.drawText(noteText, 435f, y, bodyPaint)
            y += 16f
        }

        // Footer
        canvas.drawText("Лист 1 из 2 • Сформировано в RF Antenna Lab (1 Гц – 6 ГГц)", 38f, height - 34f, bodyPaint)
    }

    private fun drawPdfAntennaSchematic(canvas: Canvas, rect: RectF, calc: AntennaCalculationResult) {
        val gridPaint = Paint().apply {
            color = Color.parseColor("#1E3A5F")
            strokeWidth = 0.7f
        }
        val step = 25f
        var gx = rect.left + step
        while (gx < rect.right) {
            canvas.drawLine(gx, rect.top, gx, rect.bottom, gridPaint)
            gx += step
        }
        var gy = rect.top + step
        while (gy < rect.bottom) {
            canvas.drawLine(rect.left, gy, rect.right, gy, gridPaint)
            gy += step
        }

        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val amberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB300")
            strokeWidth = 2.2f
            style = Paint.Style.STROKE
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val cx = rect.centerX()
        val cy = rect.centerY()

        when (calc.antennaType) {
            AntennaType.DIPOLE, AntennaType.T2FD_BEVERAGE -> {
                val halfSpan = rect.width() * 0.36f
                canvas.drawLine(cx - halfSpan, cy - 15f, cx - 8f, cy - 15f, cyanPaint)
                canvas.drawLine(cx + 8f, cy - 15f, cx + halfSpan, cy - 15f, cyanPaint)
                canvas.drawCircle(cx, cy - 15f, 6f, amberPaint)
                canvas.drawLine(cx, cy - 9f, cx, rect.bottom - 18f, amberPaint)
                val lTotal = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                canvas.drawText("L = $lTotal", cx - 55f, cy - 30f, labelPaint)
                canvas.drawText("Балун 50 Ом (H = ${formatMetricLength(calc.mastHeightMeters)})", cx + 10f, cy + 35f, labelPaint)
            }
            AntennaType.YAGI, AntennaType.LPDA -> {
                val boomStart = rect.left + 60f
                val boomEnd = rect.right - 60f
                canvas.drawLine(boomStart, cy, boomEnd, cy, amberPaint)
                val elems = calc.dimensions.filter { it.code != "BOOM" }
                val count = elems.size.coerceAtLeast(2)
                elems.forEachIndexed { idx, item ->
                    val x = boomStart + (boomEnd - boomStart) * (idx.toFloat() / (count - 1).coerceAtLeast(1))
                    val halfH = (68f - idx * 4.5f).coerceAtLeast(24f)
                    canvas.drawLine(x, cy - halfH, x, cy + halfH, cyanPaint)
                    canvas.drawText(item.code, x - 10f, cy - halfH - 6f, labelPaint)
                }
            }
            AntennaType.MAG_LOOP -> {
                canvas.drawCircle(cx, cy, 62f, cyanPaint)
                canvas.drawCircle(cx, cy + 42f, 18f, amberPaint)
                canvas.drawRect(cx - 12f, cy - 70f, cx + 12f, cy - 54f, amberPaint)
                canvas.drawText("КПЕ ${String.format("%.1f пФ", calc.tuningCapacitorPf ?: 0.0)}", cx + 18f, cy - 58f, labelPaint)
                canvas.drawText("Петля связи d=D/5", cx + 24f, cy + 48f, labelPaint)
            }
            else -> {
                // Vertical / Discone / Quad / Helical universal technical diagram
                canvas.drawLine(cx, rect.top + 28f, cx, cy + 10f, cyanPaint)
                canvas.drawLine(cx, cy + 10f, cx - 80f, rect.bottom - 24f, amberPaint)
                canvas.drawLine(cx, cy + 10f, cx + 80f, rect.bottom - 24f, amberPaint)
                canvas.drawCircle(cx, cy + 10f, 6f, amberPaint)
                val d0 = calc.dimensions.firstOrNull()?.formattedLength() ?: ""
                canvas.drawText("${calc.antennaType.code}: $d0", cx + 12f, cy - 20f, labelPaint)
            }
        }
    }

    private fun drawPage2PlotsAndAssembly(
        canvas: Canvas,
        width: Int,
        height: Int,
        calc: AntennaCalculationResult,
        vnaOpt: VnaOptimizationResult?
    ) {
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1528")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1528")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
        }

        var y = 52f
        canvas.drawText("4. ГРАФИК КСВ(f) И ПОЛЯРНАЯ ДИАГРАММА НАПРАВЛЕННОСТИ", 38f, y, sectionTitlePaint)
        y += 14f

        // Left box: SWR plot; Right box: Polar pattern
        val swrRect = RectF(38f, y, 310f, y + 190f)
        val polarRect = RectF(325f, y, width - 38f, y + 190f)
        val boxBg = Paint().apply { color = Color.parseColor("#0B1528") }
        canvas.drawRoundRect(swrRect, 8f, 8f, boxBg)
        canvas.drawRoundRect(polarRect, 8f, 8f, boxBg)

        // Draw SWR curve inside swrRect
        val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val swrPath = Path()
        calc.sweepPoints.forEachIndexed { idx, pt ->
            val px = swrRect.left + 16f + (swrRect.width() - 32f) * (idx.toFloat() / (calc.sweepPoints.size - 1).coerceAtLeast(1))
            val normSwr = ((pt.swr50.coerceIn(1.0, 6.0) - 1.0) / 5.0).toFloat()
            val py = swrRect.bottom - 20f - (swrRect.height() - 40f) * (1f - normSwr)
            if (idx == 0) swrPath.moveTo(px, py) else swrPath.lineTo(px, py)
        }
        canvas.drawPath(swrPath, curvePaint)

        // Draw Polar pattern inside polarRect
        val pcx = polarRect.centerX()
        val pcy = polarRect.centerY()
        val maxR = polarRect.height() * 0.38f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawCircle(pcx, pcy, maxR, ringPaint)
        canvas.drawCircle(pcx, pcy, maxR * 0.66f, ringPaint)

        val polarPath = Path()
        calc.polarPattern.forEachIndexed { idx, pt ->
            val rad = (pt.angleDeg - 90) * PI / 180.0
            val normGain = ((pt.azimuthGainNormalizedDb + 35.0).coerceIn(0.0, 35.0) / 35.0).toFloat()
            val r = maxR * normGain
            val px = (pcx + r * cos(rad)).toFloat()
            val py = (pcy + r * sin(rad)).toFloat()
            if (idx == 0) polarPath.moveTo(px, py) else polarPath.lineTo(px, py)
        }
        polarPath.close()
        canvas.drawPath(polarPath, curvePaint)

        y += 214f
        canvas.drawText("5. ФИЗИЧЕСКОЕ ОПИСАНИЕ И СХЕМА СОГЛАСОВАНИЯ", 38f, y, sectionTitlePaint)
        y += 16f
        drawWrappedText(canvas, calc.physicsDescriptionRu, 42f, y, width - 84f, bodyPaint).also { y = it + 8f }
        drawWrappedText(canvas, "Согласование: ${calc.matchingAdviceRu}", 42f, y, width - 84f, bodyPaint).also { y = it + 14f }

        canvas.drawText("6. ПОШАГОВАЯ ИНСТРУКЦИЯ ПО СБОРКЕ И НАСТРОЙКЕ", 38f, y, sectionTitlePaint)
        y += 16f
        calc.assemblyStepsRu.forEach { step ->
            y = drawWrappedText(canvas, step, 42f, y, width - 84f, bodyPaint) + 6f
        }

        if (vnaOpt != null) {
            y += 8f
            canvas.drawText("7. ПРОТОКОЛ АВТОМАТИЧЕСКОЙ ОПТИМИЗАЦИИ ПО ИЗМЕРЕНИЯМ VNA", 38f, y, sectionTitlePaint)
            y += 16f
            y = drawWrappedText(canvas, vnaOpt.actionSummaryRu, 42f, y, width - 84f, bodyPaint) + 6f
            vnaOpt.stepByStepTuningRu.take(3).forEach { tuneStep ->
                y = drawWrappedText(canvas, tuneStep, 42f, y, width - 84f, bodyPaint) + 4f
            }
        }

        canvas.drawText("Лист 2 из 2 • RF Antenna Lab • Инженерный расчёт и синтез антенн", 38f, height - 34f, bodyPaint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var y = startY
        val words = text.split(" ")
        var currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                canvas.drawText(currentLine.toString(), x, y, paint)
                y += 14f
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), x, y, paint)
            y += 14f
        }
        return y
    }
}

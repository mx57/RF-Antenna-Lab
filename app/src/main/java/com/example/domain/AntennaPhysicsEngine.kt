package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class AntennaCategory(val titleRu: String) {
    DIPOLE_VERT("Диполи и Вертикалы"),
    DIRECTIONAL("Направленные (Волновой канал / Квадрат)"),
    ALL_WAVE("Всеволновые и Широкополосные"),
    LOOP_SHF("Магнитные рамки, КНЧ и СВЧ")
}

enum class AntennaType(
    val code: String,
    val titleRu: String,
    val subtitleRu: String,
    val category: AntennaCategory,
    val minFreqMhz: Double,
    val maxFreqMhz: Double,
    val defaultElements: Int,
    val isAllWave: Boolean = false
) {
    DIPOLE(
        code = "DIPOLE",
        titleRu = "Полуволновый диполь / Inverted-V",
        subtitleRu = "Классический резонансный вибратор λ/2 с симметричным питанием",
        category = AntennaCategory.DIPOLE_VERT,
        minFreqMhz = 0.001,
        maxFreqMhz = 6000.0,
        defaultElements = 1
    ),
    GROUND_PLANE(
        code = "GP",
        titleRu = "Четвертьволновый штырь (Ground Plane)",
        subtitleRu = "Всенаправленный вертикал λ/4 с 4 наклонными противовесами 135° (50 Ом)",
        category = AntennaCategory.DIPOLE_VERT,
        minFreqMhz = 0.1,
        maxFreqMhz = 6000.0,
        defaultElements = 5
    ),
    J_POLE(
        code = "J_POLE",
        titleRu = "J-Антенна / Коллинеар Slim Jim",
        subtitleRu = "Полуволновый вертикальный излучатель с четвертьволновым согласующим шлейфом",
        category = AntennaCategory.DIPOLE_VERT,
        minFreqMhz = 14.0,
        maxFreqMhz = 3000.0,
        defaultElements = 2
    ),
    YAGI(
        code = "YAGI",
        titleRu = "Волновой канал (Уда-Яги 3..15 эл.)",
        subtitleRu = "Высокоэффективная направленная антенна с рефлектором, вибратором и директорами",
        category = AntennaCategory.DIRECTIONAL,
        minFreqMhz = 1.8,
        maxFreqMhz = 6000.0,
        defaultElements = 5
    ),
    CUBICAL_QUAD(
        code = "QUAD",
        titleRu = "Двойной квадрат (Cubical Quad / Moxon)",
        subtitleRu = "Замкнутые рамочные элементы с периметром ~1λ и низким углом излучения",
        category = AntennaCategory.DIRECTIONAL,
        minFreqMhz = 3.5,
        maxFreqMhz = 3000.0,
        defaultElements = 2
    ),
    DISCONE(
        code = "DISCONE",
        titleRu = "Дискоконусная всеволновая (Discone)",
        subtitleRu = "Сверхширокополосная всеволновая антенна с перекрытием до 10:1 по частоте",
        category = AntennaCategory.ALL_WAVE,
        minFreqMhz = 20.0,
        maxFreqMhz = 6000.0,
        defaultElements = 9,
        isAllWave = true
    ),
    LPDA(
        code = "LPDA",
        titleRu = "Логопериодическая всеволновая (LPDA)",
        subtitleRu = "Направленная всеволновая антенна (τ=0.86, σ=0.15) с постоянным КСВ в широкой полосе",
        category = AntennaCategory.ALL_WAVE,
        minFreqMhz = 1.0,
        maxFreqMhz = 6000.0,
        defaultElements = 8,
        isAllWave = true
    ),
    T2FD_BEVERAGE(
        code = "T2FD",
        titleRu = "Всеволновый T2FD / Бегущей волны",
        subtitleRu = "Широкополосный наклонный петлевой диполь с безындукционным резистором 390–450 Ом",
        category = AntennaCategory.ALL_WAVE,
        minFreqMhz = 0.01,
        maxFreqMhz = 60.0,
        defaultElements = 2,
        isAllWave = true
    ),
    MAG_LOOP(
        code = "MAG_LOOP",
        titleRu = "Магнитная рамка (Magnetic Loop + КНЧ/СВ)",
        subtitleRu = "Узкополосная высокодобротная рамка малого размера (1 Гц – 60 МГц) с КПЕ",
        category = AntennaCategory.LOOP_SHF,
        minFreqMhz = 0.000001, // 1 Hz!
        maxFreqMhz = 145.0,
        defaultElements = 2
    ),
    HELICAL_PATCH(
        code = "HELICAL",
        titleRu = "Спиральная СВЧ (Helical) / Патч (до 6 ГГц)",
        subtitleRu = "Антенна круговой поляризации для спутников, L-диапазона, Wi-Fi и FPV 1.2–6.0 ГГц",
        category = AntennaCategory.LOOP_SHF,
        minFreqMhz = 400.0,
        maxFreqMhz = 6000.0,
        defaultElements = 6
    );

    companion object {
        fun fromCode(code: String): AntennaType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DIPOLE
    }
}

data class FrequencyBandPreset(
    val id: String,
    val labelRu: String,
    val freqMhz: Double,
    val descriptionRu: String
)

val ALL_BAND_PRESETS = listOf(
    FrequencyBandPreset("ELF_7HZ", "КНЧ 7.83 Гц (Резонанс Шумана)", 0.00000783, "Сверхнизкие частоты (1 Гц – 30 Гц), приём геомагнитных волн"),
    FrequencyBandPreset("VLF_24KHZ", "СНЧ 24 кГц (Радиостанции СДВ)", 0.024, "Сверхдлинные волны (3–30 кГц), морская связь и эталоны времени"),
    FrequencyBandPreset("LF_136KHZ", "ДВ 2200м (136.5 кГц)", 0.1365, "Любительский ДВ диапазон"),
    FrequencyBandPreset("MF_475KHZ", "СВ 630м (475 кГц)", 0.475, "Средневолновый любительский диапазон"),
    FrequencyBandPreset("HF_160M", "КВ 160м (1.85 МГц)", 1.85, "Ночной DX-диапазон «Топ-бэнд»"),
    FrequencyBandPreset("HF_80M", "КВ 80м (3.65 МГц)", 3.65, "Региональная и ближняя КВ связь"),
    FrequencyBandPreset("HF_40M", "КВ 40м (7.10 МГц)", 7.10, "Круглосуточный всемирный КВ диапазон"),
    FrequencyBandPreset("HF_20M", "КВ 20м (14.175 МГц)", 14.175, "Главный дневной дальний DX-диапазон"),
    FrequencyBandPreset("HF_15M", "КВ 15м (21.20 МГц)", 21.20, "Дальние межконтинентальные трассы"),
    FrequencyBandPreset("HF_11M_CB", "Си-Би 27 МГц (27.20 МГц)", 27.20, "Гражданский диапазон 11 метров"),
    FrequencyBandPreset("HF_10M", "КВ 10м (28.50 МГц)", 28.50, "Переходный КВ/УКВ диапазон"),
    FrequencyBandPreset("VHF_6M", "УКВ 6м (50.15 МГц)", 50.15, "«Магический диапазон» Спорадик-Е"),
    FrequencyBandPreset("VHF_AIR", "Авиа 118–136 МГц (124.0 МГц)", 124.0, "Авиационный диапазон АМ"),
    FrequencyBandPreset("VHF_2M", "УКВ 2м (145.50 МГц)", 145.50, "Основной любительский УКВ диапазон"),
    FrequencyBandPreset("UHF_70CM", "ДМВ 70см (433.50 МГц)", 433.50, "Любительский диапазон 70 см, LPD/PMR и спутники"),
    FrequencyBandPreset("UHF_868", "ISM / LoRa 868 МГц", 868.0, "Телеметрия, Meshtastic и IoT"),
    FrequencyBandPreset("SHF_23CM", "СВЧ 23см (1296 МГц)", 1296.0, "СВЧ связь, EME (Луна) и спутники"),
    FrequencyBandPreset("SHF_2G4", "СВЧ 13см (2425 МГц)", 2425.0, "QO-100 Uplink, Wi-Fi и ISM 2.4 ГГц"),
    FrequencyBandPreset("SHF_5G8", "СВЧ 5см (5800 МГц)", 5800.0, "Широкополосные радиомосты, FPV и СВЧ до 6 ГГц")
)

data class AntennaDimensionItem(
    val code: String,
    val labelRu: String,
    val valueMeters: Double,
    val positionMeters: Double = 0.0,
    val noteRu: String = ""
) {
    fun formattedLength(): String = formatMetricLength(valueMeters)
    fun formattedPosition(): String = formatMetricLength(positionMeters)
}

fun formatMetricLength(meters: Double): String {
    val absM = abs(meters)
    return when {
        absM >= 1000.0 -> String.format("%.2f км (%.1f м)", meters / 1000.0, meters)
        absM >= 1.0 -> String.format("%.3f м (%.0f мм)", meters, meters * 1000.0)
        absM >= 0.01 -> String.format("%.1f мм (%.2f см)", meters * 1000.0, meters * 100.0)
        else -> String.format("%.2f мм", meters * 1000.0)
    }
}

fun formatFrequencyDisplay(freqMhz: Double): String {
    return when {
        freqMhz < 0.001 -> String.format("%.2f Гц", freqMhz * 1_000_000.0)
        freqMhz < 1.0 -> String.format("%.3f кГц", freqMhz * 1_000.0)
        freqMhz >= 1000.0 -> String.format("%.4f ГГц (%.1f МГц)", freqMhz / 1000.0, freqMhz)
        else -> String.format("%.4f МГц", freqMhz)
    }
}

data class ImpedancePoint(
    val freqMhz: Double,
    val rOhms: Double,
    val xOhms: Double,
    val swr50: Double,
    val returnLossDb: Double,
    val gammaReal: Double,
    val gammaImag: Double
)

data class PolarPatternPoint(
    val angleDeg: Int,
    val azimuthGainNormalizedDb: Double, // 0 dB down to -40 dB
    val elevationGainNormalizedDb: Double
)

data class AntennaCalculationResult(
    val antennaType: AntennaType,
    val frequencyMhz: Double,
    val upperFreqMhz: Double,
    val wavelengthMeters: Double,
    val wireDiameterMm: Double,
    val mastHeightMeters: Double,
    val elementCount: Int,
    val velocityFactor: Double,
    val txPowerWatts: Double,
    val dimensions: List<AntennaDimensionItem>,
    val centerImpedanceR: Double,
    val centerImpedanceX: Double,
    val centerSwr50: Double,
    val bandwidthSwr2Khz: Double,
    val gainDbi: Double,
    val frontToBackDb: Double,
    val radiationEfficiencyPct: Double,
    val takeOffAngleDeg: Double,
    val tuningCapacitorPf: Double?,
    val loopCapacitorVoltageV: Double?,
    val sweepPoints: List<ImpedancePoint>,
    val polarPattern: List<PolarPatternPoint>,
    val physicsDescriptionRu: String,
    val assemblyStepsRu: List<String>,
    val matchingAdviceRu: String
)

object AntennaPhysicsEngine {
    private const val SPEED_OF_LIGHT = 299.792458 // m * MHz

    fun calculate(
        antennaType: AntennaType,
        frequencyMhz: Double,
        upperFreqMhz: Double = frequencyMhz * 3.0,
        wireDiameterMm: Double = 2.0,
        mastHeightMeters: Double = 10.0,
        elementCount: Int = antennaType.defaultElements,
        velocityFactor: Double = 0.95,
        txPowerWatts: Double = 100.0,
        boomLengthOverrideFactor: Double = 1.0
    ): AntennaCalculationResult {
        val safeFreqMhz = frequencyMhz.coerceIn(0.000001, 6000.0)
        val lambda = SPEED_OF_LIGHT / safeFreqMhz
        val wireRadiusM = (wireDiameterMm.coerceIn(0.05, 500.0) / 1000.0) / 2.0
        // End-effect shortening factor K depending on lambda / diameter ratio
        val slenderness = max(10.0, lambda / (2.0 * wireRadiusM))
        val kFactor = (1.0 - (0.048 / ln(slenderness))) * velocityFactor.coerceIn(0.5, 1.0)

        val dimensions = mutableListOf<AntennaDimensionItem>()
        var rCenter = 50.0
        var xCenter = 0.0
        var gainDbi = 2.15
        var fbDb = 0.0
        var efficiencyPct = 97.0
        var qFactor = 12.0
        var capPf: Double? = null
        var capVoltage: Double? = null

        when (antennaType) {
            AntennaType.DIPOLE -> {
                val totalLength = 0.5 * lambda * kFactor * boomLengthOverrideFactor
                val armLength = totalLength / 2.0
                dimensions.add(AntennaDimensionItem("L_TOTAL", "Общая длина полотна диполя (L)", totalLength, 0.0, "Резонансная полуволновая длина с учётом КУ и толщины провода"))
                dimensions.add(AntennaDimensionItem("L_ARM", "Длина каждого плеча (L/2)", armLength, 0.0, "Два симметричных плеча от центрального изолятора"))
                dimensions.add(AntennaDimensionItem("GAP", "Зазор в точке питания", (lambda * 0.002).coerceIn(0.005, 0.05), 0.0, "Расстояние между клеммами балуна 1:1"))
                dimensions.add(AntennaDimensionItem("H_OPT", "Оптимальная высота подвеса (0.5λ)", 0.5 * lambda, 0.0, "Обеспечивает угол излучения ~28° и входное R ≈ 72 Ом"))

                val heightRatio = mastHeightMeters / lambda
                // Mutual impedance with ground reflection
                val groundEffectR = 73.1 * (1.0 - 0.35 * sin(4.0 * PI * heightRatio) / max(0.3, 2.0 * PI * heightRatio))
                rCenter = groundEffectR.coerceIn(35.0, 95.0)
                xCenter = (boomLengthOverrideFactor - 1.0) * 420.0
                gainDbi = if (heightRatio >= 0.45) 5.8 else 2.15 + 3.0 * heightRatio
                fbDb = 0.0
                qFactor = max(6.0, ln(slenderness) * 1.4)
            }

            AntennaType.GROUND_PLANE -> {
                val radiatorLen = 0.25 * lambda * kFactor * boomLengthOverrideFactor
                val radialLen = 0.25 * lambda * 1.02 * velocityFactor
                dimensions.add(AntennaDimensionItem("L_VERT", "Длина вертикального штыря (λ/4)", radiatorLen, 0.0, "Центральный вертикальный излучатель"))
                dimensions.add(AntennaDimensionItem("L_RAD", "Длина каждого противовеса (4 шт.)", radialLen, 0.0, "Располагаются под углом 135° к штырю (наклон 45° вниз)"))
                dimensions.add(AntennaDimensionItem("D_BASE", "Размах основания противовесов", radialLen * 1.414, 0.0, "Проекция наклонных радиальных элементов"))

                rCenter = 50.5
                xCenter = (boomLengthOverrideFactor - 1.0) * 280.0
                gainDbi = 1.85
                fbDb = 0.0
                qFactor = max(7.0, ln(slenderness) * 1.2)
            }

            AntennaType.J_POLE -> {
                val totalRadiator = 0.75 * lambda * kFactor * boomLengthOverrideFactor
                val matchingStub = 0.25 * lambda * kFactor
                val radiatorSection = totalRadiator - matchingStub
                val stubSpacing = 0.025 * lambda
                val tapPoint = 0.032 * lambda
                dimensions.add(AntennaDimensionItem("L_LONG", "Длинное колено (3/4 λ)", totalRadiator, 0.0, "Цельная трубка излучателя + согласующей секции"))
                dimensions.add(AntennaDimensionItem("L_STUB", "Короткое колено шлейфа (1/4 λ)", matchingStub, stubSpacing, "Параллельный четвертьволновый согласующий шлейф"))
                dimensions.add(AntennaDimensionItem("L_RAD", "Активная излучающая часть (1/2 λ)", radiatorSection, 0.0, "Верхняя полуволновая секция"))
                dimensions.add(AntennaDimensionItem("S_GAP", "Расстояние между трубками шлейфа", stubSpacing, 0.0, "Межцентровое расстояние"))
                dimensions.add(AntennaDimensionItem("L_TAP", "Точка подключения кабеля 50 Ом от КЗ", tapPoint, 0.0, "Подбирается по минимуму КСВ скользящим хомутом"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 320.0
                gainDbi = 3.2
                fbDb = 0.0
                qFactor = 14.0
            }

            AntennaType.YAGI -> {
                val nEl = elementCount.coerceIn(3, 15)
                val reflectorLen = 0.495 * lambda * velocityFactor
                val drivenLen = 0.473 * lambda * kFactor * boomLengthOverrideFactor
                dimensions.add(AntennaDimensionItem("REF", "Рефлектор (R)", reflectorLen, 0.0, "Задний отражающий элемент"))
                var boomPos = 0.18 * lambda
                dimensions.add(AntennaDimensionItem("DRV", "Активный вибратор (DE)", drivenLen, boomPos, "Точка запитки 50 Ом через гамма- или симметрирующее устройство"))

                for (i in 1..(nEl - 2)) {
                    val dirFactor = (0.452 - 0.008 * ln(i.toDouble() + 1.0)).coerceAtLeast(0.405)
                    val spacing = if (i == 1) 0.14 * lambda else (0.22 + 0.02 * min(i, 5)) * lambda
                    boomPos += spacing
                    val dirLen = dirFactor * lambda * velocityFactor
                    dimensions.add(
                        AntennaDimensionItem(
                            "D$i",
                            "Директор D$i",
                            dirLen,
                            boomPos,
                            "Направляющий элемент #$i на траверсе"
                        )
                    )
                }
                dimensions.add(AntennaDimensionItem("BOOM", "Общая длина траверсы (бума)", boomPos, 0.0, "Полная база антенны от рефлектора до D${nEl - 2}"))

                rCenter = 48.5
                xCenter = (boomLengthOverrideFactor - 1.0) * 490.0
                gainDbi = 5.2 + 2.8 * log10(nEl.toDouble()) * 2.2
                fbDb = (14.0 + (nEl - 2) * 2.1).coerceAtMost(28.5)
                qFactor = 18.0 + nEl * 1.5
            }

            AntennaType.CUBICAL_QUAD -> {
                val nEl = elementCount.coerceIn(2, 6)
                val refPerimeter = 1.032 * lambda * velocityFactor
                val drvPerimeter = 1.005 * lambda * velocityFactor * boomLengthOverrideFactor
                val sideDrv = drvPerimeter / 4.0
                val sideRef = refPerimeter / 4.0
                val spacing = 0.18 * lambda
                dimensions.add(AntennaDimensionItem("P_REF", "Периметр рамки рефлектора", refPerimeter, 0.0, "Сторона квадрата рефлектора: ${formatMetricLength(sideRef)}"))
                dimensions.add(AntennaDimensionItem("P_DRV", "Периметр активной рамки (вибратора)", drvPerimeter, spacing, "Сторона активного квадрата: ${formatMetricLength(sideDrv)}"))
                var pos = spacing
                for (i in 1..(nEl - 2)) {
                    pos += 0.18 * lambda
                    val dirPerim = (0.975 - 0.006 * i) * lambda * velocityFactor
                    dimensions.add(AntennaDimensionItem("P_D$i", "Периметр директора D$i", dirPerim, pos, "Сторона квадрата: ${formatMetricLength(dirPerim / 4.0)}"))
                }
                dimensions.add(AntennaDimensionItem("BOOM", "Длина траверсы (бума)", pos, 0.0, "Расстояние между распорками («ежами»)"))

                rCenter = 52.0 // With quarter-wave 75-ohm matching stub
                xCenter = (boomLengthOverrideFactor - 1.0) * 390.0
                gainDbi = 7.2 + (nEl - 2) * 1.9
                fbDb = 20.0 + (nEl - 2) * 2.0
                qFactor = 13.0
            }

            AntennaType.DISCONE -> {
                // Wideband Discone designed from safeFreqMhz (lowest cutoff) up to 10x
                val coneSlant = 0.275 * lambda * velocityFactor * boomLengthOverrideFactor
                val coneBaseDiam = coneSlant // 60 degree cone apex angle
                val discDiam = 0.7 * coneBaseDiam
                val gapInsulator = (0.008 * lambda).coerceIn(0.002, 0.05)
                dimensions.add(AntennaDimensionItem("D_DISC", "Диаметр верхнего диска (D)", discDiam, 0.0, "Сплошной диск или 8–12 радиальных спиц длиной ${formatMetricLength(discDiam / 2.0)}"))
                dimensions.add(AntennaDimensionItem("L_CONE", "Длина образующей юбки конуса (L)", coneSlant, 0.0, "8–12 наклонных стержней под углом 60° при вершине"))
                dimensions.add(AntennaDimensionItem("D_CONE", "Нижний диаметр основания конуса (C)", coneBaseDiam, 0.0, "Раскрытие юбки конуса в основании"))
                dimensions.add(AntennaDimensionItem("S_GAP", "Изоляционный зазор диск–конус (S)", gapInsulator, 0.0, "Центральная жила — к диску, оплётка — к вершине конуса"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 60.0
                gainDbi = 2.0
                fbDb = 0.0
                qFactor = 1.6 // Ultra-wideband!
            }

            AntennaType.LPDA -> {
                val fMin = safeFreqMhz
                val fMax = max(safeFreqMhz * 1.8, upperFreqMhz.coerceAtMost(6000.0))
                val tau = 0.86
                val sigma = 0.15
                val lambdaMax = SPEED_OF_LIGHT / fMin
                var lCurrent = 0.5 * lambdaMax * velocityFactor * boomLengthOverrideFactor
                var pos = 0.0
                val nEl = elementCount.coerceIn(5, 16)
                for (i in 1..nEl) {
                    dimensions.add(
                        AntennaDimensionItem(
                            "EL_$i",
                            "Вибратор #$i (${if (i == 1) "НЧ задний" else if (i == nEl) "ВЧ передний" else "средний"})",
                            lCurrent,
                            pos,
                            "Плечо: ${formatMetricLength(lCurrent / 2.0)}"
                        )
                    )
                    val dNext = 2.0 * sigma * lCurrent
                    pos += dNext
                    lCurrent *= tau
                }
                dimensions.add(AntennaDimensionItem("BOOM", "Общая длина двухпроводного бума", pos, 0.0, "Питание с ВЧ-конца с перекрёстной фазировкой элементов (180°)"))

                rCenter = 51.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 90.0
                gainDbi = 7.4
                fbDb = 18.5
                qFactor = 2.2
            }

            AntennaType.T2FD_BEVERAGE -> {
                val totalLen = 0.33 * lambda * velocityFactor * boomLengthOverrideFactor
                val wireSpacing = (0.01 * lambda).coerceIn(0.15, 1.2)
                dimensions.add(AntennaDimensionItem("L_T2FD", "Полный размах полотна T2FD", totalLen, 0.0, "Работает в полосе от ${formatFrequencyDisplay(safeFreqMhz)} до ${formatFrequencyDisplay(safeFreqMhz * 6.0)}"))
                dimensions.add(AntennaDimensionItem("W_SPACE", "Расстояние между верхним и нижним проводами", wireSpacing, 0.0, "Диэлектрические распорки через каждые 2.5–3 м"))
                dimensions.add(AntennaDimensionItem("R_TERM", "Нагрузочный безындукционный резистор", 0.390, 0.0, "Номинал 390–450 Ом (мощность ≥ 35% от P_пер), балун 9:1 в точке питания"))

                rCenter = 49.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 80.0
                gainDbi = 1.5
                fbDb = 0.0
                efficiencyPct = 72.0
                qFactor = 1.8
            }

            AntennaType.MAG_LOOP -> {
                // Loop diameter ~ 0.08 lambda for HF/VHF, or multi-turn coil for ELF/VLF/LF
                val loopDiam = if (safeFreqMhz < 0.5) {
                    (1.2 * boomLengthOverrideFactor).coerceIn(0.3, 5.0)
                } else {
                    (0.08 * lambda * boomLengthOverrideFactor).coerceIn(0.15, 4.0)
                }
                val loopCircumference = PI * loopDiam
                val couplingDiam = loopDiam * 0.20
                val turns = if (safeFreqMhz < 0.3) max(10, (1.0 / sqrt(safeFreqMhz)).toInt().coerceAtMost(500)) else 1

                // Inductance of circular loop in Henries
                val radiusM = loopDiam / 2.0
                val wireRad = max(0.001, wireRadiusM)
                val inductanceH = turns.toDouble().pow(2) * 4e-7 * PI * radiusM * (ln(8.0 * radiusM / wireRad) - 2.0)
                val omega = 2.0 * PI * safeFreqMhz * 1e6
                val xLoop = omega * inductanceH
                val capFarads = 1.0 / (omega * omega * inductanceH)
                capPf = capFarads * 1e12

                // Radiation resistance R_rad = 31171 * N^2 * (Area / lambda^2)^2
                val area = PI * radiusM * radiusM
                val rRad = 31171.0 * turns * turns * (area / (lambda * lambda)).pow(2)
                // Skin effect loss resistance of copper
                val rLoss = turns * loopCircumference / (2.0 * PI * wireRad) * sqrt(PI * safeFreqMhz * 1e6 * 4e-7 * PI / 5.8e7)
                efficiencyPct = ((rRad / (rRad + rLoss + 1e-12)) * 100.0).coerceIn(0.5, 98.0)
                qFactor = (xLoop / (2.0 * (rRad + rLoss + 1e-6))).coerceIn(25.0, 1200.0)
                capVoltage = sqrt(txPowerWatts * xLoop * qFactor).coerceAtMost(45000.0)

                dimensions.add(AntennaDimensionItem("D_LOOP", "Диаметр основной рамки (D)", loopDiam, 0.0, "Число витков: $turns шт., периметр витка: ${formatMetricLength(loopCircumference)}"))
                dimensions.add(AntennaDimensionItem("D_COUP", "Диаметр петли связи (d = D/5)", couplingDiam, 0.0, "Располагается внутри основной рамки напротив конденсатора КПЕ"))
                dimensions.add(AntennaDimensionItem("C_TUNE", "Ёмкость вакуумного/воздушного КПЕ", (capPf ?: 50.0) / 1000.0, 0.0, String.format("Требуемая ёмкость: %.1f пФ, напряжение на КПЕ (при %.0f Вт): %.0f В!", capPf ?: 0.0, txPowerWatts, capVoltage ?: 0.0)))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 1400.0
                gainDbi = if (efficiencyPct > 50.0) 1.5 else -6.0 + efficiencyPct * 0.12
                fbDb = 0.0
            }

            AntennaType.HELICAL_PATCH -> {
                val nTurns = elementCount.coerceIn(3, 20)
                val circumference = 1.02 * lambda * velocityFactor * boomLengthOverrideFactor
                val helixDiam = circumference / PI
                val turnSpacing = 0.24 * lambda
                val axialLength = nTurns * turnSpacing
                val reflectorDiam = 0.85 * lambda

                dimensions.add(AntennaDimensionItem("D_HELIX", "Диаметр намотки спирали (D)", helixDiam, 0.0, "Длина одного витка C ≈ 1λ = ${formatMetricLength(circumference)}"))
                dimensions.add(AntennaDimensionItem("S_TURN", "Шаг витков спирали (S = 0.24λ)", turnSpacing, 0.0, "Угол подъёма витка α ≈ 13.5°"))
                dimensions.add(AntennaDimensionItem("L_AXIS", "Осевая длина спирали ($nTurns витков)", axialLength, 0.0, "Обеспечивает круговую поляризацию (RHCP/LHCP)"))
                dimensions.add(AntennaDimensionItem("D_REF", "Диаметр экрана-рефлектора", reflectorDiam, 0.0, "Плюс четвертьволновый треугольный согласующий трансформатор 140 -> 50 Ом"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 180.0
                gainDbi = 10.8 + 10.0 * log10(nTurns * 0.24)
                fbDb = 16.0
                qFactor = 4.5
            }
        }

        // Generate 41-point frequency sweep around center frequency (or across wide span for all-wave)
        val spanRatio = if (antennaType.isAllWave) 0.45 else (2.2 / qFactor.coerceIn(2.0, 200.0)).coerceIn(0.01, 0.25)
        val fStart = max(0.000001, safeFreqMhz * (1.0 - spanRatio))
        val fEnd = min(6000.0, safeFreqMhz * (1.0 + spanRatio))
        val sweepPoints = (0..40).map { idx ->
            val frac = idx / 40.0
            val f = fStart + (fEnd - fStart) * frac
            val delta = (f / safeFreqMhz) - (safeFreqMhz / f)
            val rFreq = (rCenter * (1.0 + 0.15 * delta)).coerceAtLeast(5.0)
            val xFreq = xCenter + qFactor * rCenter * delta
            computeImpedancePoint(f, rFreq, xFreq, 50.0)
        }

        val centerPoint = computeImpedancePoint(safeFreqMhz, rCenter, xCenter, 50.0)
        val bwSwr2Khz = ((safeFreqMhz * 1000.0) / qFactor) * 1.4

        val polarPattern = (0..359 step 5).map { angleDeg ->
            val rad = angleDeg * PI / 180.0
            val azGain = when (antennaType) {
                AntennaType.DIPOLE, AntennaType.T2FD_BEVERAGE, AntennaType.MAG_LOOP -> {
                    val figure8 = abs(cos(rad)).coerceAtLeast(0.08)
                    20.0 * log10(figure8)
                }
                AntennaType.GROUND_PLANE, AntennaType.J_POLE, AntennaType.DISCONE -> {
                    -0.3 * sin(rad * 4.0) // Omnidirectional in azimuth
                }
                AntennaType.YAGI, AntennaType.CUBICAL_QUAD, AntennaType.LPDA, AntennaType.HELICAL_PATCH -> {
                    val forward = ((1.0 + cos(rad)) / 2.0).pow(elementCount.coerceIn(2, 10).toDouble() * 0.85)
                    val backLobe = 10.0.pow(-fbDb / 20.0) * ((1.0 - cos(rad)) / 2.0).pow(2.0)
                    val sideRipple = 0.08 * abs(sin(rad * 2.0))
                    val amp = (forward + backLobe + sideRipple).coerceIn(0.02, 1.0)
                    20.0 * log10(amp)
                }
            }
            val elevGain = (-12.0 * sin(rad - PI / 6.0).pow(2.0)).coerceIn(-35.0, 0.0)
            PolarPatternPoint(
                angleDeg = angleDeg,
                azimuthGainNormalizedDb = azGain.coerceIn(-38.0, 0.0),
                elevationGainNormalizedDb = elevGain
            )
        }

        val heightInLambda = (mastHeightMeters / lambda).coerceAtLeast(0.05)
        val takeOffAngle = if (heightInLambda >= 0.25) {
            (Math.toDegrees(kotlin.math.asin((1.0 / (4.0 * heightInLambda)).coerceIn(0.08, 0.95)))).coerceIn(6.0, 75.0)
        } else {
            45.0
        }

        return AntennaCalculationResult(
            antennaType = antennaType,
            frequencyMhz = safeFreqMhz,
            upperFreqMhz = max(safeFreqMhz, upperFreqMhz),
            wavelengthMeters = lambda,
            wireDiameterMm = wireDiameterMm,
            mastHeightMeters = mastHeightMeters,
            elementCount = elementCount,
            velocityFactor = velocityFactor,
            txPowerWatts = txPowerWatts,
            dimensions = dimensions,
            centerImpedanceR = centerPoint.rOhms,
            centerImpedanceX = centerPoint.xOhms,
            centerSwr50 = centerPoint.swr50,
            bandwidthSwr2Khz = bwSwr2Khz,
            gainDbi = gainDbi,
            frontToBackDb = fbDb,
            radiationEfficiencyPct = efficiencyPct,
            takeOffAngleDeg = takeOffAngle,
            tuningCapacitorPf = capPf,
            loopCapacitorVoltageV = capVoltage,
            sweepPoints = sweepPoints,
            polarPattern = polarPattern,
            physicsDescriptionRu = buildPhysicsDescription(antennaType, safeFreqMhz, lambda),
            assemblyStepsRu = buildAssemblySteps(antennaType, dimensions, safeFreqMhz),
            matchingAdviceRu = buildMatchingAdvice(antennaType, centerPoint.rOhms, centerPoint.xOhms, safeFreqMhz)
        )
    }

    fun computeImpedancePoint(freqMhz: Double, r: Double, x: Double, z0: Double = 50.0): ImpedancePoint {
        val denom = (r + z0).pow(2) + x.pow(2)
        val gammaReal = ((r * r - z0 * z0 + x * x) / denom).coerceIn(-0.995, 0.995)
        val gammaImag = ((2.0 * z0 * x) / denom).coerceIn(-0.995, 0.995)
        val rho = sqrt(gammaReal * gammaReal + gammaImag * gammaImag).coerceIn(0.0, 0.985)
        val swr = ((1.0 + rho) / (1.0 - rho)).coerceIn(1.0, 99.9)
        val returnLoss = if (rho > 1e-5) -20.0 * log10(rho) else 50.0
        return ImpedancePoint(
            freqMhz = freqMhz,
            rOhms = r,
            xOhms = x,
            swr50 = swr,
            returnLossDb = returnLoss,
            gammaReal = gammaReal,
            gammaImag = gammaImag
        )
    }

    private fun buildPhysicsDescription(type: AntennaType, freqMhz: Double, lambda: Double): String {
        return when (type) {
            AntennaType.DIPOLE ->
                "Полуволновый симметричный вибратор на частоту ${formatFrequencyDisplay(freqMhz)} (λ = ${formatMetricLength(lambda)}). Распределение тока синусоидальное с пучностью тока в центре и пучностями напряжения на концах лучей. При подвесе в виде Inverted-V с углом при вершине 90°–110° входное сопротивление снижается с 73 Ом до 50 Ом, обеспечивая прямое согласование с коаксиальным кабелем."
            AntennaType.GROUND_PLANE ->
                "Четвертьволновый вертикальный излучатель с системой из 4 приподнятых противовесов. Наклон противовесов под углом 45° вниз (135° к штырю) повышает входной импеданс с 36 Ом до 50 Ом и формирует прижатый к горизонту лепесток диаграммы направленности для дальних связей."
            AntennaType.J_POLE ->
                "Вертикальная полуволновая антенна, питаемая с конца через четвертьволновый короткозамкнутый двухпроводный трансформатор. Нижняя точка шлейфа имеет нулевой ВЧ-потенциал и может быть напрямую заземлена на металлическую мачту для грозозащиты."
            AntennaType.YAGI ->
                "Многоэлементная директорная антенна бегущей волны («Волновой канал»). Пассивный рефлектор удлиняется на ~4.5% (индуктивный характер тока), а директоры укорачиваются на 4–8% (емкостной характер), формируя узкий главный лепесток и подавление заднего излучения."
            AntennaType.CUBICAL_QUAD ->
                "Полноразмерная рамочная антенна «Двойной квадрат» (Cubical Quad). Замкнутая рамка периметром ~1λ образует два синфазных полуволновых вибратора, обеспечивая выигрыш до +2 дБ по сравнению с Яги при той же длине траверсы и меньшую чувствительность к атмосферным помехам."
            AntennaType.DISCONE ->
                "Всеволновая сверхширокополосная дискоконусная антенна с перекрытием по частоте до 10:1 и вертикальной поляризацией. Идеальна для широкополосного мониторинга эфира на SDR-приёмники («Малахит», Arinst V2D) и многодиапазонной работы на передачу."
            AntennaType.LPDA ->
                "Логопериодическая дипольная антенна (LPDA) с коэффициентом подобия τ = 0.86. Активная область автоматически перемещается вдоль собирательной линии при изменении частоты, сохраняя стабильный импеданс 50 Ом и высокую направленность в широкой полосе частот."
            AntennaType.T2FD_BEVERAGE ->
                "Всеволновая апериодическая антенна бегущей волны (Tilted Terminated Folded Dipole). Нагрузочный резистор поглощает остаточную волну, устраняя стоячие волны и позволяя работать без перестройки тюнера в полосе с перекрытием 1:6."
            AntennaType.MAG_LOOP ->
                "Высокодобротная магнитная рамка (Magnetic Loop / Ferrite ELF Loop). Реагирует преимущественно на магнитную составляющую ЭМ-поля, подавляя бытовые электрические помехи на 15–25 дБ. Требует высоковольтного конденсатора настройки с минимальными потерями."
            AntennaType.HELICAL_PATCH ->
                "Осевая спиральная антенна (Axial Mode Helical) для диапазонов УВЧ/СВЧ до 6 ГГц. При длине витка C ≈ 1λ и шаге S ≈ 0.24λ излучает вдоль оси с круговой поляризацией, устраняя замирания от вращения плоскости поляризации (эффект Фарадея)."
        }
    }

    private fun buildAssemblySteps(type: AntennaType, dims: List<AntennaDimensionItem>, freqMhz: Double): List<String> {
        val firstDim = dims.firstOrNull()?.formattedLength() ?: ""
        return listOf(
            "1. Подготовка материала: выберите медный канатик, биметалл или алюминиевую трубку расчётного диаметра. Отрежьте элементы согласно таблице чертежа (основной размер: $firstDim с технологическим запасом +2% на точную настройку).",
            "2. Сборка узла питания: установите герметичную коробку с разъёмом SO-239 / N-типа (или SMA для СВЧ > 1 ГГц). Для симметричных антенн (${type.titleRu}) обязательно установите запорный дроссель (токовый балун 1:1 на феррите FT240-43/61).",
            "3. Монтаж элементов и изоляторов: закрепите элементы на диэлектрических изоляторах из стеклотекстолита, капролона или УФ-стойкого поликарбоната, строго выдерживая линейные размеры и соосность.",
            "4. Предварительная проверка на VNA: подключите векторный анализатор цепей (Arinst V2D / NanoVNA) через откалиброванный кабель (SOL-калибровка на конце фидера) и снимите годограф импеданса на частоте ${formatFrequencyDisplay(freqMhz)}.",
            "5. Точная доводка в резонанс: используйте вкладку «Оптимизация VNA» в приложении для автоматического пересчёта подрезки/удлинения полотна до достижения КСВ ≤ 1.15, после чего загерметизируйте все паяные и болтовые соединения."
        )
    }

    private fun buildMatchingAdvice(type: AntennaType, r: Double, x: Double, freqMhz: Double): String {
        return when (type) {
            AntennaType.DIPOLE -> "Симметрирующий токовый балун 1:1 (8–10 витков кабеля на кольце FT240-43 для КВ или четвертьволновый стакан для УКВ). Входное сопротивление: ${String.format("%.1f", r)} Ом."
            AntennaType.GROUND_PLANE -> "Прямое подключение коаксиального кабеля 50 Ом (жила к вертикалу, оплётка к 4 противовесам под углом 135°) + запорный ВЧ-дроссель у точки питания."
            AntennaType.J_POLE -> "Скользящий хомут вдоль четвертьволнового шлейфа: перемещайте точку подключения на высоте 3–4% λ от закороченной перемычки до достижения R = 50 Ом, X = 0."
            AntennaType.YAGI -> "Гамма-согласование (Gamma Match) с последовательной ёмкостью или разрезной вибратор с Hairpin-шлейфом (β-матч) и балуном 1:1."
            AntennaType.CUBICAL_QUAD -> "Четвертьволновый согласующий трансформатор из коаксиального кабеля 75 Ом (длиной λ/4 × КУ_кабеля) между рамкой (~110 Ом) и фидером 50 Ом."
            AntennaType.DISCONE, AntennaType.LPDA, AntennaType.MAG_LOOP -> "Прямое питание кабелем 50 Ом. Для магнитной рамки: индуктивная петля связи диаметром 1/5 от главного кольца или гамма-согласование."
            AntennaType.T2FD_BEVERAGE -> "Широкополосный трансформатор 9:1 (450 Ом → 50 Ом) на двухъёмкостном бинокле BN-43-7051 и безындукционный резистор 390–450 Ом."
            AntennaType.HELICAL_PATCH -> "Треугольная медная согласующая полоска длиной λ/4 шириной от 12 мм у начала первого витка с плавным переходом к разъёму SMA/N 50 Ом."
        }
    }
}

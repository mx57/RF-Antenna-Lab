package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class AntennaCategory(val titleRu: String) {
    DIPOLE_WIRE("Диполи и Проволочные КВ"),
    VERTICAL_COLLINEAR("Вертикалы и Коллинеары"),
    DIRECTIONAL_BEAM("Направленные (Яги, Квадраты, Моксон)"),
    ALL_WAVE_UWB("Всеволновые и Широкополосные"),
    LOOP_FRACTAL("Рамочные, КНЧ и Фрактальные"),
    SHF_SATELLITE("Спутники, Рупоры и СВЧ (до 6 ГГц)")
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
        titleRu = "Полуволновый диполь (λ/2)",
        subtitleRu = "Классический резонансный симметричный вибратор с питанием в центре",
        category = AntennaCategory.DIPOLE_WIRE,
        minFreqMhz = 0.001,
        maxFreqMhz = 6000.0,
        defaultElements = 1
    ),
    INVERTED_V(
        code = "INVERTED_V",
        titleRu = "Антенна Inverted-V («Перевёрнутая V»)",
        subtitleRu = "Двухлучевой диполь на одной мачте с углом раскрыва 90°–110° и входным R ≈ 50 Ом",
        category = AntennaCategory.DIPOLE_WIRE,
        minFreqMhz = 0.01,
        maxFreqMhz = 1000.0,
        defaultElements = 1
    ),
    EFHW(
        code = "EFHW",
        titleRu = "Полуволновый End-Fed (EFHW 49:1 / Фукс)",
        subtitleRu = "Многодиапазонный луч длиной λ/2, питаемый с конца через трансформатор 49:1 (2450 → 50 Ом)",
        category = AntennaCategory.DIPOLE_WIRE,
        minFreqMhz = 1.0,
        maxFreqMhz = 500.0,
        defaultElements = 1,
        isAllWave = true
    ),
    OCFD_WINDOM(
        code = "OCFD_WINDOM",
        titleRu = "Несимметричный диполь (OCFD / Windom)",
        subtitleRu = "Многодиапазонный диполь со смещённой точкой питания (33% / 67%) и балуном 4:1 (200 Ом)",
        category = AntennaCategory.DIPOLE_WIRE,
        minFreqMhz = 1.8,
        maxFreqMhz = 500.0,
        defaultElements = 1,
        isAllWave = true
    ),
    G5RV_ZS6BKW(
        code = "G5RV",
        titleRu = "Многодиапазонная G5RV / ZS6BKW",
        subtitleRu = "Симметричный вибратор с согласующей двухпроводной линией 450 Ом («лесенкой»)",
        category = AntennaCategory.DIPOLE_WIRE,
        minFreqMhz = 1.8,
        maxFreqMhz = 60.0,
        defaultElements = 1,
        isAllWave = true
    ),
    GROUND_PLANE(
        code = "GP",
        titleRu = "Четвертьволновый штырь (Ground Plane λ/4)",
        subtitleRu = "Всенаправленный вертикал с 4 наклонными противовесами 135° (50 Ом)",
        category = AntennaCategory.VERTICAL_COLLINEAR,
        minFreqMhz = 0.1,
        maxFreqMhz = 6000.0,
        defaultElements = 5
    ),
    VERTICAL_5_8(
        code = "VERT_5_8",
        titleRu = "Удлинённый вертикал 5/8 λ с катушкой",
        subtitleRu = "Вертикальный штырь 0.625λ с максимальным прижатием лепестка к горизонту (+3 дБ к λ/4)",
        category = AntennaCategory.VERTICAL_COLLINEAR,
        minFreqMhz = 10.0,
        maxFreqMhz = 3000.0,
        defaultElements = 5
    ),
    J_POLE(
        code = "J_POLE",
        titleRu = "J-Антенна / Коллинеар Slim Jim",
        subtitleRu = "Полуволновый вертикал с четвертьволновым короткозамкнутым согласующим шлейфом",
        category = AntennaCategory.VERTICAL_COLLINEAR,
        minFreqMhz = 14.0,
        maxFreqMhz = 3000.0,
        defaultElements = 2
    ),
    COLLINEAR_COAX(
        code = "COLLINEAR",
        titleRu = "Коаксиальная коллинеарная (Coaxial Collinear)",
        subtitleRu = "Многосекционная фазированная вертикальная решётка из отрезков λ/2 с высоким усилением в горизонте",
        category = AntennaCategory.VERTICAL_COLLINEAR,
        minFreqMhz = 100.0,
        maxFreqMhz = 6000.0,
        defaultElements = 6
    ),
    YAGI(
        code = "YAGI",
        titleRu = "Волновой канал (Уда-Яги 3..15 эл.)",
        subtitleRu = "Высокоэффективная направленная антенна с рефлектором, вибратором и директорами",
        category = AntennaCategory.DIRECTIONAL_BEAM,
        minFreqMhz = 1.8,
        maxFreqMhz = 6000.0,
        defaultElements = 5
    ),
    MOXON(
        code = "MOXON",
        titleRu = "Прямоугольник Моксона (Moxon Rectangle)",
        subtitleRu = "Компактная 2-элементная направленная антенна с загнутыми концами, 50 Ом и F/B до 28 дБ",
        category = AntennaCategory.DIRECTIONAL_BEAM,
        minFreqMhz = 3.5,
        maxFreqMhz = 6000.0,
        defaultElements = 2
    ),
    CUBICAL_QUAD(
        code = "QUAD",
        titleRu = "Двойной квадрат (Cubical Quad)",
        subtitleRu = "Полноразмерные замкнутые рамочные элементы периметром ~1λ с низким углом излучения",
        category = AntennaCategory.DIRECTIONAL_BEAM,
        minFreqMhz = 3.5,
        maxFreqMhz = 3000.0,
        defaultElements = 2
    ),
    HB9CV(
        code = "HB9CV",
        titleRu = "Активная направленная HB9CV (2 активы)",
        subtitleRu = "Швейцарская 2-элементная антенна с активным питанием обоих вибраторов через фазосдвигающую линию",
        category = AntennaCategory.DIRECTIONAL_BEAM,
        minFreqMhz = 14.0,
        maxFreqMhz = 3000.0,
        defaultElements = 2
    ),
    HEXBEAM(
        code = "HEXBEAM",
        titleRu = "Гексагональный бим (HexBeam)",
        subtitleRu = "Направленная КВ/УКВ антенна на 6 стеклопластиковых шестах в форме шестиугольника",
        category = AntennaCategory.DIRECTIONAL_BEAM,
        minFreqMhz = 7.0,
        maxFreqMhz = 500.0,
        defaultElements = 2
    ),
    SPIDERBEAM(
        code = "SPIDERBEAM",
        titleRu = "Спайдербим (Spiderbeam крестовой)",
        subtitleRu = "Лёгкая экспедиционная направленная Яги с V-образно изогнутыми проволочными элементами",
        category = AntennaCategory.DIRECTIONAL_BEAM,
        minFreqMhz = 7.0,
        maxFreqMhz = 500.0,
        defaultElements = 3
    ),
    DISCONE(
        code = "DISCONE",
        titleRu = "Дискоконусная всеволновая (Discone)",
        subtitleRu = "Сверхширокополосная всеволновая антенна с перекрытием до 10:1 по частоте",
        category = AntennaCategory.ALL_WAVE_UWB,
        minFreqMhz = 20.0,
        maxFreqMhz = 6000.0,
        defaultElements = 9,
        isAllWave = true
    ),
    LPDA(
        code = "LPDA",
        titleRu = "Логопериодическая всеволновая (LPDA)",
        subtitleRu = "Направленная всеволновая антенна (τ=0.86, σ=0.15) с постоянным КСВ в широкой полосе",
        category = AntennaCategory.ALL_WAVE_UWB,
        minFreqMhz = 1.0,
        maxFreqMhz = 6000.0,
        defaultElements = 8,
        isAllWave = true
    ),
    T2FD_BEVERAGE(
        code = "T2FD",
        titleRu = "Всеволновый T2FD / Антенна Бевериджа",
        subtitleRu = "Широкополосная апериодическая антенна бегущей волны с нагрузочным резистором 390–450 Ом",
        category = AntennaCategory.ALL_WAVE_UWB,
        minFreqMhz = 0.01,
        maxFreqMhz = 60.0,
        defaultElements = 2,
        isAllWave = true
    ),
    BICONE_BOWTIE(
        code = "BICONE",
        titleRu = "Биконическая / Бабочка (Biconical UWB)",
        subtitleRu = "Всеволновая симметричная коническая / V-образная антенна для панорамного SDR-анализа",
        category = AntennaCategory.ALL_WAVE_UWB,
        minFreqMhz = 25.0,
        maxFreqMhz = 6000.0,
        defaultElements = 8,
        isAllWave = true
    ),
    DELTA_LOOP(
        code = "DELTA_LOOP",
        titleRu = "Треугольная рамка (Delta Loop 1λ)",
        subtitleRu = "Полноразмерный замкнутый треугольник периметром 1λ с малым уровнем бытовых помех",
        category = AntennaCategory.LOOP_FRACTAL,
        minFreqMhz = 1.8,
        maxFreqMhz = 1000.0,
        defaultElements = 1
    ),
    MAG_LOOP(
        code = "MAG_LOOP",
        titleRu = "Магнитная рамка (Magnetic Loop + КНЧ)",
        subtitleRu = "Узкополосная высокодобротная рамка малого размера (1 Гц – 60 МГц) с высоковольтным КПЕ",
        category = AntennaCategory.LOOP_FRACTAL,
        minFreqMhz = 0.000001,
        maxFreqMhz = 145.0,
        defaultElements = 2
    ),
    FRACTAL_KOCH(
        code = "FRACTAL",
        titleRu = "Фрактальная антенна Коха / Серпинского",
        subtitleRu = "Многорезонансная самоподобная геометрия с сокращением габаритов на 38% и многодиапазонной работой",
        category = AntennaCategory.LOOP_FRACTAL,
        minFreqMhz = 3.5,
        maxFreqMhz = 6000.0,
        defaultElements = 4,
        isAllWave = true
    ),
    QFH_SATELLITE(
        code = "QFH",
        titleRu = "Квадрифилярная спутниковая (QFH)",
        subtitleRu = "Четырёхзаходная объёмная спираль с купольной ДН и круговой поляризацией (NOAA / Meteor / L-Band)",
        category = AntennaCategory.SHF_SATELLITE,
        minFreqMhz = 100.0,
        maxFreqMhz = 3000.0,
        defaultElements = 4
    ),
    HELICAL_PATCH(
        code = "HELICAL",
        titleRu = "Спиральная СВЧ (Helical) / Патч",
        subtitleRu = "Антенна круговой поляризации для спутников, QO-100, Wi-Fi и FPV 400–6000 МГц",
        category = AntennaCategory.SHF_SATELLITE,
        minFreqMhz = 400.0,
        maxFreqMhz = 6000.0,
        defaultElements = 6
    ),
    HORN_SHF(
        code = "HORN_SHF",
        titleRu = "Рупорная / Вивальди СВЧ (Horn / Vivaldi)",
        subtitleRu = "Эталонная направленная апертурная СВЧ-антенна с широкой полосой для диапазонов 1–6 ГГц",
        category = AntennaCategory.SHF_SATELLITE,
        minFreqMhz = 700.0,
        maxFreqMhz = 6000.0,
        defaultElements = 4,
        isAllWave = true
    ),
    PARABOLIC_DISH(
        code = "DISH_SHF",
        titleRu = "Параболическая зеркальная СВЧ (Dish)",
        subtitleRu = "Узконаправленное параболическое зеркало с облучателем (F/D = 0.45) для дальнего СВЧ и EME",
        category = AntennaCategory.SHF_SATELLITE,
        minFreqMhz = 900.0,
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
    FrequencyBandPreset("ELF_7HZ", "КНЧ 7.83 Гц (Шуман)", 0.00000783, "Сверхнизкие частоты (1 Гц – 30 Гц), резонанс Шумана"),
    FrequencyBandPreset("VLF_24KHZ", "СНЧ 24 кГц (СДВ)", 0.024, "Сверхдлинные волны (3–30 кГц)"),
    FrequencyBandPreset("LF_136KHZ", "ДВ 2200м (136.5 кГц)", 0.1365, "Любительский ДВ диапазон"),
    FrequencyBandPreset("MF_475KHZ", "СВ 630м (475 кГц)", 0.475, "Средневолновый любительский диапазон"),
    FrequencyBandPreset("HF_160M", "КВ 160м (1.85 МГц)", 1.85, "Ночной DX-диапазон"),
    FrequencyBandPreset("HF_80M", "КВ 80м (3.65 МГц)", 3.65, "Региональная КВ связь"),
    FrequencyBandPreset("HF_40M", "КВ 40м (7.10 МГц)", 7.10, "Круглосуточный КВ диапазон"),
    FrequencyBandPreset("HF_20M", "КВ 20м (14.175 МГц)", 14.175, "Главный дальний DX-диапазон"),
    FrequencyBandPreset("HF_15M", "КВ 15м (21.20 МГц)", 21.20, "Межконтинентальные трассы"),
    FrequencyBandPreset("HF_11M_CB", "Си-Би 27 МГц (27.20 МГц)", 27.20, "Гражданский диапазон 11 метров"),
    FrequencyBandPreset("HF_10M", "КВ 10м (28.50 МГц)", 28.50, "Переходный КВ/УКВ диапазон"),
    FrequencyBandPreset("VHF_6M", "УКВ 6м (50.15 МГц)", 50.15, "Спорадик-Е диапазон"),
    FrequencyBandPreset("VHF_AIR", "Авиа 124.0 МГц", 124.0, "Авиационный диапазон АМ"),
    FrequencyBandPreset("VHF_NOAA", "Спутники 137.5 МГц", 137.50, "Метеоспутники NOAA / Meteor-M"),
    FrequencyBandPreset("VHF_2M", "УКВ 2м (145.50 МГц)", 145.50, "Основной любительский УКВ диапазон"),
    FrequencyBandPreset("UHF_70CM", "ДМВ 70см (433.50 МГц)", 433.50, "Любительский диапазон 70 см, LPD/PMR"),
    FrequencyBandPreset("UHF_868", "LoRa / ISM 868 МГц", 868.0, "Телеметрия, Meshtastic и IoT"),
    FrequencyBandPreset("SHF_ADSB", "ADS-B 1090 МГц", 1090.0, "Авиационные транспондеры ADS-B"),
    FrequencyBandPreset("SHF_23CM", "СВЧ 23см (1296 МГц)", 1296.0, "СВЧ связь, EME и спутники"),
    FrequencyBandPreset("SHF_2G4", "СВЧ 13см (2425 МГц)", 2425.0, "QO-100 Uplink и Wi-Fi 2.4 ГГц"),
    FrequencyBandPreset("SHF_5G8", "СВЧ 5см (5800 МГц)", 5800.0, "Радиомосты, FPV и СВЧ до 6 ГГц")
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
    val azimuthGainNormalizedDb: Double,
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
    val loadingCoilUh: Double?,
    val coilTurnsHintRu: String?,
    val estimatedLineOfSightKm: Double,
    val safeRfDistanceMeters: Double,
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
        var coilUh: Double? = null
        var coilHint: String? = null

        when (antennaType) {
            AntennaType.DIPOLE -> {
                val totalLength = 0.5 * lambda * kFactor * boomLengthOverrideFactor
                val armLength = totalLength / 2.0
                dimensions.add(AntennaDimensionItem("L_TOTAL", "Общая длина полотна диполя (L)", totalLength, 0.0, "Резонансная длина λ/2 с учётом КУ и толщины"))
                dimensions.add(AntennaDimensionItem("L_ARM", "Длина каждого плеча (L/2)", armLength, 0.0, "Два симметричных плеча от центрального изолятора"))
                dimensions.add(AntennaDimensionItem("GAP", "Зазор в точке питания", (lambda * 0.002).coerceIn(0.005, 0.05), 0.0, "Расстояние между клеммами балуна 1:1"))
                dimensions.add(AntennaDimensionItem("H_OPT", "Оптимальная высота подвеса (0.5λ)", 0.5 * lambda, 0.0, "Обеспечивает угол излучения ~28° и R ≈ 72 Ом"))

                val heightRatio = mastHeightMeters / lambda
                val groundEffectR = 73.1 * (1.0 - 0.35 * sin(4.0 * PI * heightRatio) / max(0.3, 2.0 * PI * heightRatio))
                rCenter = groundEffectR.coerceIn(35.0, 95.0)
                xCenter = (boomLengthOverrideFactor - 1.0) * 420.0
                gainDbi = if (heightRatio >= 0.45) 5.8 else 2.15 + 3.0 * heightRatio
                qFactor = max(6.0, ln(slenderness) * 1.4)
            }

            AntennaType.INVERTED_V -> {
                val totalLength = 0.486 * lambda * kFactor * boomLengthOverrideFactor
                val armLength = totalLength / 2.0
                val mastTop = mastHeightMeters
                val spanGround = armLength * 1.414
                val endHeight = (mastTop - armLength * 0.65).coerceAtLeast(1.8)
                dimensions.add(AntennaDimensionItem("L_TOTAL", "Полная длина лучей Inverted-V", totalLength, 0.0, "Угол при вершине мачты 90°–110°"))
                dimensions.add(AntennaDimensionItem("L_ARM", "Длина каждого наклонного луча (L/2)", armLength, 0.0, "Крепится к вершине мачты через центральный балун 1:1"))
                dimensions.add(AntennaDimensionItem("W_SPAN", "Горизонтальный размах между оттяжками", spanGround, 0.0, "Расстояние между точками крепления концов лучей"))
                dimensions.add(AntennaDimensionItem("H_END", "Высота нижних концов над землёй", endHeight, 0.0, "Не менее 2.0 м для безопасности и сохранения добротности"))

                rCenter = 51.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 380.0
                gainDbi = 3.8
                qFactor = 10.5
            }

            AntennaType.EFHW -> {
                val wireLen = 0.492 * lambda * kFactor * boomLengthOverrideFactor
                val counterpoise = 0.05 * lambda
                val coilPos = 0.055 * lambda
                coilUh = (110.0 / safeFreqMhz).coerceIn(0.05, 500.0)
                coilHint = buildWheelersCoilHint(coilUh, 32.0)
                dimensions.add(AntennaDimensionItem("L_WIRE", "Длина излучающего полотна (λ/2)", wireLen, 0.0, "Работает на основной частоте и гармониках 2f, 3f, 4f"))
                dimensions.add(AntennaDimensionItem("L_CP", "Длина короткого противовеса (0.05λ)", counterpoise, 0.0, "Иلب роль ВЧ-противовеса оплётки кабеля до дросселя"))
                dimensions.add(AntennaDimensionItem("X_COIL", "Отступ компенсирующей катушки от трансформатора", coilPos, 0.0, "Катушка ${String.format("%.2f мкГн", coilUh)} для выравнивания ВЧ гармоник"))
                dimensions.add(AntennaDimensionItem("TR_49", "Трансформатор 49:1 (2450 → 50 Ом)", 0.06, 0.0, "2 первич. + 14 вторич. витков на 2 кольцах FT240-43 + конденсатор 100 пФ 3 кВ"))

                rCenter = 52.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 290.0
                gainDbi = 2.4
                qFactor = 7.5
            }

            AntennaType.OCFD_WINDOM -> {
                val totalLen = 0.488 * lambda * kFactor * boomLengthOverrideFactor
                val shortArm = totalLen * 0.333
                val longArm = totalLen * 0.667
                dimensions.add(AntennaDimensionItem("L_TOTAL", "Общая длина полотна OCFD (Windom)", totalLen, 0.0, "Точка запитки смещена в пропорции 33.3% / 66.7%"))
                dimensions.add(AntennaDimensionItem("L_SHORT", "Короткое плечо (1/3 L)", shortArm, 0.0, "Подключается к одной клемме балуна 4:1"))
                dimensions.add(AntennaDimensionItem("L_LONG", "Длинное плечо (2/3 L)", longArm, 0.0, "Подключается ко второй клемме балуна 4:1"))
                dimensions.add(AntennaDimensionItem("BAL_4_1", "Токовый балун Гуанелла 4:1 (200 → 50 Ом)", 0.08, 0.0, "Обязателен запорный дроссель 1:1 ниже балуна"))

                rCenter = 49.5
                xCenter = (boomLengthOverrideFactor - 1.0) * 310.0
                gainDbi = 3.1
                qFactor = 8.0
            }

            AntennaType.G5RV_ZS6BKW -> {
                val flatTop = 0.945 * lambda * velocityFactor * boomLengthOverrideFactor
                val halfFlat = flatTop / 2.0
                val ladderLine = 0.338 * lambda * 0.91
                dimensions.add(AntennaDimensionItem("L_TOP", "Горизонтальное полотно ZS6BKW / G5RV", flatTop, 0.0, "Два симметричных плеча по ${formatMetricLength(halfFlat)}"))
                dimensions.add(AntennaDimensionItem("L_ARM", "Длина одного плеча", halfFlat, 0.0, "Горизонтальная или наклонная растяжка"))
                dimensions.add(AntennaDimensionItem("L_STUB", "Длина двухпроводной линии 450 Ом", ladderLine, 0.0, "Согласующий трансформирующий шлейф вертикально вниз"))
                dimensions.add(AntennaDimensionItem("CHOKE", "Симметрирующий дроссель 1:1 внизу шлейфа", 0.06, 0.0, "Переход с линии 450 Ом на коаксиал 50 Ом"))

                rCenter = 53.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 350.0
                gainDbi = 4.2
                qFactor = 9.0
            }

            AntennaType.GROUND_PLANE -> {
                val radiatorLen = 0.25 * lambda * kFactor * boomLengthOverrideFactor
                val radialLen = 0.25 * lambda * 1.02 * velocityFactor
                dimensions.add(AntennaDimensionItem("L_VERT", "Длина вертикального штыря (λ/4)", radiatorLen, 0.0, "Центральный вертикальный излучатель"))
                dimensions.add(AntennaDimensionItem("L_RAD", "Длина каждого противовеса (4 шт.)", radialLen, 0.0, "Наклон 45° вниз (135° к штырю) для R = 50 Ом"))
                dimensions.add(AntennaDimensionItem("D_BASE", "Диаметр проекции основания", radialLen * 1.414, 0.0, "Габаритный диаметр радиальных элементов"))

                rCenter = 50.5
                xCenter = (boomLengthOverrideFactor - 1.0) * 280.0
                gainDbi = 1.85
                qFactor = max(7.0, ln(slenderness) * 1.2)
            }

            AntennaType.VERTICAL_5_8 -> {
                val radiatorLen = 0.625 * lambda * kFactor * boomLengthOverrideFactor
                val radialLen = 0.25 * lambda * velocityFactor
                // Base loading coil needed to cancel capacitive reactance of 5/8 wave (~ -j115 Ohms -> need +j115 Ohms)
                val omega = 2.0 * PI * safeFreqMhz * 1e6
                coilUh = (115.0 / omega) * 1e6
                coilHint = buildWheelersCoilHint(coilUh, 25.0)
                dimensions.add(AntennaDimensionItem("L_5_8", "Длина вертикального излучателя (5/8 λ)", radiatorLen, 0.0, "Формирует максимально прижатый лепесток к горизонту"))
                dimensions.add(AntennaDimensionItem("L_COIL", "Удлиняющая катушка в основании", (0.04 * lambda).coerceIn(0.02, 0.35), 0.0, "Индуктивность L = ${String.format("%.2f мкГн", coilUh)} ($coilHint)"))
                dimensions.add(AntennaDimensionItem("L_RAD", "Четвертьволновые противовесы (4–8 шт.)", radialLen, 0.0, "Горизонтальные или под углом 15°"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 360.0
                gainDbi = 4.85
                qFactor = 13.5
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
                dimensions.add(AntennaDimensionItem("L_TAP", "Точка подключения кабеля 50 Ом от КЗ", tapPoint, 0.0, "Подбирается скользящим хомутом по минимуму КСВ"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 320.0
                gainDbi = 3.2
                qFactor = 14.0
            }

            AntennaType.COLLINEAR_COAX -> {
                val nSec = elementCount.coerceIn(2, 12)
                val secLen = 0.5 * lambda * 0.82 * boomLengthOverrideFactor // Velocity factor of coax ~0.82
                val topWhip = 0.25 * lambda * kFactor
                val totalHeight = nSec * secLen + topWhip
                dimensions.add(AntennaDimensionItem("L_SEC", "Длина каждой коаксиальной секции (λ/2 × КУ)", secLen, 0.0, "Всего секций: $nSec шт. со взаимной перефазировкой жилы и оплётки"))
                dimensions.add(AntennaDimensionItem("L_TOP", "Верхний четвертьволновый штырь (λ/4)", topWhip, 0.0, "Завершающий излучатель на вершине"))
                dimensions.add(AntennaDimensionItem("H_ALL", "Полная высота коллинеарной сборки", totalHeight, 0.0, "Размещается внутри радиопрозрачного стеклопластикового тубуса"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 410.0
                gainDbi = 3.0 + 10.0 * log10(nSec.toDouble())
                qFactor = 11.0 + nSec
            }

            AntennaType.YAGI -> {
                val nEl = elementCount.coerceIn(3, 15)
                val reflectorLen = 0.495 * lambda * velocityFactor
                val drivenLen = 0.473 * lambda * kFactor * boomLengthOverrideFactor
                dimensions.add(AntennaDimensionItem("REF", "Рефлектор (R)", reflectorLen, 0.0, "Задний отражающий элемент"))
                var boomPos = 0.18 * lambda
                dimensions.add(AntennaDimensionItem("DRV", "Активный вибратор (DE)", drivenLen, boomPos, "Точка запитки 50 Ом"))

                for (i in 1..(nEl - 2)) {
                    val dirFactor = (0.452 - 0.008 * ln(i.toDouble() + 1.0)).coerceAtLeast(0.405)
                    val spacing = if (i == 1) 0.14 * lambda else (0.22 + 0.02 * min(i, 5)) * lambda
                    boomPos += spacing
                    val dirLen = dirFactor * lambda * velocityFactor
                    dimensions.add(AntennaDimensionItem("D$i", "Директор D$i", dirLen, boomPos, "Направляющий элемент #$i"))
                }
                dimensions.add(AntennaDimensionItem("BOOM", "Общая длина траверсы (бума)", boomPos, 0.0, "Полная база от рефлектора до D${nEl - 2}"))

                rCenter = 48.5
                xCenter = (boomLengthOverrideFactor - 1.0) * 490.0
                gainDbi = 5.2 + 2.8 * log10(nEl.toDouble()) * 2.2
                fbDb = (14.0 + (nEl - 2) * 2.1).coerceAtMost(28.5)
                qFactor = 18.0 + nEl * 1.5
            }

            AntennaType.MOXON -> {
                // Classic Cebik Moxon Rectangle equations (A, B, C, D, E)
                val dimA = 0.363 * lambda * velocityFactor * boomLengthOverrideFactor
                val dimB = 0.052 * lambda * velocityFactor
                val dimC = 0.014 * lambda
                val dimD = 0.068 * lambda * velocityFactor
                val dimE = dimB + dimC + dimD
                dimensions.add(AntennaDimensionItem("A_WIDTH", "Ширина прямоугольника Моксона (A)", dimA, 0.0, "Размах вибратора и рефлектора"))
                dimensions.add(AntennaDimensionItem("B_DRV_TAIL", "Загнутый хвост вибратора (B)", dimB, 0.0, "Загибается назад к рефлектору под 90°"))
                dimensions.add(AntennaDimensionItem("C_GAP", "Изоляционный зазор между хвостами (C)", dimC, 0.0, "Фиксируется диэлектрическими трубками"))
                dimensions.add(AntennaDimensionItem("D_REF_TAIL", "Загнутый хвост рефлектора (D)", dimD, 0.0, "Загибается вперёд к вибратору под 90°"))
                dimensions.add(AntennaDimensionItem("E_DEPTH", "Полная продольная глубина (E = B+C+D)", dimE, 0.0, "Длина бума всего 0.134λ при прямом питании 50 Ом!"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 340.0
                gainDbi = 6.1
                fbDb = 26.5
                qFactor = 11.5
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
                dimensions.add(AntennaDimensionItem("BOOM", "Длина траверсы (бума)", pos, 0.0, "Расстояние между крестовинами («ежами»)"))

                rCenter = 52.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 390.0
                gainDbi = 7.2 + (nEl - 2) * 1.9
                fbDb = 20.0 + (nEl - 2) * 2.0
                qFactor = 13.0
            }

            AntennaType.HB9CV -> {
                val rearEl = 0.500 * lambda * velocityFactor * boomLengthOverrideFactor
                val frontEl = 0.462 * lambda * velocityFactor * boomLengthOverrideFactor
                val boomLen = 0.125 * lambda
                val gammaTapRear = 0.062 * lambda
                val gammaTapFront = 0.058 * lambda
                dimensions.add(AntennaDimensionItem("L_REAR", "Задний активный вибратор (L1 = 0.50λ)", rearEl, 0.0, "Точка подключения фазовой линии: ${formatMetricLength(gammaTapRear)}"))
                dimensions.add(AntennaDimensionItem("L_FRONT", "Передний активный вибратор (L2 = 0.46λ)", frontEl, boomLen, "Точка подключения фазовой линии: ${formatMetricLength(gammaTapFront)}"))
                dimensions.add(AntennaDimensionItem("BOOM", "Межэлементное расстояние (0.125λ)", boomLen, 0.0, "Перекрёстная фазирующая линия сдвигает фазу на 225°"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 310.0
                gainDbi = 6.5
                fbDb = 24.0
                qFactor = 12.0
            }

            AntennaType.HEXBEAM -> {
                val hexRadius = 0.165 * lambda * boomLengthOverrideFactor
                val drvWire = 0.482 * lambda * velocityFactor
                val refWire = 0.508 * lambda * velocityFactor
                val tipGap = 0.018 * lambda
                dimensions.add(AntennaDimensionItem("R_SPREAD", "Радиус каждого из 6 шестов каркаса", hexRadius, 0.0, "Диаметр описанной окружности гексабима: ${formatMetricLength(hexRadius * 2.0)}"))
                dimensions.add(AntennaDimensionItem("L_DRV", "Длина проволочного W-вибратора", drvWire, 0.0, "Два плеча по ${formatMetricLength(drvWire / 2.0)}"))
                dimensions.add(AntennaDimensionItem("L_REF", "Длина проволочного рефлектора", refWire, 0.0, "Растягивается по внешнему контуру шестиугольника"))
                dimensions.add(AntennaDimensionItem("S_TIPS", "Диэлектрический зазор между концами", tipGap, 0.0, "Шнур Кевлар / Дакрон с высокой электропрочностью"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 320.0
                gainDbi = 5.9
                fbDb = 22.0
                qFactor = 11.0
            }

            AntennaType.SPIDERBEAM -> {
                val spreadLen = 0.255 * lambda * boomLengthOverrideFactor
                val refLen = 0.498 * lambda * velocityFactor
                val drvLen = 0.476 * lambda * velocityFactor
                val dirLen = 0.450 * lambda * velocityFactor
                dimensions.add(AntennaDimensionItem("L_POLE", "Длина каждой из 4 распорок крестовины", spreadLen, 0.0, "Габарит квадрата рамы: ${formatMetricLength(spreadLen * 1.414)}"))
                dimensions.add(AntennaDimensionItem("L_REF", "Проволочный V-рефлектор", refLen, 0.0, "Огибает задние распорки"))
                dimensions.add(AntennaDimensionItem("L_DRV", "Проволочный активный вибратор", drvLen, 0.16 * lambda, "Запитка в центре через балун 1:1"))
                dimensions.add(AntennaDimensionItem("L_DIR", "Проволочный V-директор", dirLen, 0.34 * lambda, "Огибает передние распорки"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 340.0
                gainDbi = 7.4
                fbDb = 23.0
                qFactor = 13.0
            }

            AntennaType.DISCONE -> {
                val coneSlant = 0.275 * lambda * velocityFactor * boomLengthOverrideFactor
                val coneBaseDiam = coneSlant
                val discDiam = 0.7 * coneBaseDiam
                val gapInsulator = (0.008 * lambda).coerceIn(0.002, 0.05)
                dimensions.add(AntennaDimensionItem("D_DISC", "Диаметр верхнего диска (D)", discDiam, 0.0, "8–12 радиальных спиц длиной ${formatMetricLength(discDiam / 2.0)}"))
                dimensions.add(AntennaDimensionItem("L_CONE", "Длина образующей юбки конуса (L)", coneSlant, 0.0, "8–12 наклонных стержней под углом 60° при вершине"))
                dimensions.add(AntennaDimensionItem("D_CONE", "Нижний диаметр основания конуса (C)", coneBaseDiam, 0.0, "Раскрытие юбки конуса в основании"))
                dimensions.add(AntennaDimensionItem("S_GAP", "Изоляционный зазор диск–конус (S)", gapInsulator, 0.0, "Жила кабеля — к диску, оплётка — к вершине конуса"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 60.0
                gainDbi = 2.0
                qFactor = 1.6
            }

            AntennaType.LPDA -> {
                val fMin = safeFreqMhz
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
                dimensions.add(AntennaDimensionItem("R_TERM", "Нагрузочный безындукционный резистор", 0.390, 0.0, "390–450 Ом (P ≥ 35% от P_пер), балун 9:1 в точке питания"))

                rCenter = 49.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 80.0
                gainDbi = 1.5
                efficiencyPct = 72.0
                qFactor = 1.8
            }

            AntennaType.BICONE_BOWTIE -> {
                val coneArm = 0.28 * lambda * velocityFactor * boomLengthOverrideFactor
                val flareWidth = coneArm * 1.15
                val feedGap = (0.006 * lambda).coerceIn(0.002, 0.04)
                dimensions.add(AntennaDimensionItem("L_CONE", "Длина образующей каждого конуса / крыла", coneArm, 0.0, "Два симметричных конуса с углом при вершине 65°"))
                dimensions.add(AntennaDimensionItem("W_FLARE", "Ширина раскрыва на торцах", flareWidth, 0.0, "Обеспечивает перекрытие по частоте до 6:1"))
                dimensions.add(AntennaDimensionItem("S_FEED", "Зазор в центральной точке питания", feedGap, 0.0, "Питание через широкополосный балун 1.5:1 или 2:1"))

                rCenter = 52.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 70.0
                gainDbi = 2.3
                qFactor = 1.9
            }

            AntennaType.DELTA_LOOP -> {
                val perimeter = 1.025 * lambda * velocityFactor * boomLengthOverrideFactor
                val sideEqual = perimeter / 3.0
                val triHeight = sideEqual * 0.866
                val matchStub75 = 0.25 * lambda * 0.66
                dimensions.add(AntennaDimensionItem("P_LOOP", "Полный периметр треугольной рамки (1λ)", perimeter, 0.0, "Замкнутая рамка — не боится статики"))
                dimensions.add(AntennaDimensionItem("L_SIDE", "Длина каждой стороны равносторонней Дельты", sideEqual, 0.0, "При запитке в нижний угол (на расстоянии 0.25λ от вершины) — вертикальная поляризация!"))
                dimensions.add(AntennaDimensionItem("H_TRI", "Высота треугольника от основания до вершины", triHeight, 0.0, "Вершина на мачте, основание параллельно земле"))
                dimensions.add(AntennaDimensionItem("L_MATCH", "Четвертьволновый трансформатор из кабеля 75 Ом", matchStub75, 0.0, "Трансформирует входное R ≈ 110 Ом рамки в 50 Ом"))

                rCenter = 51.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 350.0
                gainDbi = 3.4
                qFactor = 10.0
            }

            AntennaType.MAG_LOOP -> {
                val loopDiam = if (safeFreqMhz < 0.5) {
                    (1.2 * boomLengthOverrideFactor).coerceIn(0.3, 5.0)
                } else {
                    (0.08 * lambda * boomLengthOverrideFactor).coerceIn(0.15, 4.0)
                }
                val loopCircumference = PI * loopDiam
                val couplingDiam = loopDiam * 0.20
                val turns = if (safeFreqMhz < 0.3) max(10, (1.0 / sqrt(safeFreqMhz)).toInt().coerceAtMost(500)) else 1

                val radiusM = loopDiam / 2.0
                val wireRad = max(0.001, wireRadiusM)
                val inductanceH = turns.toDouble().pow(2) * 4e-7 * PI * radiusM * (ln(8.0 * radiusM / wireRad) - 2.0)
                val omega = 2.0 * PI * safeFreqMhz * 1e6
                val xLoop = omega * inductanceH
                val capFarads = 1.0 / (omega * omega * inductanceH)
                capPf = capFarads * 1e12

                val area = PI * radiusM * radiusM
                val rRad = 31171.0 * turns * turns * (area / (lambda * lambda)).pow(2)
                val rLoss = turns * loopCircumference / (2.0 * PI * wireRad) * sqrt(PI * safeFreqMhz * 1e6 * 4e-7 * PI / 5.8e7)
                efficiencyPct = ((rRad / (rRad + rLoss + 1e-12)) * 100.0).coerceIn(0.5, 98.0)
                qFactor = (xLoop / (2.0 * (rRad + rLoss + 1e-6))).coerceIn(25.0, 1200.0)
                capVoltage = sqrt(txPowerWatts * xLoop * qFactor).coerceAtMost(45000.0)

                dimensions.add(AntennaDimensionItem("D_LOOP", "Диаметр основной рамки (D)", loopDiam, 0.0, "Число витков: $turns шт., периметр: ${formatMetricLength(loopCircumference)}"))
                dimensions.add(AntennaDimensionItem("D_COUP", "Диаметр петли связи (d = D/5)", couplingDiam, 0.0, "Располагается внутри основной рамки напротив КПЕ"))
                dimensions.add(AntennaDimensionItem("C_TUNE", "Ёмкость высоковольтного КПЕ", (capPf ?: 50.0) / 1000.0, 0.0, String.format("Требуется: %.1f пФ, напряжение на КПЕ (при %.0f Вт): %.0f В!", capPf ?: 0.0, txPowerWatts, capVoltage ?: 0.0)))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 1400.0
                gainDbi = if (efficiencyPct > 50.0) 1.5 else -6.0 + efficiencyPct * 0.12
            }

            AntennaType.FRACTAL_KOCH -> {
                val outerSpan = 0.31 * lambda * kFactor * boomLengthOverrideFactor
                val segStep1 = outerSpan / 3.0
                val segStep2 = segStep1 / 3.0
                val wireTotal = outerSpan * (4.0 / 3.0).pow(2)
                dimensions.add(AntennaDimensionItem("L_SPAN", "Габаритный размах фрактала Коха 2-го порядка", outerSpan, 0.0, "На 38% компактнее обычного диполя при полной длине провода ${formatMetricLength(wireTotal)}"))
                dimensions.add(AntennaDimensionItem("S_ITER1", "Шаг базового зубца 1-й итерации (L/3)", segStep1, 0.0, "Угол излома сегментов 60°"))
                dimensions.add(AntennaDimensionItem("S_ITER2", "Микро-сегмент 2-й итерации (L/9)", segStep2, 0.0, "Формирует дополнительные высокочастотные резонансы"))

                rCenter = 48.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 260.0
                gainDbi = 2.05
                qFactor = 5.5
            }

            AntennaType.QFH_SATELLITE -> {
                val axialH = 0.26 * lambda * boomLengthOverrideFactor
                val diamLarge = 0.175 * lambda
                val diamSmall = 0.162 * lambda
                val loopLargeTotal = 1.05 * lambda * velocityFactor
                val loopSmallTotal = 0.955 * lambda * velocityFactor
                dimensions.add(AntennaDimensionItem("H_QFH", "Осевая высота спиральной корзины QFH", axialH, 0.0, "Поворот витков на 180° (0.5 оборота) по высоте"))
                dimensions.add(AntennaDimensionItem("D_LARGE", "Диаметр большой петли (Квадратурная +45°)", diamLarge, 0.0, "Полная длина трубки большой петли: ${formatMetricLength(loopLargeTotal)}"))
                dimensions.add(AntennaDimensionItem("D_SMALL", "Диаметр малой петли (Квадратурная -45°)", diamSmall, 0.0, "Полная длина трубки малой петли: ${formatMetricLength(loopSmallTotal)}"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 210.0
                gainDbi = 4.2
                qFactor = 7.0
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
                dimensions.add(AntennaDimensionItem("L_AXIS", "Осевая длина спирали ($nTurns витков)", axialLength, 0.0, "Круговая поляризация RHCP/LHCP"))
                dimensions.add(AntennaDimensionItem("D_REF", "Диаметр экрана-рефлектора", reflectorDiam, 0.0, "Четвертьволновый треугольный трансформатор 140 → 50 Ом"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 180.0
                gainDbi = 10.8 + 10.0 * log10(nTurns * 0.24)
                fbDb = 16.0
                qFactor = 4.5
            }

            AntennaType.HORN_SHF -> {
                val apertureA = 2.4 * lambda * boomLengthOverrideFactor
                val apertureB = 1.8 * lambda * boomLengthOverrideFactor
                val hornLen = 2.8 * lambda
                val wgA = 0.72 * lambda
                val wgB = 0.36 * lambda
                val probeLen = 0.23 * lambda
                dimensions.add(AntennaDimensionItem("A_HORN", "Ширина раскрыва рупора (A, плоскость H)", apertureA, 0.0, "Формирует главный лепесток в плоскости H"))
                dimensions.add(AntennaDimensionItem("B_HORN", "Высота раскрыва рупора (B, плоскость E)", apertureB, 0.0, "Апертурный КПД η ≈ 0.54"))
                dimensions.add(AntennaDimensionItem("L_HORN", "Длина конусной части рупора", hornLen, 0.0, "Плавный волноводный переход"))
                dimensions.add(AntennaDimensionItem("W_FEED", "Сечение питающего волновода (a × b)", wgA, wgB, "Коаксиально-волноводный штырь: ${formatMetricLength(probeLen)}"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 120.0
                gainDbi = 10.0 * log10(4.0 * PI * (apertureA * apertureB * 0.54) / (lambda * lambda))
                fbDb = 25.0
                qFactor = 2.4
            }

            AntennaType.PARABOLIC_DISH -> {
                val dishMult = elementCount.coerceIn(3, 20).toDouble()
                val dishDiam = dishMult * lambda * 0.65 * boomLengthOverrideFactor
                val focalRatio = 0.45
                val focalLen = dishDiam * focalRatio
                val dishDepth = (dishDiam * dishDiam) / (16.0 * focalLen)
                dimensions.add(AntennaDimensionItem("D_DISH", "Диаметр параболического зеркала (D)", dishDiam, 0.0, "Отношение F/D = $focalRatio"))
                dimensions.add(AntennaDimensionItem("F_FOCUS", "Фокусное расстояние до облучателя (F)", focalLen, 0.0, "Точка установки фазового центра облучателя"))
                dimensions.add(AntennaDimensionItem("H_DEPTH", "Глубина чаши параболоида (c)", dishDepth, 0.0, "Формула профиля: y = x² / (4F)"))

                rCenter = 50.0
                xCenter = (boomLengthOverrideFactor - 1.0) * 160.0
                val eff = 0.58
                gainDbi = 10.0 * log10(eff * (PI * dishDiam / lambda).pow(2)).coerceIn(8.0, 48.0)
                fbDb = 30.0
                qFactor = 5.0
            }
        }

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
                AntennaType.DIPOLE, AntennaType.INVERTED_V, AntennaType.EFHW,
                AntennaType.OCFD_WINDOM, AntennaType.G5RV_ZS6BKW, AntennaType.T2FD_BEVERAGE,
                AntennaType.MAG_LOOP, AntennaType.DELTA_LOOP, AntennaType.FRACTAL_KOCH, AntennaType.BICONE_BOWTIE -> {
                    val figure8 = abs(cos(rad)).coerceAtLeast(0.08)
                    20.0 * log10(figure8)
                }
                AntennaType.GROUND_PLANE, AntennaType.VERTICAL_5_8, AntennaType.J_POLE,
                AntennaType.COLLINEAR_COAX, AntennaType.DISCONE, AntennaType.QFH_SATELLITE -> {
                    -0.3 * sin(rad * 4.0)
                }
                AntennaType.YAGI, AntennaType.MOXON, AntennaType.CUBICAL_QUAD, AntennaType.HB9CV,
                AntennaType.HEXBEAM, AntennaType.SPIDERBEAM, AntennaType.LPDA,
                AntennaType.HELICAL_PATCH, AntennaType.HORN_SHF, AntennaType.PARABOLIC_DISH -> {
                    val beamSharpness = if (antennaType == AntennaType.PARABOLIC_DISH) 8.0 else elementCount.coerceIn(2, 10).toDouble() * 0.85
                    val forward = ((1.0 + cos(rad)) / 2.0).pow(beamSharpness)
                    val backLobe = 10.0.pow(-fbDb / 20.0) * ((1.0 - cos(rad)) / 2.0).pow(2.0)
                    val sideRipple = 0.07 * abs(sin(rad * 2.0))
                    val amp = (forward + backLobe + sideRipple).coerceIn(0.015, 1.0)
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
            Math.toDegrees(asin((1.0 / (4.0 * heightInLambda)).coerceIn(0.08, 0.95))).coerceIn(5.0, 75.0)
        } else {
            45.0
        }

        // Line of sight + Ionospheric/Tropospheric range estimation & RF Safety distance
        val radioHorizonKm = 4.12 * (sqrt(mastHeightMeters.coerceAtLeast(1.0)) + sqrt(10.0))
        val effectiveRangeKm = if (safeFreqMhz in 1.5..30.0) {
            // HF Skywave + Groundwave estimate
            (radioHorizonKm * 18.0 * 10.0.pow(gainDbi / 30.0)).coerceAtMost(12000.0)
        } else {
            (radioHorizonKm * (1.0 + gainDbi * 0.09)).coerceAtLeast(5.0)
        }
        val eirpWatts = txPowerWatts * 10.0.pow(gainDbi / 10.0)
        // Safe controlled RF exposure distance (S_lim ~ 2 W/m^2): R = sqrt(EIRP / (4 * pi * S_lim))
        val safeDistMeters = sqrt(eirpWatts / (4.0 * PI * 2.0)).coerceAtLeast(0.25)

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
            loadingCoilUh = coilUh,
            coilTurnsHintRu = coilHint,
            estimatedLineOfSightKm = effectiveRangeKm,
            safeRfDistanceMeters = safeDistMeters,
            sweepPoints = sweepPoints,
            polarPattern = polarPattern,
            physicsDescriptionRu = buildPhysicsDescription(antennaType, safeFreqMhz, lambda),
            assemblyStepsRu = buildAssemblySteps(antennaType, dimensions, safeFreqMhz),
            matchingAdviceRu = buildMatchingAdvice(antennaType, centerPoint.rOhms, centerPoint.xOhms, safeFreqMhz)
        )
    }

    /**
     * Wheeler's single-layer air-core coil formula calculator:
     * L(uH) = (r_inches^2 * N^2) / (9 * r_inches + 10 * l_inches)
     */
    fun buildWheelersCoilHint(inductanceUh: Double, diameterMm: Double): String {
        val rInches = (diameterMm / 2.0) / 25.4
        val turns = sqrt(inductanceUh * (9.0 * rInches + 10.0 * rInches * 1.6) / (rInches * rInches)).coerceIn(2.0, 250.0)
        val lengthMm = diameterMm * 0.8
        return String.format("%.0f витков на оправке Ø%.0f мм (длина намотки ~%.0f мм)", turns, diameterMm, lengthMm)
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
        val base = "Частота ${formatFrequencyDisplay(freqMhz)} (длина волны λ = ${formatMetricLength(lambda)}). "
        return base + when (type) {
            AntennaType.DIPOLE -> "Полуволновый симметричный вибратор с пучностью тока в центре и сопротивлением излучения ~73 Ом в свободном пространстве."
            AntennaType.INVERTED_V -> "Наклон плеч диполя под углом 90°–110° с одной мачты понижает входной импеданс до 50 Ом и расширяет диаграмму направленности для круговой связи."
            AntennaType.EFHW -> "Полуволновый излучатель, питаемый с конца в пучности напряжения (R_вх ≈ 2450 Ом) через широкополосный автотрансформатор 49:1. Резонирует на основной частоте и всех чётных/нечётных гармониках."
            AntennaType.OCFD_WINDOM -> "Несимметричный диполь со смещением точки запитки на 1/3 длины полотна, где импеданс составляет ~200 Ом на нескольких любительских диапазонах одновременно."
            AntennaType.G5RV_ZS6BKW -> "Оптимизированный компьютерным моделированием вариант G5RV (ZS6BKW): секция двухпроводной линии 450 Ом работает как реактивный трансформатор импедансов на 5 диапазонах."
            AntennaType.GROUND_PLANE -> "Четвертьволновый вертикальный штырь с 4 противовесами под углом 135°, обеспечивающий круговой азимутальный обзор и 50-омное согласование."
            AntennaType.VERTICAL_5_8 -> "Удлинённый вертикал 5/8 λ концентрирует максимум энергии вдоль горизонта, давая максимальное усиление среди одноэлементных штырей (+3 дБ относительно λ/4)."
            AntennaType.J_POLE -> "Полуволновый вертикальный излучатель с питанием через четвертьволновый согласующий КЗ-шлейф; не требует радиальных противовесов и может заземляться по постоянному току."
            AntennaType.COLLINEAR_COAX -> "Вертикальная синфазная решётка из полуволновых коаксиальных секций с перекрёстным соединением центральной жилы и экрана на стыках для сложения мощности в горизонтальной плоскости."
            AntennaType.YAGI -> "Классический директорный волновой канал Уда-Яги: пассивный рефлектор и цепочка директоров формируют бегущую волну с высоким усилением и подавлением тыла."
            AntennaType.MOXON -> "Двухэлементный прямоугольник Моксона с загнутыми навстречу друг другу концами элементов: критическая связь между хвостами даёт подавление заднего лепестка до 26–28 дБ при входном R = 50 Ом."
            AntennaType.CUBICAL_QUAD -> "Полноразмерная рамочная антенна «Двойной квадрат» (Cubical Quad) периметром ~1λ с двумя активными полуволновыми участками в каждой рамке."
            AntennaType.HB9CV -> "Двухэлементная антенна с активным питанием обоих вибраторов со сдвигом фазы 225°, обеспечивающая усиление 3-элементной Яги при втрое меньшей длине бума."
            AntennaType.HEXBEAM -> "Шестиугольная направленная антенна с W-образным вибратором и полукруглым рефлектором на 6 стеклопластиковых распорках."
            AntennaType.SPIDERBEAM -> "Многодиапазонный лёгкий волновой канал на крестообразной раме из 4 стеклопластиковых шестов."
            AntennaType.DISCONE -> "Всеволновая дискоконусная антенна с перекрытием по частоте до 10:1 и вертикальной поляризацией — идеальна для SDR-приёмников Малахит и Arinst V2D."
            AntennaType.LPDA -> "Логопериодическая дипольная решётка (LPDA) с постоянным входным сопротивлением и направленностью в сверхширокой полосе частот."
            AntennaType.T2FD_BEVERAGE -> "Апериодическая всеволновая антенна бегущей волны с поглощающим безындукционным резистором, устраняющим стоячие волны в широком диапазоне."
            AntennaType.BICONE_BOWTIE -> "Симметричная биконическая широкополосная антенна с плавным волновым переходом и равномерным КСВ в полосе 6:1."
            AntennaType.DELTA_LOOP -> "Полноразмерная замкнутая треугольная рамка периметром 1λ: при запитке на расстоянии λ/4 от вершины излучает под низким углом к горизонту с вертикальной поляризацией."
            AntennaType.MAG_LOOP -> "Магнитная рамка малого электрического размера с высокой добротностью Q, подавляющая электрическую компоненту бытового смога на 20 дБ."
            AntennaType.FRACTAL_KOCH -> "Самоподобный фрактальный излучатель Коха 2-го порядка: изломы проводника создают распределённую индуктивно-емкостную нагрузку и многорезонансные свойства."
            AntennaType.QFH_SATELLITE -> "Квадрифилярная спиральная антенна из двух ортогональных бифилярных рамок со сдвигом фазы 90°, формирующая полусферическую диаграмму направленности для приёма спутников без поворотного устройства."
            AntennaType.HELICAL_PATCH -> "Осевая спиральная СВЧ-антенна с круговой поляризацией для спутниковой связи, телеметрии и диапазонов до 6 ГГц."
            AntennaType.HORN_SHF -> "Пирамидальный СВЧ-рупор с плавным переходом от прямоугольного волновода к излучающему раскрыву — эталонная антенна для измерений и радиорелейных линий."
            AntennaType.PARABOLIC_DISH -> "Зеркальная параболическая антенна СВЧ, преобразующая сферический фронт волны от облучателя в фокусе F в плоский синфазный фронт с узким лучом."
        }
    }

    private fun buildAssemblySteps(type: AntennaType, dims: List<AntennaDimensionItem>, freqMhz: Double): List<String> {
        val firstDim = dims.firstOrNull()?.formattedLength() ?: ""
        return listOf(
            "1. Заготовка элементов: подготовьте проводники или трубки согласно таблице чертежа (базовый размер: $firstDim с запасом +2% на подгонку) и проверьте 3D-схему узлов крепления.",
            "2. Монтаж точки запитки и согласования: установите разъём SO-239 / N-типа / SMA и рекомендуемый трансформатор/балун для ${type.titleRu}.",
            "3. Сборка геометрии на изоляторах: зафиксируйте элементы на диэлектрических распорках, соблюдая углы и межэлементные расстояния 3D-чертежа.",
            "4. Приборный контроль VNA / SDR: подключите Arinst V2D, Малахит или NanoVNA на частоте ${formatFrequencyDisplay(freqMhz)}, снимите график КСВ и выполните авто-оптимизацию во вкладке «VNA / ДН»."
        )
    }

    private fun buildMatchingAdvice(type: AntennaType, r: Double, x: Double, freqMhz: Double): String {
        return when (type) {
            AntennaType.DIPOLE, AntennaType.INVERTED_V, AntennaType.HEXBEAM, AntennaType.SPIDERBEAM, AntennaType.FRACTAL_KOCH ->
                "Симметрирующий токовый балун 1:1 (8–10 витков коаксиала на кольце FT240-43/61). Входное сопротивление R ≈ ${String.format("%.1f", r)} Ом."
            AntennaType.EFHW ->
                "Широкополосный автотрансформатор 49:1 (2 первичных и 14 вторичных витков на FT240-43 + компенсационный конденсатор 100 пФ 3 кВ параллельно первичной обмотке)."
            AntennaType.OCFD_WINDOM ->
                "Токовый балун Гуанелла 4:1 (200 → 50 Ом) на двухъёмкостном сердечнике + запорный дроссель 1:1 на расстоянии 1.5 м ниже точки питания."
            AntennaType.G5RV_ZS6BKW ->
                "Симметричная двухпроводная линия 450 Ом расчётной длины + токовый балун 1:1 в месте перехода на коаксиальный кабель 50 Ом."
            AntennaType.GROUND_PLANE, AntennaType.COLLINEAR_COAX, AntennaType.MOXON, AntennaType.DISCONE, AntennaType.LPDA, AntennaType.QFH_SATELLITE, AntennaType.HORN_SHF, AntennaType.PARABOLIC_DISH ->
                "Прямое питание коаксиальным кабелем 50 Ом + симметрирующий чулок или ферритовые защёлки у точки запитки."
            AntennaType.VERTICAL_5_8 ->
                "Удлиняющая катушка в основании с отводом от 25–30% витков снизу (автотрансформаторное включение на 50 Ом)."
            AntennaType.J_POLE ->
                "Скользящий контакт вдоль четвертьволнового шлейфа на высоте 3–4% λ от короткозамкнутого основания."
            AntennaType.YAGI, AntennaType.HB9CV ->
                "Гамма-согласование (Gamma Match) с последовательным конструктивным конденсатором или Hairpin-шлейф с балуном 1:1."
            AntennaType.CUBICAL_QUAD, AntennaType.DELTA_LOOP ->
                "Четвертьволновый согласующий трансформатор из коаксиального кабеля 75 Ом длиной (λ/4 × КУ_кабеля) для перехода 110 Ом → 50 Ом."
            AntennaType.T2FD_BEVERAGE ->
                "Трансформатор 9:1 (450 → 50 Ом) в точке запитки и безындукционный резистор 390–450 Ом на противоположном конце."
            AntennaType.BICONE_BOWTIE ->
                "Широкополосный балун 1.5:1 или прямое питание 50 Ом при угле конуса 65°."
            AntennaType.MAG_LOOP ->
                "Индуктивная петля связи диаметром 1/5 от главного кольца напротив высоковольтного вакуумного КПЕ."
            AntennaType.HELICAL_PATCH ->
                "Четвертьволновая треугольная медная согласующая полоска (140 → 50 Ом) на начальном витке у рефлектора."
        }
    }
}

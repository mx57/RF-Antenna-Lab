package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class CoaxCableSpec(
    val id: String,
    val name: String,
    val impedanceOhms: Double,
    val velocityFactor: Double,
    val k1ConductorLoss: Double, // attenuation dB/100m = k1 * sqrt(f_MHz) + k2 * f_MHz
    val k2DielectricLoss: Double,
    val outerDiameterMm: Double,
    val maxPowerWattsAt100Mhz: Double
)

val BUILTIN_COAX_CABLES = listOf(
    CoaxCableSpec("RG58", "RG-58 A/U (50 Ом, Ø5.0 мм, гибкий полевой)", 50.0, 0.66, 1.42, 0.0032, 5.0, 400.0),
    CoaxCableSpec("RG8X", "RG-8X / Mini-8 (50 Ом, Ø6.1 мм, вспененный ПЭ)", 50.0, 0.78, 0.86, 0.0018, 6.1, 750.0),
    CoaxCableSpec("RG213", "RG-213 / U (50 Ом, Ø10.3 мм, базовый КВ/УКВ)", 50.0, 0.66, 0.62, 0.0016, 10.3, 1500.0),
    CoaxCableSpec("RK50_7_11", "РК 50-7-11 (50 Ом, Ø10.0 мм, ГОСТ радиолюбительский)", 50.0, 0.66, 0.68, 0.0017, 10.0, 1400.0),
    CoaxCableSpec("LMR400", "Times Microwave LMR-400 (50 Ом, Ø10.3 мм, Low-Loss)", 50.0, 0.85, 0.39, 0.0008, 10.3, 2500.0),
    CoaxCableSpec("HELIAX_7_8", "Andrew Heliax LDF5-50A 7/8\" (50 Ом, Ø28 мм, магистральный СВЧ)", 50.0, 0.88, 0.115, 0.00025, 28.0, 9000.0)
)

data class CableLossResult(
    val cable: CoaxCableSpec,
    val freqMhz: Double,
    val lengthMeters: Double,
    val loadSwr: Double,
    val txPowerWatts: Double,
    val matchedLossDb: Double,
    val additionalSwrLossDb: Double,
    val totalLossDb: Double,
    val powerAtAntennaWatts: Double,
    val efficiencyPercent: Double,
    val swrAtTransmitter: Double
)

data class PluginExecutionOutput(
    val pluginName: String,
    val primaryValue: Double,
    val primaryUnit: String,
    val formattedSummaryRu: String,
    val breakdownMetrics: List<Pair<String, String>>
)

object PluginEngine {

    /**
     * Built-in reference implementation of the Coaxial Cable Loss & Power Efficiency Plugin
     * (incorporating conductor skin-effect loss k1*sqrt(f), dielectric loss k2*f, and standing-wave mismatch loss).
     */
    fun calculateCoaxCableLoss(
        cable: CoaxCableSpec,
        freqMhz: Double,
        lengthMeters: Double,
        loadSwr: Double,
        txPowerWatts: Double
    ): CableLossResult {
        val safeFreq = freqMhz.coerceIn(0.000001, 6000.0)
        val safeLen = lengthMeters.coerceIn(0.1, 1000.0)
        val safeSwr = loadSwr.coerceIn(1.0, 50.0)
        val safePower = txPowerWatts.coerceIn(0.1, 10000.0)

        val attenPer100m = cable.k1ConductorLoss * sqrt(safeFreq) + cable.k2DielectricLoss * safeFreq
        val matchedLossDb = attenPer100m * (safeLen / 100.0)

        // Mismatch reflection coefficient at antenna load
        val rhoLoad = (safeSwr - 1.0) / (safeSwr + 1.0)
        val lossLinear = 10.0.pow(matchedLossDb / 10.0) // alpha = 10^(A_dB / 10)
        val totalLossLinear = if (lossLinear > 1.0001) {
            (lossLinear * lossLinear - rhoLoad * rhoLoad) / (lossLinear * (1.0 - rhoLoad * rhoLoad))
        } else {
            1.0
        }
        val totalLossDb = (10.0 * log10(totalLossLinear.coerceAtLeast(1.0))).coerceAtLeast(matchedLossDb)
        val swrExtraLossDb = (totalLossDb - matchedLossDb).coerceAtLeast(0.0)

        val powerDelivered = safePower * 10.0.pow(-totalLossDb / 10.0)
        val efficiencyPct = ((powerDelivered / safePower) * 100.0).coerceIn(0.1, 100.0)

        // SWR seen at the transmitter end (attenuated reflection)
        val rhoTx = (rhoLoad / lossLinear).coerceIn(0.0, 0.99)
        val swrAtTx = ((1.0 + rhoTx) / (1.0 - rhoTx)).coerceAtLeast(1.0)

        return CableLossResult(
            cable = cable,
            freqMhz = safeFreq,
            lengthMeters = safeLen,
            loadSwr = safeSwr,
            txPowerWatts = safePower,
            matchedLossDb = matchedLossDb,
            additionalSwrLossDb = swrExtraLossDb,
            totalLossDb = totalLossDb,
            powerAtAntennaWatts = powerDelivered,
            efficiencyPercent = efficiencyPct,
            swrAtTransmitter = swrAtTx
        )
    }

    /**
     * Evaluates a user-authored or built-in plugin formula expression with parameters.
     * Supports variables (freqMhz, lambdaM, swr, powerW, lengthM, k1, k2, dbi, rOhms, xOhms)
     * and functions (sqrt, log10, ln, sin, cos, abs).
     */
    fun executePluginFormula(
        pluginName: String,
        formulaExpression: String,
        inputSchemaJson: String,
        freqMhz: Double,
        swr: Double,
        powerW: Double
    ): PluginExecutionOutput {
        val vars = mutableMapOf(
            "freqmhz" to freqMhz,
            "f" to freqMhz,
            "lambdam" to (299.792458 / freqMhz.coerceAtLeast(0.000001)),
            "swr" to swr.coerceAtLeast(1.0),
            "powerw" to powerW.coerceAtLeast(0.1),
            "lengthm" to 30.0,
            "k1" to 0.39,
            "k2" to 0.0008,
            "dbi" to 2.15,
            "rohms" to 50.0,
            "xohms" to 0.0
        )

        // Parse custom key=value pairs from inputSchemaJson
        inputSchemaJson.split(",", ";", "\n").forEach { token ->
            val parts = token.split("=", ":")
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim().toDoubleOrNull()
                if (key.isNotEmpty() && value != null) {
                    vars[key] = value
                }
            }
        }

        val value = try {
            evaluateMathExpression(formulaExpression, vars)
        } catch (e: Exception) {
            0.0
        }

        val lossPowerW = powerW * 10.0.pow(-abs(value) / 10.0)
        return PluginExecutionOutput(
            pluginName = pluginName,
            primaryValue = value,
            primaryUnit = if (formulaExpression.lowercase().contains("k1") || pluginName.lowercase().contains("loss") || pluginName.lowercase().contains("потер")) "дБ" else "ед.",
            formattedSummaryRu = String.format("Результат расчёта плагина «%s»: %.3f", pluginName, value),
            breakdownMetrics = listOf(
                "Частота (freqMhz)" to formatFrequencyDisplay(freqMhz),
                "Длина волны (lambdaM)" to formatMetricLength(vars["lambdam"] ?: 0.0),
                "Вычисленное значение" to String.format("%.4f", value),
                "Эквивалент мощности (при дБ)" to String.format("%.1f Вт из %.0f Вт", lossPowerW, powerW)
            )
        )
    }

    /**
     * Recursive-descent mathematical expression parser for the RF Plugin SDK.
     */
    fun evaluateMathExpression(expr: String, variables: Map<String, Double>): Double {
        val clean = expr.trim()
        if (clean.isEmpty()) return 0.0
        val parser = object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < clean.length) clean[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code || ch == '\t'.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                return if (x.isFinite()) x else 0.0
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val denom = parseFactor()
                            x = if (abs(denom) > 1e-12) x / denom else 0.0
                        }
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = clean.substring(startPos, pos).toDoubleOrNull() ?: 0.0
                } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch == '_'.code) {
                    while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch in '0'.code..'9'.code || ch == '_'.code) nextChar()
                    val name = clean.substring(startPos, pos).lowercase()
                    if (eat('('.code)) {
                        val arg = parseExpression()
                        eat(')'.code)
                        x = when (name) {
                            "sqrt" -> sqrt(arg.coerceAtLeast(0.0))
                            "log10" -> log10(arg.coerceAtLeast(1e-12))
                            "ln" -> ln(arg.coerceAtLeast(1e-12))
                            "sin" -> sin(arg)
                            "cos" -> cos(arg)
                            "abs" -> abs(arg)
                            else -> arg
                        }
                    } else {
                        x = when (name) {
                            "pi" -> PI
                            else -> variables[name] ?: 1.0
                        }
                    }
                } else {
                    x = 0.0
                }

                if (eat('^'.code)) {
                    x = x.pow(parseFactor())
                }
                return x
            }
        }
        return parser.parse()
    }

    val PLUGIN_SDK_SPECIFICATION_RU = """
        API РАЗРАБОТКИ ПЛАГИНОВ (RF Antenna Lab Plugin SDK v1.2)
        =========================================================
        1. Архитектура расчётно-аппаратных плагинов:
           Плагин описывается JSON/DSL-дескриптором CustomPluginDoc и исполняется в изолированном математическом и протокольном контуре PluginEngine.
        
        2. Категории плагинов (category):
           • CABLE_LOSS — расчёт затухания, КПД и фазовой задержки в фидерных линиях.
           • ANTENNA_FORMULA — пользовательские эмпирические формулы укорочения, индуктивности катушек и трапов.
           • MATCHING_NETWORK — синтез балунов, ШПТЛ и СУ.
           • RECEIVER_DRIVER — профили CAT/AT-команд для внешних приёмников и анализаторов (USB CDC / Bluetooth SPP).
        
        3. Системные переменные, доступные в формуле (formulaExpression):
           • freqMhz (или f) — текущая рабочая частота, МГц (1e-6 .. 6000)
           • lambdaM — длина волны в свободном пространстве, м
           • swr — текущий КСВ нагрузки
           • powerW — мощность передатчика, Вт
           • lengthM, k1, k2 — пользовательские коэффициенты из поля inputSchemaJson (формат: lengthM=30,k1=0.39,k2=0.0008)
        
        4. Поддерживаемые математические функции:
           • sqrt(x), log10(x), ln(x), sin(x), cos(x), abs(x), оператор степени ^ и константа pi.
        
        5. Пример простого плагина расчёта кабельных потерь (LMR-400):
           • Название: Потери в коаксиале LMR-400 (дБ)
           • Параметры (inputSchemaJson): lengthM=35,k1=0.39,k2=0.0008
           • Формула (formulaExpression): (k1 * sqrt(freqMhz) + k2 * freqMhz) * (lengthM / 100) + 0.15 * (swr - 1)
    """.trimIndent()
}

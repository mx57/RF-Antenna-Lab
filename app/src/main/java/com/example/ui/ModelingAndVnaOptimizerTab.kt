package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.AntennaCalculationResult
import com.example.domain.VnaOptimizationResult
import com.example.domain.formatFrequencyDisplay

@Composable
fun ModelingAndVnaOptimizerTab(
    calc: AntennaCalculationResult,
    vnaOpt: VnaOptimizationResult?,
    measuredResonanceMhz: Double,
    measuredR: Double,
    measuredX: Double,
    onUpdateVnaInputs: (Double, Double, Double) -> Unit,
    onApplyOptimizationToGeometry: () -> Unit,
    onImportTouchstoneSample: (String) -> Unit,
    onSaveVnaToCloud: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    var resInput by remember(measuredResonanceMhz) { mutableStateOf(measuredResonanceMhz.toString()) }
    var rInput by remember(measuredR) { mutableStateOf(measuredR.toString()) }
    var xInput by remember(measuredX) { mutableStateOf(measuredX.toString()) }
    var showTouchstoneDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. VNA Measurement Input & Touchstone .s1p Import Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "VNA Optimizer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Интеграция с VNA и Авто-Оптимизация КСВ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Целевая частота настройки: ${formatFrequencyDisplay(calc.frequencyMhz)}. Введите результаты измерения с векторного анализатора цепей (NanoVNA / Arinst V2D) или загрузите файл Touchstone (.s1p) для автоматического пересчёта длины элементов и согласующего звена.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = resInput,
                        onValueChange = { str ->
                            resInput = str
                            str.toDoubleOrNull()?.let { f -> onUpdateVnaInputs(f, measuredR, measuredX) }
                        },
                        label = { Text("Факт. резонанс (МГц)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_vna_resonance"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rInput,
                        onValueChange = { str ->
                            rInput = str
                            str.toDoubleOrNull()?.let { r -> onUpdateVnaInputs(measuredResonanceMhz, r, measuredX) }
                        },
                        label = { Text("R (Ом)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_vna_r"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = xInput,
                        onValueChange = { str ->
                            xInput = str
                            str.toDoubleOrNull()?.let { x -> onUpdateVnaInputs(measuredResonanceMhz, measuredR, x) }
                        },
                        label = { Text("jX (Ом)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_vna_x"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val f0 = calc.frequencyMhz
                            val sampleS1p = """
                                # MHz S RI R 50
                                ${f0 * 0.96} -0.28 -0.32
                                ${f0 * 0.985} -0.06 0.04
                                $f0 0.14 0.21
                                ${f0 * 1.02} 0.31 0.38
                            """.trimIndent()
                            onImportTouchstoneSample(sampleS1p)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_s1p_button")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import S1P")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Импорт .s1p (VNA)")
                    }

                    Button(
                        onClick = onApplyOptimizationToGeometry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_vna_optimization_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Apply Tuning")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Авто-подстройка", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Automated Optimization Recommendation & L-Network Synthesis Card
        if (vnaOpt != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Протокол автоматической оптимизации (${vnaOpt.iterationsRun} итераций симплекс-поиска)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = vnaOpt.actionSummaryRu,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    vnaOpt.stepByStepTuningRu.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSaveVnaToCloud,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_vna_cloud_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Save VNA")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить протокол оптимизации VNA в облако")
                    }
                }
            }
        }

        // 3. SWR(f) Overlay (Measured Amber vs Optimized Cyan)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. График КСВ(f): Измерение VNA (янтарный) vs После оптимизации (голубой)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                SwrFrequencyChartCanvas(
                    primarySweep = vnaOpt?.optimizedSweep ?: calc.sweepPoints,
                    secondarySweep = vnaOpt?.measuredSweep
                )
            }
        }

        // 4. Smith Chart (Impedance Locus Z = R + jX)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = "Smith Chart",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. Диаграмма Вольперта-Смита (Комплексный импеданс Z)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                SmithChartCanvas(
                    primarySweep = vnaOpt?.optimizedSweep ?: calc.sweepPoints,
                    secondarySweep = vnaOpt?.measuredSweep
                )
            }
        }

        // 5. Azimuth (H-plane) & Elevation (E-plane) Polar Radiation Pattern
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Radiation Pattern",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4. Диаграмма направленности (Азимут H и Угол места E)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                PolarPatternCanvas(
                    pattern = calc.polarPattern,
                    gainDbi = calc.gainDbi,
                    fbDb = calc.frontToBackDb
                )
            }
        }
    }
}

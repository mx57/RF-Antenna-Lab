package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.ALL_BAND_PRESETS
import com.example.domain.AntennaCalculationResult
import com.example.domain.AntennaType
import com.example.domain.formatFrequencyDisplay
import com.example.domain.formatMetricLength

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorAndBlueprintTab(
    calc: AntennaCalculationResult,
    onSelectAntennaType: (AntennaType) -> Unit,
    onApplyBandPreset: (Double) -> Unit,
    onUpdateParameters: (Double, Double, Double, Double, Int, Double, Double) -> Unit,
    onExportPdf: () -> Unit,
    onSaveProfileToCloud: (String, String) -> Unit
) {
    val context = LocalContext.current
    var freqInput by remember(calc.frequencyMhz) { mutableStateOf(calc.frequencyMhz.toString()) }
    var wireInput by remember(calc.wireDiameterMm) { mutableStateOf(calc.wireDiameterMm.toString()) }
    var mastInput by remember(calc.mastHeightMeters) { mutableStateOf(calc.mastHeightMeters.toString()) }
    var vfInput by remember(calc.velocityFactor) { mutableStateOf(calc.velocityFactor.toString()) }
    var profileNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Antenna Topology Selector (Standard, Directional, All-Wave, Magnetic Loop / SHF)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Тип антенны (Резонансные, Направленные и Всеволновые)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AntennaType.entries.forEach { type ->
                        val selected = type == calc.antennaType
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectAntennaType(type) },
                            label = {
                                Text(
                                    text = if (type.isAllWave) "★ ${type.titleRu}" else type.titleRu,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("antenna_type_${type.code}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = calc.antennaType.subtitleRu,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Frequency Band Presets (1 Hz ELF up to 6 GHz SHF) & Geometric Parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Диапазон частот (от 1 Гц КНЧ до 6 ГГц СВЧ) и Геометрия",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ALL_BAND_PRESETS.forEach { preset ->
                        AssistChip(
                            onClick = {
                                freqInput = preset.freqMhz.toString()
                                onApplyBandPreset(preset.freqMhz)
                            },
                            label = { Text(preset.labelRu, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("band_preset_${preset.id}"),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = freqInput,
                        onValueChange = { str ->
                            freqInput = str
                            str.toDoubleOrNull()?.let { f ->
                                onUpdateParameters(
                                    f,
                                    calc.upperFreqMhz,
                                    calc.wireDiameterMm,
                                    calc.mastHeightMeters,
                                    calc.elementCount,
                                    calc.velocityFactor,
                                    calc.txPowerWatts
                                )
                            }
                        },
                        label = { Text("Частота F0 (МГц, 1e-6=1Гц)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_frequency_mhz"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = wireInput,
                        onValueChange = { str ->
                            wireInput = str
                            str.toDoubleOrNull()?.let { w ->
                                onUpdateParameters(
                                    calc.frequencyMhz,
                                    calc.upperFreqMhz,
                                    w,
                                    calc.mastHeightMeters,
                                    calc.elementCount,
                                    calc.velocityFactor,
                                    calc.txPowerWatts
                                )
                            }
                        },
                        label = { Text("Ø проводника (мм)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_wire_mm"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = mastInput,
                        onValueChange = { str ->
                            mastInput = str
                            str.toDoubleOrNull()?.let { m ->
                                onUpdateParameters(
                                    calc.frequencyMhz,
                                    calc.upperFreqMhz,
                                    calc.wireDiameterMm,
                                    m,
                                    calc.elementCount,
                                    calc.velocityFactor,
                                    calc.txPowerWatts
                                )
                            }
                        },
                        label = { Text("Высота подвеса H (м)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_mast_height"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vfInput,
                        onValueChange = { str ->
                            vfInput = str
                            str.toDoubleOrNull()?.let { vf ->
                                onUpdateParameters(
                                    calc.frequencyMhz,
                                    calc.upperFreqMhz,
                                    calc.wireDiameterMm,
                                    calc.mastHeightMeters,
                                    calc.elementCount,
                                    vf,
                                    calc.txPowerWatts
                                )
                            }
                        },
                        label = { Text("Коэф. укорочения КУ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_velocity_factor"),
                        singleLine = true
                    )
                }

                if (calc.antennaType in listOf(AntennaType.YAGI, AntennaType.LPDA, AntennaType.CUBICAL_QUAD, AntennaType.HELICAL_PATCH)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Число элементов / витков антенны: ${calc.elementCount} шт.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Slider(
                        value = calc.elementCount.toFloat(),
                        onValueChange = { v ->
                            onUpdateParameters(
                                calc.frequencyMhz,
                                calc.upperFreqMhz,
                                calc.wireDiameterMm,
                                calc.mastHeightMeters,
                                v.toInt(),
                                calc.velocityFactor,
                                calc.txPowerWatts
                            )
                        },
                        valueRange = 2f..15f,
                        steps = 12,
                        modifier = Modifier.testTag("slider_element_count")
                    )
                }
            }
        }

        // 3. Key Calculated Electrical Metrics Dashboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Расчётные параметры (${formatFrequencyDisplay(calc.frequencyMhz)} • λ = ${formatMetricLength(calc.wavelengthMeters)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3
                ) {
                    MetricBadgeCard(
                        title = "Импеданс Zin",
                        value = String.format("%.1f %+.1fj Ω", calc.centerImpedanceR, calc.centerImpedanceX),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "КСВ (50 Ом)",
                        value = String.format("%.2f : 1", calc.centerSwr50),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Усиление G",
                        value = String.format("%.2f dBi", calc.gainDbi),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Полоса КСВ≤2",
                        value = String.format("%.1f кГц", calc.bandwidthSwr2Khz),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Подавление F/B",
                        value = String.format("%.1f дБ", calc.frontToBackDb),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Угол излучения",
                        value = String.format("%.1f° (КПД %.0f%%)", calc.takeOffAngleDeg, calc.radiationEfficiencyPct),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Interactive Vector CAD Blueprint & Dimension Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Blueprint",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. Сборочный чертёж и спецификация размеров",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                AntennaBlueprintCanvas(calc = calc)

                Spacer(modifier = Modifier.height(12.dp))

                calc.dimensions.forEach { dim ->
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[${dim.code}] ${dim.labelRu}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = dim.formattedLength(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (dim.noteRu.isNotEmpty() || dim.positionMeters > 0.0) {
                            val posText = if (dim.positionMeters > 0.0) "Позиция на буме: ${dim.formattedPosition()} • " else ""
                            Text(
                                text = "$posText${dim.noteRu}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // 5. Physics Explanation & Step-by-Step Assembly Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Physics",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4. Физическое описание и схема согласования",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = calc.physicsDescriptionRu,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Узел согласования: ${calc.matchingAdviceRu}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Assembly",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5. Пошаговая инструкция по сборке и настройке",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                calc.assemblyStepsRu.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // 6. Export to PDF & Save to Cloud Action Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Экспорт документации и облачное сохранение профиля",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Название профиля для сохранения в облако (опционально)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_name"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("export_pdf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Экспорт PDF", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onSaveProfileToCloud(profileNameInput, calc.matchingAdviceRu)
                            profileNameInput = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_profile_cloud_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Save Cloud")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("В облако", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadgeCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

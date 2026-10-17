package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DiscoveredHardwarePort
import com.example.domain.HardwareBridgeManager
import com.example.domain.RadioReceiverProfile
import com.example.domain.SdrSpectrumFrame
import com.example.domain.formatFrequencyDisplay

@Composable
fun SdrReceiverBridgeTab(
    selectedReceiver: RadioReceiverProfile,
    selectedTransport: String,
    spanKhz: Double,
    isStreamRunning: Boolean,
    spectrumFrame: SdrSpectrumFrame,
    waterfallHistory: List<List<Float>>,
    discoveredPorts: List<DiscoveredHardwarePort>,
    onSelectReceiver: (RadioReceiverProfile) -> Unit,
    onSelectTransport: (String) -> Unit,
    onUpdateSpanKhz: (Double) -> Unit,
    onToggleStream: () -> Unit,
    onScanHardware: () -> Unit,
    onCaptureToVnaOptimizer: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onScanHardware()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Receiver Model Selector (Arinst V2D, Малахит DSP, NanoVNA-H4, TinySA Ultra)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "SDR Receivers",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Подключение приёмников и анализаторов (USB / Bluetooth)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioReceiverProfile.entries.forEach { profile ->
                        FilterChip(
                            selected = profile == selectedReceiver,
                            onClick = { onSelectReceiver(profile) },
                            label = { Text(profile.titleRu, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.testTag("receiver_${profile.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${selectedReceiver.protocolDescriptionRu}\nДиапазон: ${selectedReceiver.freqRangeRu} • ${selectedReceiver.usbVidPidHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Transport selector + Hardware Scan button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTransport == "USB_OTG",
                        onClick = { onSelectTransport("USB_OTG") },
                        label = { Text("USB OTG (CDC/Serial)") },
                        leadingIcon = { Icon(Icons.Default.Usb, contentDescription = "USB") },
                        modifier = Modifier.testTag("transport_usb")
                    )
                    FilterChip(
                        selected = selectedTransport == "BLUETOOTH",
                        onClick = { onSelectTransport("BLUETOOTH") },
                        label = { Text("Bluetooth SPP/BLE") },
                        leadingIcon = { Icon(Icons.Default.BluetoothSearching, contentDescription = "BT") },
                        modifier = Modifier.testTag("transport_bt")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                btPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            } else {
                                onScanHardware()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_hardware_ports_button")
                    ) {
                        Icon(Icons.Default.BluetoothSearching, contentDescription = "Scan")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Опрос портов USB/BT")
                    }

                    Button(
                        onClick = onToggleStream,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_sdr_stream_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreamRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isStreamRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Stream"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isStreamRunning) "Пауза потока" else "Пуск потока")
                    }
                }

                if (discoveredPorts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    discoveredPorts.forEach { port ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${port.name} [${port.transportType}]",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = port.detailsRu,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Real-Time Spectrum & Color Waterfall Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Панорама спектра и Водопад в реальном времени (${selectedReceiver.titleRu})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Центр: ${formatFrequencyDisplay(spectrumFrame.centerFreqMhz)} • Полоса обзора: ${spanKhz.toInt()} кГц • Команда CAT: ${
                        HardwareBridgeManager.buildCatCommandPreview(
                            selectedReceiver,
                            spectrumFrame.centerFreqMhz,
                            spanKhz
                        )
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                SdrSpectrumWaterfallCanvas(
                    frame = spectrumFrame,
                    waterfallHistory = waterfallHistory
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Полоса обзора панорамы (Span): ${spanKhz.toInt()} кГц",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = spanKhz.toFloat(),
                    onValueChange = { onUpdateSpanKhz(it.toDouble()) },
                    valueRange = 50f..5000f,
                    modifier = Modifier.testTag("slider_sdr_span")
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadgeCard(
                        title = "S-Метр / Уровень",
                        value = spectrumFrame.sMeterLabel,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Отношение С/Ш",
                        value = String.format("+%.1f дБ (Шум %.0f)", spectrumFrame.snrDb, spectrumFrame.noiseFloorDbm),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "КСВ на частоте",
                        value = String.format("%.2f (R=%.0f Ω)", spectrumFrame.liveVnaSwr, spectrumFrame.liveVnaR),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCaptureToVnaOptimizer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("capture_sdr_to_vna_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.Default.SendToMobile, contentDescription = "Send to VNA")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Передать текущий замер в авто-оптимизатор антенны",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

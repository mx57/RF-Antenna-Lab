package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.RfCloudRepository
import com.example.ui.CalculatorAndBlueprintTab
import com.example.ui.MainWorkspaceTab
import com.example.ui.ModelingAndVnaOptimizerTab
import com.example.ui.PluginsAndCloudTab
import com.example.ui.RfLabViewModel
import com.example.ui.SdrReceiverBridgeTab
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                RfLabRootNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun RfLabRootNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    // Launch directly into the RF Antenna Lab workspace without blocking on a login screen!
    // Google Sign-In is optional and accessible in the Settings / Cloud tab.
    RfLabWorkspaceScreen(
        currentUserId = currentUser?.uid,
        userEmail = currentUser?.email,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RfLabWorkspaceScreen(
    currentUserId: String?,
    userEmail: String?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: RfLabViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                RfLabViewModel(RfCloudRepository(db), currentUserId)
            }
        }
    )

    LaunchedEffect(currentUserId) {
        viewModel.updateAuthenticatedUser(currentUserId)
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val calcResult by viewModel.calculationResult.collectAsStateWithLifecycle()
    val synthesisGoal by viewModel.synthesisGoal.collectAsStateWithLifecycle()
    val maxSpanLimitMeters by viewModel.maxSpanLimitMeters.collectAsStateWithLifecycle()
    val synthesizedBlueprint by viewModel.synthesizedBlueprint.collectAsStateWithLifecycle()
    val vnaOptResult by viewModel.vnaOptimizationResult.collectAsStateWithLifecycle()
    val measResMhz by viewModel.measuredResonanceMhz.collectAsStateWithLifecycle()
    val measR by viewModel.measuredR.collectAsStateWithLifecycle()
    val measX by viewModel.measuredX.collectAsStateWithLifecycle()

    val selectedReceiver by viewModel.selectedReceiver.collectAsStateWithLifecycle()
    val selectedTransport by viewModel.selectedTransport.collectAsStateWithLifecycle()
    val spanKhz by viewModel.spanKhz.collectAsStateWithLifecycle()
    val isStreamRunning by viewModel.isStreamRunning.collectAsStateWithLifecycle()
    val spectrumFrame by viewModel.currentSpectrumFrame.collectAsStateWithLifecycle()
    val waterfallHistory by viewModel.waterfallHistory.collectAsStateWithLifecycle()
    val discoveredPorts by viewModel.discoveredPorts.collectAsStateWithLifecycle()

    val selectedCoax by viewModel.selectedCoaxCable.collectAsStateWithLifecycle()
    val coaxLength by viewModel.coaxLengthMeters.collectAsStateWithLifecycle()
    val cableLossResult by viewModel.cableLossResult.collectAsStateWithLifecycle()
    val lastPluginExecution by viewModel.lastPluginExecution.collectAsStateWithLifecycle()

    val savedProfiles by viewModel.savedProfilesState.collectAsStateWithLifecycle()
    val savedVna by viewModel.savedVnaState.collectAsStateWithLifecycle()
    val savedPlugins by viewModel.savedPluginsState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RF Antenna Lab • 1 Гц – 6 ГГц",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (userEmail != null) "Облако: $userEmail" else "Автономный режим • 3D САПР, 26 антенн, VNA и SDR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportAndSharePdf(context) },
                        modifier = Modifier.testTag("top_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Экспорт паспорта антенны в PDF",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectTab(MainWorkspaceTab.PLUGINS_CLOUD) },
                        modifier = Modifier.testTag("top_cloud_settings_button")
                    ) {
                        Icon(
                            imageVector = if (currentUserId != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = "Настройки облачной синхронизации",
                            tint = if (currentUserId != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Переключить тему"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainWorkspaceTab.CALCULATOR,
                    onClick = { viewModel.selectTab(MainWorkspaceTab.CALCULATOR) },
                    icon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = "Калькулятор") },
                    label = { Text("3D Чертёж", maxLines = 1) },
                    modifier = Modifier.testTag("nav_tab_calculator")
                )
                NavigationBarItem(
                    selected = selectedTab == MainWorkspaceTab.MODELING_VNA,
                    onClick = { viewModel.selectTab(MainWorkspaceTab.MODELING_VNA) },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = "Моделирование и VNA") },
                    label = { Text("VNA / ДН", maxLines = 1) },
                    modifier = Modifier.testTag("nav_tab_vna")
                )
                NavigationBarItem(
                    selected = selectedTab == MainWorkspaceTab.SDR_RECEIVERS,
                    onClick = { viewModel.selectTab(MainWorkspaceTab.SDR_RECEIVERS) },
                    icon = { Icon(Icons.Default.Radio, contentDescription = "Приёмники и Спектр") },
                    label = { Text("Приёмники", maxLines = 1) },
                    modifier = Modifier.testTag("nav_tab_sdr")
                )
                NavigationBarItem(
                    selected = selectedTab == MainWorkspaceTab.PLUGINS_CLOUD,
                    onClick = { viewModel.selectTab(MainWorkspaceTab.PLUGINS_CLOUD) },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "Плагины и Настройки") },
                    label = { Text("Плагины", maxLines = 1) },
                    modifier = Modifier.testTag("nav_tab_plugins")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 860.dp)
            ) {
                AnimatedVisibility(visible = statusMessage != null) {
                    statusMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { viewModel.clearStatusMessage() }) {
                                    Text("ОК")
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    MainWorkspaceTab.CALCULATOR -> {
                        CalculatorAndBlueprintTab(
                            calc = calcResult,
                            synthesisGoal = synthesisGoal,
                            maxSpanLimitMeters = maxSpanLimitMeters,
                            synthesizedBlueprint = synthesizedBlueprint,
                            onSelectAntennaType = viewModel::updateAntennaType,
                            onApplyBandPreset = viewModel::applyBandPreset,
                            onUpdateParameters = viewModel::updateParameters,
                            onUpdateSynthesisConfig = viewModel::updateSynthesisConfig,
                            onSynthesizeNewTopology = viewModel::synthesizeNewAntennaTopology,
                            onClearSynthesizedBlueprint = viewModel::clearSynthesizedBlueprint,
                            onSaveSynthesizedToProfiles = viewModel::saveSynthesizedBlueprintToProfiles,
                            onExportPdf = { viewModel.exportAndSharePdf(context) },
                            onSaveProfileToCloud = viewModel::saveCurrentProfileToCloud
                        )
                    }
                    MainWorkspaceTab.MODELING_VNA -> {
                        ModelingAndVnaOptimizerTab(
                            calc = calcResult,
                            vnaOpt = vnaOptResult,
                            measuredResonanceMhz = measResMhz,
                            measuredR = measR,
                            measuredX = measX,
                            onUpdateVnaInputs = viewModel::updateVnaInputs,
                            onApplyOptimizationToGeometry = viewModel::applyVnaOptimizationToGeometry,
                            onImportTouchstoneSample = viewModel::importTouchstoneS1pSample,
                            onSaveVnaToCloud = viewModel::saveVnaOptimizationToCloud,
                            onNavigateBack = { viewModel.selectTab(MainWorkspaceTab.CALCULATOR) }
                        )
                    }
                    MainWorkspaceTab.SDR_RECEIVERS -> {
                        SdrReceiverBridgeTab(
                            selectedReceiver = selectedReceiver,
                            selectedTransport = selectedTransport,
                            spanKhz = spanKhz,
                            isStreamRunning = isStreamRunning,
                            spectrumFrame = spectrumFrame,
                            waterfallHistory = waterfallHistory,
                            discoveredPorts = discoveredPorts,
                            onSelectReceiver = viewModel::selectReceiver,
                            onSelectTransport = viewModel::selectTransport,
                            onUpdateSpanKhz = viewModel::updateSpanKhz,
                            onToggleStream = viewModel::toggleSpectrumStream,
                            onScanHardware = { viewModel.refreshHardwarePorts(context) },
                            onCaptureToVnaOptimizer = {
                                viewModel.captureLiveSdrToVnaOptimizer()
                                viewModel.selectTab(MainWorkspaceTab.MODELING_VNA)
                            },
                            onNavigateBack = { viewModel.selectTab(MainWorkspaceTab.CALCULATOR) }
                        )
                    }
                    MainWorkspaceTab.PLUGINS_CLOUD -> {
                        PluginsAndCloudTab(
                            currentUserId = currentUserId,
                            userEmail = userEmail,
                            selectedCoax = selectedCoax,
                            coaxLengthMeters = coaxLength,
                            cableLossResult = cableLossResult,
                            lastPluginExecution = lastPluginExecution,
                            savedProfilesState = savedProfiles,
                            savedVnaState = savedVna,
                            savedPluginsState = savedPlugins,
                            onUpdateCoax = viewModel::updateCoaxCable,
                            onCreatePlugin = viewModel::saveCustomPluginToCloud,
                            onExecutePlugin = viewModel::executeCustomPlugin,
                            onDeletePlugin = viewModel::deletePluginFromCloud,
                            onLoadProfile = viewModel::loadProfileFromCloud,
                            onDeleteProfile = viewModel::deleteProfileFromCloud,
                            onDeleteVna = viewModel::deleteVnaMeasurementFromCloud,
                            onNavigateBack = { viewModel.selectTab(MainWorkspaceTab.CALCULATOR) }
                        )
                    }
                }
            }
        }
    }
}

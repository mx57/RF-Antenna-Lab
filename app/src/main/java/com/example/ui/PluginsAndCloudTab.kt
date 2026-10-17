package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.AntennaProfileDoc
import com.example.data.CustomPluginDoc
import com.example.data.VnaMeasurementDoc
import com.example.domain.BUILTIN_COAX_CABLES
import com.example.domain.CableLossResult
import com.example.domain.CoaxCableSpec
import com.example.domain.PluginEngine
import com.example.domain.PluginExecutionOutput
import com.example.domain.formatFrequencyDisplay

@Composable
fun PluginsAndCloudTab(
    currentUserId: String?,
    userEmail: String?,
    selectedCoax: CoaxCableSpec,
    coaxLengthMeters: Double,
    cableLossResult: CableLossResult,
    lastPluginExecution: PluginExecutionOutput?,
    savedProfilesState: CloudListState<AntennaProfileDoc>,
    savedVnaState: CloudListState<VnaMeasurementDoc>,
    savedPluginsState: CloudListState<CustomPluginDoc>,
    onUpdateCoax: (CoaxCableSpec, Double) -> Unit,
    onCreatePlugin: (String, String, String, String, String, String) -> Unit,
    onExecutePlugin: (CustomPluginDoc) -> Unit,
    onDeletePlugin: (String) -> Unit,
    onLoadProfile: (AntennaProfileDoc) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDeleteVna: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authInfoMessage by remember { mutableStateOf<String?>(null) }

    var showSdkSpec by remember { mutableStateOf(false) }
    var newPluginName by remember { mutableStateOf("Расчёт потерь в фидере LMR-400") }
    var newPluginAuthor by remember { mutableStateOf("RA3-DEV") }
    var newPluginFormula by remember { mutableStateOf("(k1 * sqrt(freqMhz) + k2 * freqMhz) * (lengthM / 100) + 0.15 * (swr - 1)") }
    var newPluginSchema by remember { mutableStateOf("lengthM=35,k1=0.39,k2=0.0008") }
    var newPluginDesc by remember { mutableStateOf("Вычисляет суммарное затухание в кабеле (дБ) с учётом скин-эффекта, потерь в диэлектрике и КСВ.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Optional Cloud Account & Sync Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Настройки облачной синхронизации (Опционально)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (currentUserId == null) {
                    Text(
                        text = "Сейчас приложение работает в автономном локальном режиме — все расчёты, профили антенн, замеры VNA и плагины доступны без ограничений. При желании вы можете войти через Google для синхронизации между устройствами.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (authInfoMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = authInfoMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            authInfoMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    authInfoMessage = "Облачная синхронизация Google успешно подключена!"
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    authInfoMessage = "Локальный режим сохранён ($msg)"
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                    authInfoMessage = "Вход отменён — продолжаем работу в автономном режиме."
                                }
                            )
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("optional_google_sign_in_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Google Sign In")
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Sign in with Google • Подключить облако",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Синхронизация активна: ${userEmail ?: "Google Аккаунт"}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Профили антенн, VNA и плагины синхронизируются с Firebase Firestore",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {
                                        authInfoMessage = "Вы вышли из облачного аккаунта."
                                    },
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("settings_sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Выйти")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выйти")
                        }
                    }
                }
            }
        }

        // 1. Reference Built-In Plugin: Coaxial Cable Loss & Efficiency Calculator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cable,
                        contentDescription = "Coax Loss Plugin",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Эталонный плагин: Расчёт кабельных потерь и КПД фидера",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Расчёт погонного затухания коаксиального кабеля на частоте ${formatFrequencyDisplay(cableLossResult.freqMhz)} с учётом дополнительного затухания от стоячих волн (КСВ = ${String.format("%.2f", cableLossResult.loadSwr)}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BUILTIN_COAX_CABLES.forEach { cable ->
                        FilterChip(
                            selected = cable.id == selectedCoax.id,
                            onClick = { onUpdateCoax(cable, coaxLengthMeters) },
                            label = { Text(cable.name, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("coax_${cable.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Длина коаксиальной линии: ${String.format("%.1f", coaxLengthMeters)} м (КУ кабеля = ${selectedCoax.velocityFactor})",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = coaxLengthMeters.toFloat(),
                    onValueChange = { onUpdateCoax(selectedCoax, it.toDouble()) },
                    valueRange = 2f..150f,
                    modifier = Modifier.testTag("slider_coax_length")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadgeCard(
                        title = "Полные потери",
                        value = String.format("%.2f дБ (КСВ +%.2f)", cableLossResult.totalLossDb, cableLossResult.additionalSwrLossDb),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Доходит в антенну",
                        value = String.format("%.1f Вт из %.0f Вт", cableLossResult.powerAtAntennaWatts, cableLossResult.txPowerWatts),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "КПД фидера",
                        value = String.format("%.1f%%", cableLossResult.efficiencyPercent),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Plugin SDK Documentation & Custom Formula Plugin Studio
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Plugin SDK",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Конструктор пользовательских плагинов и API",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    OutlinedButton(
                        onClick = { showSdkSpec = !showSdkSpec },
                        modifier = Modifier.testTag("toggle_sdk_spec_button")
                    ) {
                        Text(if (showSdkSpec) "Скрыть API" else "Описание API")
                    }
                }

                if (showSdkSpec) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = PluginEngine.PLUGIN_SDK_SPECIFICATION_RU,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPluginName,
                    onValueChange = { newPluginName = it },
                    label = { Text("Название плагина") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_plugin_name"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPluginFormula,
                    onValueChange = { newPluginFormula = it },
                    label = { Text("Математическая формула DSL (freqMhz, lambdaM, swr, lengthM, k1, k2)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_plugin_formula")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPluginSchema,
                        onValueChange = { newPluginSchema = it },
                        label = { Text("Параметры (key=val)") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_plugin_schema"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPluginAuthor,
                        onValueChange = { newPluginAuthor = it },
                        label = { Text("Позывной / Автор") },
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("input_plugin_author"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onCreatePlugin(
                            newPluginName,
                            newPluginAuthor,
                            "CABLE_LOSS",
                            newPluginDesc,
                            newPluginFormula,
                            newPluginSchema
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_plugin_button")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Plugin")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить и зарегистрировать плагин", fontWeight = FontWeight.Bold)
                }

                if (lastPluginExecution != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${lastPluginExecution.formattedSummaryRu} ${lastPluginExecution.primaryUnit}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            lastPluginExecution.breakdownMetrics.forEach { (k, v) ->
                                Text(
                                    text = "• $k: $v",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Installed Custom Plugins List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Установленные пользовательские плагины",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                when (savedPluginsState) {
                    is CloudListState.Loading -> Text("Загрузка плагинов...")
                    is CloudListState.Error -> Text(savedPluginsState.message, color = MaterialTheme.colorScheme.error)
                    is CloudListState.Success -> {
                        savedPluginsState.items.forEach { plug ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${plug.name} (v${plug.version} • ${plug.author})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Формула: ${plug.formulaExpression}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(
                                    onClick = { onExecutePlugin(plug) },
                                    modifier = Modifier.testTag("run_plugin_${plug.id}")
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = "Выполнить плагин",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeletePlugin(plug.id) },
                                    modifier = Modifier.testTag("delete_plugin_${plug.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить плагин",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }

        // 4. Saved Antenna Profiles & VNA Measurements
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Saved Profiles",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4. Сохранённые профили антенн и архив замеров VNA",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (savedProfilesState) {
                    is CloudListState.Loading -> Text("Загрузка профилей...")
                    is CloudListState.Error -> Text(savedProfilesState.message, color = MaterialTheme.colorScheme.error)
                    is CloudListState.Success -> {
                        savedProfilesState.items.forEach { prof ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prof.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${prof.antennaType} • ${formatFrequencyDisplay(prof.frequencyMhz)} • КСВ ${String.format("%.2f", prof.calculatedSwr)} • ${String.format("%.1f dBi", prof.gainDbi)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onLoadProfile(prof) },
                                    modifier = Modifier.testTag("load_profile_${prof.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Загрузить профиль",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteProfile(prof.id) },
                                    modifier = Modifier.testTag("delete_profile_${prof.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить профиль",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }

                if (savedVnaState is CloudListState.Success && savedVnaState.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Архив протоколов оптимизации VNA (${savedVnaState.items.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    savedVnaState.items.forEach { vna ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${vna.deviceName} [${vna.connectionType}] • F0=${formatFrequencyDisplay(vna.targetFreqMhz)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = vna.recommendation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteVna(vna.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить замер VNA",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

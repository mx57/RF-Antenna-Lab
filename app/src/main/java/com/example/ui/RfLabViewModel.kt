package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AntennaProfileDoc
import com.example.data.CustomPluginDoc
import com.example.data.RfCloudRepository
import com.example.data.VnaMeasurementDoc
import com.example.domain.AntennaCalculationResult
import com.example.domain.AntennaGeneticSynthesizer
import com.example.domain.AntennaPhysicsEngine
import com.example.domain.AntennaType
import com.example.domain.BUILTIN_COAX_CABLES
import com.example.domain.CableLossResult
import com.example.domain.CoaxCableSpec
import com.example.domain.DiscoveredHardwarePort
import com.example.domain.HardwareBridgeManager
import com.example.domain.PdfPassportGenerator
import com.example.domain.PluginEngine
import com.example.domain.PluginExecutionOutput
import com.example.domain.RadioReceiverProfile
import com.example.domain.SdrSpectrumFrame
import com.example.domain.SynthesisGoal
import com.example.domain.SynthesizedAntennaBlueprint
import com.example.domain.VnaOptimizationResult
import com.example.domain.VnaOptimizerEngine
import com.example.domain.formatFrequencyDisplay
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudListState<out T> {
    data object Loading : CloudListState<Nothing>
    data class Success<T>(val items: List<T>) : CloudListState<T>
    data class Error(val message: String) : CloudListState<Nothing>
}

enum class MainWorkspaceTab(val titleRu: String) {
    CALCULATOR("Калькулятор и Чертёж"),
    MODELING_VNA("Моделирование и VNA"),
    SDR_RECEIVERS("Приёмники и Спектр"),
    PLUGINS_CLOUD("Плагины и Настройки")
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RfLabViewModel(
    private val repository: RfCloudRepository,
    initialUserId: String?
) : ViewModel() {

    private val _authenticatedUserId = MutableStateFlow(initialUserId)
    val authenticatedUserId: StateFlow<String?> = _authenticatedUserId.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainWorkspaceTab.CALCULATOR)
    val selectedTab: StateFlow<MainWorkspaceTab> = _selectedTab.asStateFlow()

    // Local session storage so profiles, VNA sweeps, and custom plugins work offline/without sign-in
    private val _localProfiles = MutableStateFlow(
        listOf(
            AntennaProfileDoc(
                id = "local_prof_20m",
                name = "КВ 20м Полуволновый диполь (14.175 МГц)",
                antennaType = "DIPOLE",
                frequencyMhz = 14.175,
                wireDiameterMm = 2.5,
                mastHeightM = 12.0,
                elementCount = 1L,
                velocityFactor = 0.95,
                calculatedSwr = 1.14,
                gainDbi = 5.80,
                notes = "Балун 1:1 на кольце FT240-43"
            ),
            AntennaProfileDoc(
                id = "local_prof_2m_yagi",
                name = "УКВ 2м 5-эл. Волновой канал (145.500 МГц)",
                antennaType = "YAGI",
                frequencyMhz = 145.500,
                wireDiameterMm = 8.0,
                mastHeightM = 8.0,
                elementCount = 5L,
                velocityFactor = 0.96,
                calculatedSwr = 1.08,
                gainDbi = 9.50,
                notes = "Гамма-согласование 50 Ом"
            ),
            AntennaProfileDoc(
                id = "local_prof_discone",
                name = "Всеволновая Дискоконусная (100–1300 МГц)",
                antennaType = "DISCONE",
                frequencyMhz = 145.0,
                wireDiameterMm = 4.0,
                mastHeightM = 10.0,
                elementCount = 9L,
                velocityFactor = 0.96,
                calculatedSwr = 1.12,
                gainDbi = 2.00,
                notes = "Обзорная антенна для SDR Малахит и Arinst V2D"
            )
        )
    )

    private val _localVnaMeasurements = MutableStateFlow<List<VnaMeasurementDoc>>(emptyList())

    private val _localPlugins = MutableStateFlow(
        listOf(
            CustomPluginDoc(
                id = "local_plug_lmr400",
                name = "Потери в коаксиале LMR-400 (дБ)",
                author = "RA3-LAB",
                version = "1.2.0",
                category = "CABLE_LOSS",
                description = "Расчёт затухания кабеля LMR-400 с учётом длины и КСВ",
                formulaExpression = "(k1 * sqrt(freqMhz) + k2 * freqMhz) * (lengthM / 100) + 0.15 * (swr - 1)",
                inputSchemaJson = "lengthM=35,k1=0.39,k2=0.0008",
                enabled = true
            ),
            CustomPluginDoc(
                id = "local_plug_trap",
                name = "Индуктивность ВЧ-дросселя / Трапа (мкГн)",
                author = "UA3-ENG",
                version = "1.0.0",
                category = "ANTENNA_FORMULA",
                description = "Резонансная индуктивность контура для ёмкости C пФ на текущей частоте",
                formulaExpression = "25330 / (freqMhz * freqMhz * cPf)",
                inputSchemaJson = "cPf=47",
                enabled = true
            )
        )
    )

    // --- 1. Antenna Calculator State (1 Hz to 6 GHz + All-Wave) ---
    private val _selectedAntennaType = MutableStateFlow(AntennaType.DIPOLE)
    val selectedAntennaType: StateFlow<AntennaType> = _selectedAntennaType.asStateFlow()

    private val _frequencyMhz = MutableStateFlow(14.175)
    val frequencyMhz: StateFlow<Double> = _frequencyMhz.asStateFlow()

    private val _upperFreqMhz = MutableStateFlow(50.0)
    val upperFreqMhz: StateFlow<Double> = _upperFreqMhz.asStateFlow()

    private val _wireDiameterMm = MutableStateFlow(2.5)
    val wireDiameterMm: StateFlow<Double> = _wireDiameterMm.asStateFlow()

    private val _mastHeightMeters = MutableStateFlow(12.0)
    val mastHeightMeters: StateFlow<Double> = _mastHeightMeters.asStateFlow()

    private val _elementCount = MutableStateFlow(3)
    val elementCount: StateFlow<Int> = _elementCount.asStateFlow()

    private val _velocityFactor = MutableStateFlow(0.95)
    val velocityFactor: StateFlow<Double> = _velocityFactor.asStateFlow()

    private val _txPowerWatts = MutableStateFlow(100.0)
    val txPowerWatts: StateFlow<Double> = _txPowerWatts.asStateFlow()

    private val _lengthScaleOverride = MutableStateFlow(1.0)
    val lengthScaleOverride: StateFlow<Double> = _lengthScaleOverride.asStateFlow()

    private val _calculationResult = MutableStateFlow(recalculateInternal())
    val calculationResult: StateFlow<AntennaCalculationResult> = _calculationResult.asStateFlow()

    // --- 1B. Evolutionary / Genetic Antenna Topology Generator State ---
    private val _synthesisGoal = MutableStateFlow(SynthesisGoal.MAX_DX_GAIN)
    val synthesisGoal: StateFlow<SynthesisGoal> = _synthesisGoal.asStateFlow()

    private val _maxSpanLimitMeters = MutableStateFlow(10.0)
    val maxSpanLimitMeters: StateFlow<Double> = _maxSpanLimitMeters.asStateFlow()

    private var synthesisSeedCounter = 1
    private val _synthesizedBlueprint = MutableStateFlow<SynthesizedAntennaBlueprint?>(null)
    val synthesizedBlueprint: StateFlow<SynthesizedAntennaBlueprint?> = _synthesizedBlueprint.asStateFlow()

    // --- 2. VNA Modeling & Auto-Optimizer State ---
    private val _measuredResonanceMhz = MutableStateFlow(13.920)
    val measuredResonanceMhz: StateFlow<Double> = _measuredResonanceMhz.asStateFlow()

    private val _measuredR = MutableStateFlow(44.5)
    val measuredR: StateFlow<Double> = _measuredR.asStateFlow()

    private val _measuredX = MutableStateFlow(18.2)
    val measuredX: StateFlow<Double> = _measuredX.asStateFlow()

    private val _vnaOptimizationResult = MutableStateFlow<VnaOptimizationResult?>(null)
    val vnaOptimizationResult: StateFlow<VnaOptimizationResult?> = _vnaOptimizationResult.asStateFlow()

    // --- 3. SDR Receiver & Real-Time Spectrum Analyzer State ---
    private val _selectedReceiver = MutableStateFlow(RadioReceiverProfile.ARINST_V2D)
    val selectedReceiver: StateFlow<RadioReceiverProfile> = _selectedReceiver.asStateFlow()

    private val _selectedTransport = MutableStateFlow("USB_OTG")
    val selectedTransport: StateFlow<String> = _selectedTransport.asStateFlow()

    private val _spanKhz = MutableStateFlow(500.0)
    val spanKhz: StateFlow<Double> = _spanKhz.asStateFlow()

    private val _discoveredPorts = MutableStateFlow<List<DiscoveredHardwarePort>>(emptyList())
    val discoveredPorts: StateFlow<List<DiscoveredHardwarePort>> = _discoveredPorts.asStateFlow()

    private val _isStreamRunning = MutableStateFlow(true)
    val isStreamRunning: StateFlow<Boolean> = _isStreamRunning.asStateFlow()

    private val _currentSpectrumFrame = MutableStateFlow(
        HardwareBridgeManager.generateSpectrumTelemetryFrame(
            receiver = RadioReceiverProfile.ARINST_V2D,
            centerFreqMhz = 14.175,
            spanKhz = 500.0,
            tickIndex = 0L,
            modulationMode = "SSB"
        )
    )
    val currentSpectrumFrame: StateFlow<SdrSpectrumFrame> = _currentSpectrumFrame.asStateFlow()

    private val _waterfallHistory = MutableStateFlow<List<List<Float>>>(emptyList())
    val waterfallHistory: StateFlow<List<List<Float>>> = _waterfallHistory.asStateFlow()

    private var spectrumJob: Job? = null
    private var tickCounter = 0L

    // --- 4. Custom Plugin System & Coax Cable Loss Calculator State ---
    private val _selectedCoaxCable = MutableStateFlow(BUILTIN_COAX_CABLES[4])
    val selectedCoaxCable: StateFlow<CoaxCableSpec> = _selectedCoaxCable.asStateFlow()

    private val _coaxLengthMeters = MutableStateFlow(30.0)
    val coaxLengthMeters: StateFlow<Double> = _coaxLengthMeters.asStateFlow()

    private val _cableLossResult = MutableStateFlow(recalculateCableLossInternal())
    val cableLossResult: StateFlow<CableLossResult> = _cableLossResult.asStateFlow()

    private val _lastPluginExecution = MutableStateFlow<PluginExecutionOutput?>(null)
    val lastPluginExecution: StateFlow<PluginExecutionOutput?> = _lastPluginExecution.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // --- 5. Auth-Gated Firestore Cloud Sync Streams combined with Local Session ---
    // CRITICAL: Never query Firestore when uid == null; only attach snapshot listeners when signed in!
    val savedProfilesState: StateFlow<CloudListState<AntennaProfileDoc>> =
        _authenticatedUserId.flatMapLatest { uid ->
            if (uid == null) {
                _localProfiles.map<List<AntennaProfileDoc>, CloudListState<AntennaProfileDoc>> {
                    CloudListState.Success(it)
                }
            } else {
                combine(
                    repository.observeAntennaProfiles(uid),
                    _localProfiles
                ) { cloudList, localList ->
                    val merged = (cloudList + localList).distinctBy { it.name }
                    CloudListState.Success(merged) as CloudListState<AntennaProfileDoc>
                }.catch { error ->
                    Log.w(TAG, "Error observing antenna_profiles", error)
                    emit(CloudListState.Success(_localProfiles.value))
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudListState.Success(_localProfiles.value)
        )

    val savedVnaState: StateFlow<CloudListState<VnaMeasurementDoc>> =
        _authenticatedUserId.flatMapLatest { uid ->
            if (uid == null) {
                _localVnaMeasurements.map<List<VnaMeasurementDoc>, CloudListState<VnaMeasurementDoc>> {
                    CloudListState.Success(it)
                }
            } else {
                combine(
                    repository.observeVnaMeasurements(uid),
                    _localVnaMeasurements
                ) { cloudList, localList ->
                    CloudListState.Success((cloudList + localList).distinctBy { it.id }) as CloudListState<VnaMeasurementDoc>
                }.catch { error ->
                    Log.w(TAG, "Error observing vna_measurements", error)
                    emit(CloudListState.Success(_localVnaMeasurements.value))
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudListState.Success(_localVnaMeasurements.value)
        )

    val savedPluginsState: StateFlow<CloudListState<CustomPluginDoc>> =
        _authenticatedUserId.flatMapLatest { uid ->
            if (uid == null) {
                _localPlugins.map<List<CustomPluginDoc>, CloudListState<CustomPluginDoc>> {
                    CloudListState.Success(it)
                }
            } else {
                combine(
                    repository.observeCustomPlugins(uid),
                    _localPlugins
                ) { cloudList, localList ->
                    CloudListState.Success((cloudList + localList).distinctBy { it.name }) as CloudListState<CustomPluginDoc>
                }.catch { error ->
                    Log.w(TAG, "Error observing custom_plugins", error)
                    emit(CloudListState.Success(_localPlugins.value))
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudListState.Success(_localPlugins.value)
        )

    init {
        runVnaAutoOptimization()
        startSpectrumTelemetryLoop()
    }

    fun updateAuthenticatedUser(userId: String?) {
        _authenticatedUserId.value = userId
    }

    fun selectTab(tab: MainWorkspaceTab) {
        _selectedTab.value = tab
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateAntennaType(type: AntennaType) {
        _selectedAntennaType.value = type
        _elementCount.value = type.defaultElements
        _lengthScaleOverride.value = 1.0
        _synthesizedBlueprint.value = null
        if (_frequencyMhz.value < type.minFreqMhz || _frequencyMhz.value > type.maxFreqMhz) {
            _frequencyMhz.value = when (type) {
                AntennaType.DISCONE, AntennaType.COLLINEAR_COAX, AntennaType.QFH_SATELLITE -> 145.50
                AntennaType.HELICAL_PATCH, AntennaType.HORN_SHF, AntennaType.PARABOLIC_DISH -> 1296.0
                else -> 14.175
            }
        }
        recalculateAll()
    }

    fun updateSynthesisConfig(goal: SynthesisGoal, maxSpanM: Double) {
        _synthesisGoal.value = goal
        _maxSpanLimitMeters.value = maxSpanM.coerceIn(0.15, 250.0)
    }

    fun synthesizeNewAntennaTopology() {
        synthesisSeedCounter++
        val synth = AntennaGeneticSynthesizer.synthesizeOptimalAntenna(
            frequencyMhz = _frequencyMhz.value,
            maxSpanMeters = _maxSpanLimitMeters.value,
            mastHeightMeters = _mastHeightMeters.value,
            goal = _synthesisGoal.value,
            seedVariation = synthesisSeedCounter
        )
        _synthesizedBlueprint.value = synth
        _statusMessage.value = "Синтезирована 3D-топология «${synth.designNameRu}» (G=${String.format("%.2f", synth.predictedGainDbi)} dBi, КСВ=${String.format("%.2f", synth.predictedSwr50)})"
    }

    fun clearSynthesizedBlueprint() {
        _synthesizedBlueprint.value = null
    }

    fun saveSynthesizedBlueprintToProfiles() {
        val synth = _synthesizedBlueprint.value ?: return
        saveCurrentProfileToCloud(
            customName = "${synth.designNameRu} (${formatFrequencyDisplay(synth.frequencyMhz)})",
            customNotes = "${synth.engineeringSummaryRu} | ${synth.matchingNetworkRu}"
        )
    }

    fun applyBandPreset(freqMhz: Double) {
        _frequencyMhz.value = freqMhz.coerceIn(0.000001, 6000.0)
        _upperFreqMhz.value = (freqMhz * 3.0).coerceAtMost(6000.0)
        _measuredResonanceMhz.value = (freqMhz * 0.985).coerceIn(0.000001, 6000.0)
        _lengthScaleOverride.value = 1.0
        recalculateAll()
    }

    fun updateParameters(
        freqMhz: Double = _frequencyMhz.value,
        upperMhz: Double = _upperFreqMhz.value,
        wireMm: Double = _wireDiameterMm.value,
        mastM: Double = _mastHeightMeters.value,
        elements: Int = _elementCount.value,
        vf: Double = _velocityFactor.value,
        powerW: Double = _txPowerWatts.value
    ) {
        _frequencyMhz.value = freqMhz.coerceIn(0.000001, 6000.0)
        _upperFreqMhz.value = upperMhz.coerceIn(_frequencyMhz.value, 6000.0)
        _wireDiameterMm.value = wireMm.coerceIn(0.05, 500.0)
        _mastHeightMeters.value = mastM.coerceIn(0.2, 500.0)
        _elementCount.value = elements.coerceIn(1, 32)
        _velocityFactor.value = vf.coerceIn(0.5, 1.0)
        _txPowerWatts.value = powerW.coerceIn(0.5, 10000.0)
        recalculateAll()
    }

    fun updateVnaInputs(measResonanceMhz: Double, rOhms: Double, xOhms: Double) {
        _measuredResonanceMhz.value = measResonanceMhz.coerceIn(0.000001, 6000.0)
        _measuredR.value = rOhms.coerceIn(1.0, 5000.0)
        _measuredX.value = xOhms.coerceIn(-5000.0, 5000.0)
        runVnaAutoOptimization()
    }

    fun runVnaAutoOptimization() {
        val base = _calculationResult.value
        val opt = VnaOptimizerEngine.optimizeFromVna(
            baseCalc = base,
            measuredResonanceMhz = _measuredResonanceMhz.value,
            measuredRAtTarget = _measuredR.value,
            measuredXAtTarget = _measuredX.value
        )
        _vnaOptimizationResult.value = opt
    }

    fun applyVnaOptimizationToGeometry() {
        val opt = _vnaOptimizationResult.value ?: return
        _lengthScaleOverride.value = opt.optimizedLengthScaleFactor
        _mastHeightMeters.value = opt.optimalMastHeightMeters
        _measuredResonanceMhz.value = _frequencyMhz.value
        _measuredR.value = opt.predictedR
        _measuredX.value = opt.predictedX
        recalculateAll()
        _statusMessage.value = "Оптимизированная геометрия применена к чертежу! Новый КСВ = ${String.format("%.2f", opt.predictedSwrAfterTuning)}"
    }

    fun importTouchstoneS1pSample(sampleS1pText: String) {
        val points = VnaOptimizerEngine.parseTouchstoneS1p(sampleS1pText)
        if (points.isNotEmpty()) {
            val minSwrPt = points.minByOrNull { it.swr50 } ?: points.first()
            val targetPt = points.minByOrNull { kotlin.math.abs(it.freqMhz - _frequencyMhz.value) } ?: minSwrPt
            _measuredResonanceMhz.value = minSwrPt.freqMhz
            _measuredR.value = targetPt.rOhms
            _measuredX.value = targetPt.xOhms
            runVnaAutoOptimization()
            _statusMessage.value = "Импортирован Touchstone .s1p (${points.size} точек). Минимум КСВ на ${formatFrequencyDisplay(minSwrPt.freqMhz)}"
        }
    }

    fun captureLiveSdrToVnaOptimizer() {
        val frame = _currentSpectrumFrame.value
        _measuredResonanceMhz.value = frame.peakFreqMhz
        _measuredR.value = frame.liveVnaR
        _measuredX.value = frame.liveVnaX
        runVnaAutoOptimization()
        _statusMessage.value = "Замер с ${_selectedReceiver.value.titleRu} передан в оптимизатор VNA!"
    }

    fun selectReceiver(receiver: RadioReceiverProfile) {
        _selectedReceiver.value = receiver
    }

    fun selectTransport(transport: String) {
        _selectedTransport.value = transport
    }

    fun updateSpanKhz(span: Double) {
        _spanKhz.value = span.coerceIn(10.0, 10000.0)
    }

    fun toggleSpectrumStream() {
        _isStreamRunning.value = !_isStreamRunning.value
    }

    fun refreshHardwarePorts(context: Context) {
        val usbPorts = HardwareBridgeManager.scanUsbDevices(context)
        val btPorts = HardwareBridgeManager.scanPairedBluetoothDevices(context)
        val combined = usbPorts + btPorts
        _discoveredPorts.value = combined
        _statusMessage.value = if (combined.isEmpty()) {
            "Физические порты USB OTG / Bluetooth не обнаружены — активен потоковый телеметрический стенд ${_selectedReceiver.value.titleRu}."
        } else {
            "Найдено аппаратных радиоинтерфейсов: ${combined.size} шт."
        }
    }

    fun updateCoaxCable(cable: CoaxCableSpec, lengthM: Double = _coaxLengthMeters.value) {
        _selectedCoaxCable.value = cable
        _coaxLengthMeters.value = lengthM.coerceIn(0.5, 500.0)
        _cableLossResult.value = recalculateCableLossInternal()
    }

    fun executeCustomPlugin(plugin: CustomPluginDoc) {
        val calc = _calculationResult.value
        val output = PluginEngine.executePluginFormula(
            pluginName = plugin.name,
            formulaExpression = plugin.formulaExpression,
            inputSchemaJson = plugin.inputSchemaJson,
            freqMhz = calc.frequencyMhz,
            swr = calc.centerSwr50,
            powerW = calc.txPowerWatts
        )
        _lastPluginExecution.value = output
        _statusMessage.value = output.formattedSummaryRu
    }

    fun exportAndSharePdf(context: Context) {
        viewModelScope.launch {
            val calc = _calculationResult.value
            val vna = _vnaOptimizationResult.value
            val result = PdfPassportGenerator.generatePassportPdf(context, calc, vna)
            result.onSuccess { outcome ->
                _statusMessage.value = "PDF-паспорт сохранён: ${outcome.fileName} (${outcome.sizeBytes / 1024} КБ)"
                try {
                    PdfPassportGenerator.sharePdfFile(context, outcome.file)
                } catch (e: Exception) {
                    Log.w(TAG, "Share dialog skipped in headless/test environment", e)
                }
            }.onFailure { err ->
                _statusMessage.value = "Ошибка генерации PDF: ${err.localizedMessage}"
            }
        }
    }

    // --- Local + Optional Cloud Persistence Actions ---
    fun saveCurrentProfileToCloud(customName: String, customNotes: String) {
        val calc = _calculationResult.value
        val profileTitle = customName.ifBlank {
            "${calc.antennaType.titleRu.take(28)} • ${formatFrequencyDisplay(calc.frequencyMhz)}"
        }
        val localId = "local_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val doc = AntennaProfileDoc(
            id = localId,
            name = profileTitle,
            antennaType = calc.antennaType.code,
            frequencyMhz = calc.frequencyMhz,
            wireDiameterMm = calc.wireDiameterMm,
            mastHeightM = calc.mastHeightMeters,
            elementCount = calc.elementCount.toLong(),
            velocityFactor = calc.velocityFactor,
            calculatedSwr = calc.centerSwr50,
            gainDbi = calc.gainDbi,
            notes = customNotes.ifBlank { calc.matchingAdviceRu }
        )
        _localProfiles.value = listOf(doc) + _localProfiles.value

        val uid = _authenticatedUserId.value
        if (uid != null) {
            viewModelScope.launch {
                val res = repository.createAntennaProfile(doc)
                _statusMessage.value = if (res.isSuccess) {
                    "Профиль «$profileTitle» сохранён и синхронизирован с облаком Firebase!"
                } else {
                    "Профиль «$profileTitle» сохранён локально."
                }
            }
        } else {
            _statusMessage.value = "Профиль «$profileTitle» сохранён! (Для облачной синхронизации войдите через Google на вкладке Плагины/Настройки)."
        }
    }

    fun loadProfileFromCloud(doc: AntennaProfileDoc) {
        _selectedAntennaType.value = AntennaType.fromCode(doc.antennaType)
        _frequencyMhz.value = doc.frequencyMhz
        _wireDiameterMm.value = doc.wireDiameterMm
        _mastHeightMeters.value = doc.mastHeightM
        _elementCount.value = doc.elementCount.toInt()
        _velocityFactor.value = doc.velocityFactor
        _lengthScaleOverride.value = 1.0
        recalculateAll()
        _selectedTab.value = MainWorkspaceTab.CALCULATOR
        _statusMessage.value = "Загружен профиль «${doc.name}»"
    }

    fun deleteProfileFromCloud(profileId: String) {
        _localProfiles.value = _localProfiles.value.filterNot { it.id == profileId }
        if (_authenticatedUserId.value != null && !profileId.startsWith("local_")) {
            viewModelScope.launch {
                repository.deleteAntennaProfile(profileId)
            }
        }
    }

    fun saveVnaOptimizationToCloud() {
        val opt = _vnaOptimizationResult.value ?: return
        val localId = "local_vna_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val doc = VnaMeasurementDoc(
            id = localId,
            deviceName = _selectedReceiver.value.titleRu,
            connectionType = _selectedTransport.value,
            targetFreqMhz = opt.targetFreqMhz,
            measuredResonanceMhz = opt.measuredResonanceMhz,
            measuredSwr = opt.measuredSwrAtTarget,
            measuredR = opt.measuredR,
            measuredX = opt.measuredX,
            recommendation = opt.actionSummaryRu
        )
        _localVnaMeasurements.value = listOf(doc) + _localVnaMeasurements.value

        if (_authenticatedUserId.value != null) {
            viewModelScope.launch {
                val res = repository.createVnaMeasurement(doc)
                _statusMessage.value = if (res.isSuccess) {
                    "Замер VNA и протокол оптимизации сохранены в облако!"
                } else {
                    "Замер VNA сохранён локально."
                }
            }
        } else {
            _statusMessage.value = "Протокол оптимизации VNA сохранён!"
        }
    }

    fun deleteVnaMeasurementFromCloud(id: String) {
        _localVnaMeasurements.value = _localVnaMeasurements.value.filterNot { it.id == id }
        if (_authenticatedUserId.value != null && !id.startsWith("local_")) {
            viewModelScope.launch {
                repository.deleteVnaMeasurement(id)
            }
        }
    }

    fun saveCustomPluginToCloud(
        name: String,
        author: String,
        category: String,
        description: String,
        formula: String,
        schema: String
    ) {
        val localId = "local_plug_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val doc = CustomPluginDoc(
            id = localId,
            name = name.ifBlank { "Плагин кабельных потерь" },
            author = author.ifBlank { "RA3-LAB" },
            version = "1.0.0",
            category = category,
            description = description.ifBlank { "Расчёт затухания и КПД антенно-фидерного тракта" },
            formulaExpression = formula.ifBlank { "(k1 * sqrt(freqMhz) + k2 * freqMhz) * (lengthM / 100)" },
            inputSchemaJson = schema.ifBlank { "lengthM=30,k1=0.39,k2=0.0008" },
            enabled = true
        )
        _localPlugins.value = listOf(doc) + _localPlugins.value

        if (_authenticatedUserId.value != null) {
            viewModelScope.launch {
                val res = repository.createCustomPlugin(doc)
                _statusMessage.value = if (res.isSuccess) {
                    "Плагин «${doc.name}» добавлен и синхронизирован с облаком!"
                } else {
                    "Плагин «${doc.name}» зарегистрирован локально!"
                }
            }
        } else {
            _statusMessage.value = "Плагин «${doc.name}» зарегистрирован и готов к расчётам!"
        }
    }

    fun deletePluginFromCloud(id: String) {
        _localPlugins.value = _localPlugins.value.filterNot { it.id == id }
        if (_authenticatedUserId.value != null && !id.startsWith("local_")) {
            viewModelScope.launch {
                repository.deleteCustomPlugin(id)
            }
        }
    }

    private fun recalculateAll() {
        _calculationResult.value = recalculateInternal()
        _cableLossResult.value = recalculateCableLossInternal()
        runVnaAutoOptimization()
    }

    private fun recalculateInternal(): AntennaCalculationResult {
        return AntennaPhysicsEngine.calculate(
            antennaType = _selectedAntennaType.value,
            frequencyMhz = _frequencyMhz.value,
            upperFreqMhz = _upperFreqMhz.value,
            wireDiameterMm = _wireDiameterMm.value,
            mastHeightMeters = _mastHeightMeters.value,
            elementCount = _elementCount.value,
            velocityFactor = _velocityFactor.value,
            txPowerWatts = _txPowerWatts.value,
            boomLengthOverrideFactor = _lengthScaleOverride.value
        )
    }

    private fun recalculateCableLossInternal(): CableLossResult {
        val calc = _calculationResult.value
        return PluginEngine.calculateCoaxCableLoss(
            cable = _selectedCoaxCable.value,
            freqMhz = calc.frequencyMhz,
            lengthMeters = _coaxLengthMeters.value,
            loadSwr = calc.centerSwr50,
            txPowerWatts = calc.txPowerWatts
        )
    }

    private fun startSpectrumTelemetryLoop() {
        spectrumJob?.cancel()
        spectrumJob = viewModelScope.launch {
            while (true) {
                if (_isStreamRunning.value) {
                    tickCounter++
                    val frame = HardwareBridgeManager.generateSpectrumTelemetryFrame(
                        receiver = _selectedReceiver.value,
                        centerFreqMhz = _frequencyMhz.value,
                        spanKhz = _spanKhz.value,
                        tickIndex = tickCounter,
                        modulationMode = "SSB"
                    )
                    _currentSpectrumFrame.value = frame
                    val updatedHistory = listOf(frame.binsDbm) + _waterfallHistory.value.take(23)
                    _waterfallHistory.value = updatedHistory
                }
                delay(280L)
            }
        }
    }

    companion object {
        private const val TAG = "RfLabViewModel"
    }
}

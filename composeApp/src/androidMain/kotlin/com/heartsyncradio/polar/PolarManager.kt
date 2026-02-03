package com.heartsyncradio.polar

import android.content.Context
import android.util.Log
import com.heartsyncradio.hrv.HrvMetrics
import com.heartsyncradio.hrv.HrvProcessor
import com.heartsyncradio.model.ConnectionState
import com.heartsyncradio.model.HeartRateData
import com.heartsyncradio.model.PolarDeviceInfo
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo as SdkPolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PolarManager(context: Context) {

    companion object {
        private const val TAG = "PolarManager"
    }

    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context.applicationContext,
        setOf(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING
        )
    )

    private var scanDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null
    private val hrvProcessor = HrvProcessor()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceId = MutableStateFlow<String?>(null)
    val connectedDeviceId: StateFlow<String?> = _connectedDeviceId.asStateFlow()

    private val _heartRateData = MutableStateFlow<HeartRateData?>(null)
    val heartRateData: StateFlow<HeartRateData?> = _heartRateData.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<PolarDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<PolarDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hrvMetrics = MutableStateFlow<HrvMetrics?>(null)
    val hrvMetrics: StateFlow<HrvMetrics?> = _hrvMetrics.asStateFlow()

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                if (!powered) {
                    _error.value = "Bluetooth is turned off"
                }
            }

            override fun deviceConnected(polarDeviceInfo: SdkPolarDeviceInfo) {
                Log.d(TAG, "Device connected: ${polarDeviceInfo.deviceId}")
                _connectionState.value = ConnectionState.CONNECTED
                _connectedDeviceId.value = polarDeviceInfo.deviceId
                startHrStreaming(polarDeviceInfo.deviceId)
            }

            override fun deviceConnecting(polarDeviceInfo: SdkPolarDeviceInfo) {
                Log.d(TAG, "Device connecting: ${polarDeviceInfo.deviceId}")
                _connectionState.value = ConnectionState.CONNECTING
            }

            override fun deviceDisconnected(polarDeviceInfo: SdkPolarDeviceInfo) {
                Log.d(TAG, "Device disconnected: ${polarDeviceInfo.deviceId}")
                hrDisposable?.dispose()
                hrDisposable = null
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectedDeviceId.value = null
                _heartRateData.value = null
                _batteryLevel.value = null
                _hrvMetrics.value = null
                hrvProcessor.reset()
            }

            override fun batteryLevelReceived(deviceId: String, level: Int) {
                Log.d(TAG, "Battery: $level%")
                _batteryLevel.value = level
            }

            override fun disInformationReceived(deviceId: String, uuid: java.util.UUID, value: String) {
                Log.d(TAG, "DIS info: $uuid = $value")
            }

            override fun disInformationReceived(deviceId: String, disInfo: com.polar.androidcommunications.api.ble.model.DisInfo) {
                Log.d(TAG, "DIS info: ${disInfo.key} = ${disInfo.value}")
            }

            override fun htsNotificationReceived(deviceId: String, data: com.polar.sdk.api.model.PolarHealthThermometerData) {
                Log.d(TAG, "HTS notification received")
            }
        })
    }

    fun startScan() {
        scanDisposable?.dispose()
        _scannedDevices.value = emptyList()
        _isScanning.value = true
        _error.value = null

        scanDisposable = api.searchForDevice()
            .subscribeOn(Schedulers.io())
            .subscribe(
                { deviceInfo ->
                    val mapped = PolarDeviceInfo(
                        deviceId = deviceInfo.deviceId,
                        name = deviceInfo.name,
                        rssi = deviceInfo.rssi,
                        isConnectable = deviceInfo.isConnectable
                    )
                    val current = _scannedDevices.value.toMutableList()
                    if (current.none { it.deviceId == mapped.deviceId }) {
                        current.add(mapped)
                        _scannedDevices.value = current
                    }
                },
                { error ->
                    Log.e(TAG, "Scan error", error)
                    _error.value = "Scan failed: ${error.message}"
                    _isScanning.value = false
                },
                {
                    _isScanning.value = false
                }
            )
    }

    fun stopScan() {
        scanDisposable?.dispose()
        scanDisposable = null
        _isScanning.value = false
    }

    fun connectToDevice(deviceId: String) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        try {
            api.connectToDevice(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e)
            _error.value = "Connection failed: ${e.message}"
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun disconnectFromDevice() {
        val deviceId = _connectedDeviceId.value ?: return
        _connectionState.value = ConnectionState.DISCONNECTING
        try {
            api.disconnectFromDevice(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed", e)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun shutDown() {
        scanDisposable?.dispose()
        hrDisposable?.dispose()
        api.shutDown()
    }

    private fun startHrStreaming(deviceId: String) {
        hrDisposable?.dispose()
        hrDisposable = api.startHrStreaming(deviceId)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { hrData ->
                    val samples = hrData.samples
                    if (samples.isNotEmpty()) {
                        val sample = samples.last()
                        _heartRateData.value = HeartRateData(
                            hr = sample.hr,
                            rrIntervals = sample.rrsMs,
                            contactStatus = sample.contactStatus,
                            timestamp = System.currentTimeMillis()
                        )

                        // Feed RR intervals into HRV processor
                        if (sample.rrsMs.isNotEmpty()) {
                            val metrics = hrvProcessor.addRrIntervals(sample.rrsMs)
                            if (metrics != null) {
                                _hrvMetrics.value = metrics
                            }
                        }
                    }
                },
                { error ->
                    Log.e(TAG, "HR streaming error", error)
                    _error.value = "HR streaming failed: ${error.message}"
                }
            )
    }
}

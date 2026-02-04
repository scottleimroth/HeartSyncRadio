package com.heartsyncradio.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heartsyncradio.ble.GenericBleManager
import com.heartsyncradio.ble.HrDeviceManager
import com.heartsyncradio.polar.PolarManager
import com.heartsyncradio.viewmodel.HomeViewModel

enum class DeviceMode {
    POLAR,
    GENERIC_BLE
}

object AppModule {

    @Volatile
    private var deviceManager: HrDeviceManager? = null

    @Volatile
    var currentMode: DeviceMode = DeviceMode.POLAR
        private set

    fun getDeviceManager(context: Context, mode: DeviceMode = currentMode): HrDeviceManager {
        // If mode changed, shut down old manager
        if (mode != currentMode && deviceManager != null) {
            deviceManager?.shutDown()
            deviceManager = null
            currentMode = mode
        }
        return deviceManager ?: synchronized(this) {
            deviceManager ?: createManager(context, mode).also {
                deviceManager = it
                currentMode = mode
            }
        }
    }

    private fun createManager(context: Context, mode: DeviceMode): HrDeviceManager {
        return when (mode) {
            DeviceMode.POLAR -> PolarManager(context.applicationContext)
            DeviceMode.GENERIC_BLE -> GenericBleManager(context.applicationContext)
        }
    }

    fun switchMode(context: Context, mode: DeviceMode): HrDeviceManager {
        return getDeviceManager(context, mode)
    }

    fun provideHomeViewModelFactory(context: Context): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                HomeViewModel(getDeviceManager(context))
            }
        }
    }

    fun shutDown() {
        deviceManager?.shutDown()
        deviceManager = null
    }
}

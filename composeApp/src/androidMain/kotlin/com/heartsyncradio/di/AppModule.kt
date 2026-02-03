package com.heartsyncradio.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heartsyncradio.polar.PolarManager
import com.heartsyncradio.viewmodel.HomeViewModel

object AppModule {

    @Volatile
    private var polarManager: PolarManager? = null

    fun getPolarManager(context: Context): PolarManager {
        return polarManager ?: synchronized(this) {
            polarManager ?: PolarManager(context.applicationContext).also {
                polarManager = it
            }
        }
    }

    fun provideHomeViewModelFactory(context: Context): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                HomeViewModel(getPolarManager(context))
            }
        }
    }

    fun shutDown() {
        polarManager?.shutDown()
        polarManager = null
    }
}

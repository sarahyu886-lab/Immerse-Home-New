package com.pico.welcomespace.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.pico.welcomespace.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}

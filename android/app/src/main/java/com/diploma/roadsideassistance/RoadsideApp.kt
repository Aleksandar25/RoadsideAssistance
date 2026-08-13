package com.diploma.roadsideassistance

import android.app.Application
import com.diploma.roadsideassistance.core.AppContainer

class RoadsideApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.di.AppContainerImpl

class MovieApplication : Application() {
    
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl()
    }
}

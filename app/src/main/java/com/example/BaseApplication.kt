package com.example

import android.app.Application
import com.example.core.di.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@BaseApplication)
                workManagerFactory()
                modules(listOf(databaseModule))
            }
        }
        
        // Schedule periodic cloud sync (metadata consistency check)
        val backgroundManager = org.koin.core.context.GlobalContext.get().get<com.example.feature.background.BackgroundManager>()
        backgroundManager.scheduleCloudSync()
    }
}

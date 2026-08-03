package com.example

import android.app.Application
import com.example.core.di.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@BaseApplication)
                modules(listOf(databaseModule))
            }
        }
    }
}

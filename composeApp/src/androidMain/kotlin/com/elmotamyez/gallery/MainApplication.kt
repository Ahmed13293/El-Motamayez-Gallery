package com.elmotamyez.gallery

import android.app.Application
import com.elmotamyez.gallery.di.appModule
import com.elmotamyez.gallery.util.ApplicationContextHolder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.context = this
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}

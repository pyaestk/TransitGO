package com.bangkoktransit.app

import android.app.Application
import com.bangkoktransit.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
class TransitGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TransitGoApplication)
            modules(appModule)
        }
    }
}

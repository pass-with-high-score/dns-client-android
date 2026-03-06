package app.pwhs.dnsclient

import android.app.Application
import app.pwhs.dnsclient.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class DnsClientApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Koin DI
        startKoin {
            androidLogger()
            androidContext(this@DnsClientApp)
            modules(appModule)
        }
    }
}

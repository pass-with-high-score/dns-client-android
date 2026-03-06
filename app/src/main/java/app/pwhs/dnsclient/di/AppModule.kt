package app.pwhs.dnsclient.di

import androidx.room.Room
import app.pwhs.dnsclient.core.dns.DohResolver
import app.pwhs.dnsclient.data.local.DnsDatabase
import app.pwhs.dnsclient.data.preferences.DnsPreferences
import app.pwhs.dnsclient.ui.diagnostics.DiagnosticsViewModel
import app.pwhs.dnsclient.ui.home.HomeViewModel
import app.pwhs.dnsclient.ui.logs.QueryLogViewModel
import app.pwhs.dnsclient.ui.servers.ServerListViewModel
import app.pwhs.dnsclient.ui.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // --- Network ---
    single {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
            engine {
                https {
                    // Trust default certificates
                }
            }
        }
    }

    single { DohResolver(get()) }

    // --- Database ---
    single {
        Room.databaseBuilder(
            androidContext(),
            DnsDatabase::class.java,
            DnsDatabase.DATABASE_NAME
        ).build()
    }

    single { get<DnsDatabase>().dnsQueryLogDao() }

    // --- Preferences ---
    single { DnsPreferences(androidContext()) }

    // --- ViewModels ---
    viewModel { HomeViewModel(get()) }
    viewModel { ServerListViewModel(get()) }
    viewModel { QueryLogViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { DiagnosticsViewModel(get(), get()) }
}

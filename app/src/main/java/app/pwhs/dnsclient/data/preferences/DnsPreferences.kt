package app.pwhs.dnsclient.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dns_preferences")

class DnsPreferences(private val context: Context) {

    companion object {
        private val KEY_SELECTED_SERVER = stringPreferencesKey("selected_server_key")
        private val KEY_NEXTDNS_PROFILE_ID = stringPreferencesKey("nextdns_profile_id")
        private val KEY_CUSTOM_DOH_URL = stringPreferencesKey("custom_doh_url")
        private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect_on_boot")
        private val KEY_LOG_QUERIES = booleanPreferencesKey("log_queries")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val selectedServerKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_SERVER] ?: "Cloudflare"
    }

    val nextDnsProfileId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NEXTDNS_PROFILE_ID] ?: ""
    }

    val customDohUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_DOH_URL] ?: ""
    }

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CONNECT] ?: false
    }

    val logQueries: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOG_QUERIES] ?: true
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_ENABLED] ?: false
    }

    suspend fun setSelectedServer(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_SERVER] = key
        }
    }

    suspend fun setNextDnsProfileId(profileId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NEXTDNS_PROFILE_ID] = profileId
        }
    }

    suspend fun setCustomDohUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_DOH_URL] = url
        }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_CONNECT] = enabled
        }
    }

    suspend fun setLogQueries(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOG_QUERIES] = enabled
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
        }
    }
}

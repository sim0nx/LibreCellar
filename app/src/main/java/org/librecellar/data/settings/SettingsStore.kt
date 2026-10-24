package org.librecellar.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private object Keys {
    val backendUrl = stringPreferencesKey("server_backend_url")
    val username = stringPreferencesKey("server_username")
    val sslVerify = booleanPreferencesKey("server_ssl_verify")
    val priceTierMarking = booleanPreferencesKey("wine_price_tier_marking")
    val hideReadOnlyShared = booleanPreferencesKey("wine_filter_hide_read_only_shared")
    val hideWritableShared = booleanPreferencesKey("wine_filter_hide_writable_shared")
    val customGrapes = stringSetPreferencesKey("custom_wine_grapes")
    /** Web-login token of the backend, encrypted with an Android Keystore key (see [TokenCipher]) */
    val encryptedToken = stringPreferencesKey("session_token_encrypted")
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "librecellar_settings"
)

data class AppSettings(
    val backendUrl: String,
    val username: String,
    val sslVerify: Boolean,
    val priceTierMarking: Boolean,
    val hideReadOnlyShared: Boolean,
    val hideWritableShared: Boolean,
    val customGrapes: Set<String>,
    val encryptedToken: String?
)

/** App settings, persisted with Jetpack DataStore. */
class SettingsStore(
    context: Context,
    // The app-wide store; tests pass their own instance
    private val store: DataStore<Preferences> = context.settingsDataStore
) {

    suspend fun load(): AppSettings {
        val p = store.data.first()
        return AppSettings(
            backendUrl = p[Keys.backendUrl] ?: DEFAULT_BACKEND_URL,
            username = p[Keys.username] ?: "",
            sslVerify = p[Keys.sslVerify] ?: true,
            priceTierMarking = p[Keys.priceTierMarking] ?: true,
            hideReadOnlyShared = p[Keys.hideReadOnlyShared] ?: false,
            hideWritableShared = p[Keys.hideWritableShared] ?: false,
            customGrapes = p[Keys.customGrapes] ?: emptySet(),
            encryptedToken = p[Keys.encryptedToken]
        )
    }

    suspend fun saveServer(url: String, username: String, sslVerify: Boolean) {
        store.edit {
            it[Keys.backendUrl] = url
            it[Keys.username] = username
            it[Keys.sslVerify] = sslVerify
        }
    }

    suspend fun saveEncryptedToken(encrypted: String?) {
        store.edit { if (encrypted == null) it.remove(Keys.encryptedToken) else it[Keys.encryptedToken] = encrypted }
    }

    suspend fun setPriceTierMarking(enabled: Boolean) = store.edit { it[Keys.priceTierMarking] = enabled }

    suspend fun setHideReadOnlyShared(hide: Boolean) = store.edit { it[Keys.hideReadOnlyShared] = hide }

    suspend fun setHideWritableShared(hide: Boolean) = store.edit { it[Keys.hideWritableShared] = hide }

    suspend fun setCustomGrapes(grapes: Set<String>) = store.edit { it[Keys.customGrapes] = grapes }

    companion object {
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000/"
    }
}

package org.librecellar

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.librecellar.data.settings.LegacyPasswords
import org.librecellar.data.settings.SettingsStore
import org.librecellar.data.settings.settingsMigrations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Same SDK as the other Robolectric tests (newer SDKs need extra JVM flags on recent JDKs)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsMigrationTest {

    @Test
    fun `settings move from SharedPreferences to DataStore and the password is removed`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacy = context.getSharedPreferences("librecellar_prefs", Context.MODE_PRIVATE)
        legacy.edit()
            .putString("server_backend_url", "https://cellar.example.org/")
            .putString("server_username", "alice")
            .putString("server_password", "plain-text-secret")
            .putBoolean("server_ssl_verify", false)
            .putBoolean("wine_price_tier_marking", false)
            .putStringSet("custom_wine_grapes", setOf("Rkatsiteli"))
            .commit()

        // A fresh store with the app's migrations (the app-wide one may already exist in this JVM)
        val store = PreferenceDataStoreFactory.create(
            migrations = settingsMigrations(context),
            produceFile = { File(context.filesDir, "datastore/migration-test.preferences_pb") }
        )
        val settings = SettingsStore(context, store).load()
        assertEquals("https://cellar.example.org/", settings.backendUrl)
        assertEquals("alice", settings.username)
        assertFalse(settings.sslVerify)
        assertFalse(settings.priceTierMarking)
        assertEquals(setOf("Rkatsiteli"), settings.customGrapes)
        assertNull(settings.encryptedToken)

        // The password never reaches DataStore; it is handed over once for a login, then deleted
        assertEquals("plain-text-secret", LegacyPasswords.take(context))
        assertNull(LegacyPasswords.take(context))
        assertFalse(legacy.contains("server_password"))
        assertFalse(legacy.contains("server_backend_url")) // migrated keys are removed too
    }
}

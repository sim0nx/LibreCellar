package org.librecellar.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import org.librecellar.data.LocalNetwork
import org.librecellar.data.api.WineApiService
import org.librecellar.data.model.AssignSlotRequest
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.CollectionInfo
import org.librecellar.data.model.DefaultCollectionUpdate
import org.librecellar.data.model.Grape
import org.librecellar.data.model.LoginRequest
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import org.librecellar.data.settings.AppSettings
import org.librecellar.data.settings.SettingsStore
import org.librecellar.data.settings.TokenCipher
import org.librecellar.ui.components.WineImageHelper
import org.librecellar.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import org.librecellar.data.api.ApiJson
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val message: String,
    val statusCode: Int? = null
)

class WineRepository(private val context: Context) {

    private val settingsStore = SettingsStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // DataStore is asynchronous; the settings are read once at startup so every flow below
    // starts with the stored values (a tiny file, read before the first screen is drawn)
    private val initialSettings: AppSettings = runBlocking(Dispatchers.IO) { settingsStore.load() }

    private val _backendUrl = MutableStateFlow(initialSettings.backendUrl)
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _username = MutableStateFlow(initialSettings.username)
    val username: StateFlow<String> = _username.asStateFlow()

    // By default, SSL verification is ON (true)
    private val _isSslVerificationEnabled = MutableStateFlow(initialSettings.sslVerify)
    val isSslVerificationEnabled: StateFlow<Boolean> = _isSslVerificationEnabled.asStateFlow()

    /**
     * Login token of the backend; the password itself is never stored. Persisted encrypted
     * with an Android Keystore key and sent as `Authorization: Bearer <token>`.
     */
    @Volatile
    private var sessionToken: String? = initialSettings.encryptedToken?.let(TokenCipher::decrypt)

    private val _isLoggedIn = MutableStateFlow(sessionToken != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isBackendOnline = MutableStateFlow(false)
    val isBackendOnline: StateFlow<Boolean> = _isBackendOnline.asStateFlow()

    // State flows for UI (Initialized empty with no sample data)
    private val _wines = MutableStateFlow<List<Wine>>(emptyList())
    val wines: StateFlow<List<Wine>> = _wines.asStateFlow()

    private val _locations = MutableStateFlow<List<CellarLocation>>(emptyList())
    val locations: StateFlow<List<CellarLocation>> = _locations.asStateFlow()

    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    // Pagination & Pull-down Sync State for Wines (Page limit 20, backend reference)
    private val _wineCurrentPage = MutableStateFlow(1)
    val wineCurrentPage: StateFlow<Int> = _wineCurrentPage.asStateFlow()

    private val _hasMoreWines = MutableStateFlow(true)
    val hasMoreWines: StateFlow<Boolean> = _hasMoreWines.asStateFlow()

    private val _isLoadingMoreWines = MutableStateFlow(false)
    val isLoadingMoreWines: StateFlow<Boolean> = _isLoadingMoreWines.asStateFlow()

    private val _isWinesRefreshing = MutableStateFlow(false)
    val isWinesRefreshing: StateFlow<Boolean> = _isWinesRefreshing.asStateFlow()

    // Pagination & Pull-down Sync State for Suppliers/Merchants (Page limit 20, backend reference)
    private val _supplierCurrentPage = MutableStateFlow(1)
    val supplierCurrentPage: StateFlow<Int> = _supplierCurrentPage.asStateFlow()

    private val _hasMoreSuppliers = MutableStateFlow(true)
    val hasMoreSuppliers: StateFlow<Boolean> = _hasMoreSuppliers.asStateFlow()

    private val _isLoadingMoreSuppliers = MutableStateFlow(false)
    val isLoadingMoreSuppliers: StateFlow<Boolean> = _isLoadingMoreSuppliers.asStateFlow()

    private val _isSuppliersRefreshing = MutableStateFlow(false)
    val isSuppliersRefreshing: StateFlow<Boolean> = _isSuppliersRefreshing.asStateFlow()

    // Pull-down Sync State for Cellar Locations / Racks (limit 20, backend reference)
    private val _isLocationsRefreshing = MutableStateFlow(false)
    val isLocationsRefreshing: StateFlow<Boolean> = _isLocationsRefreshing.asStateFlow()

    // Price Tier Marking setting (enabled by default)
    private val _isPriceTierMarkingEnabled = MutableStateFlow(initialSettings.priceTierMarking)
    val isPriceTierMarkingEnabled: StateFlow<Boolean> = _isPriceTierMarkingEnabled.asStateFlow()

    suspend fun updatePriceTierMarkingEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        settingsStore.setPriceTierMarking(enabled)
        _isPriceTierMarkingEnabled.value = enabled
    }

    // Collections visible to the logged-in user and where their new entries go (multi-user backend)
    private val _collections = MutableStateFlow<List<CollectionInfo>>(emptyList())
    val collections: StateFlow<List<CollectionInfo>> = _collections.asStateFlow()

    private val _defaultCollection = MutableStateFlow<String?>(null)
    val defaultCollection: StateFlow<String?> = _defaultCollection.asStateFlow()

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    /** Loads the user's collections and default collection; a no-op on single-user backends. */
    suspend fun refreshCollections() = withContext(Dispatchers.IO) {
        try {
            val me = apiService.getMe()
            val cols = apiService.getCollections()
            if (me.isSuccessful && cols.isSuccessful) {
                _currentUsername.value = me.body()?.username
                _defaultCollection.value = me.body()?.defaultCollection
                _collections.value = cols.body().orEmpty()
            } else {
                _collections.value = emptyList()
            }
        } catch (e: Exception) {
            Log.w("WineRepository", "Could not load collections", e)
        }
    }

    /** Chooses the collection new entries go to: [owner] username, or null for the user's own. */
    suspend fun updateDefaultCollection(owner: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.setDefaultCollection(DefaultCollectionUpdate(owner))
            resp.body()?.takeIf { resp.isSuccessful }?.let {
                _defaultCollection.value = it.defaultCollection
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w("WineRepository", "Could not set default collection", e)
        }
        false
    }

    // Wine list filters for entries of collections shared with the user (persisted across restarts)
    private val _hideReadOnlyShared = MutableStateFlow(initialSettings.hideReadOnlyShared)
    val hideReadOnlyShared: StateFlow<Boolean> = _hideReadOnlyShared.asStateFlow()

    private val _hideWritableShared = MutableStateFlow(initialSettings.hideWritableShared)
    val hideWritableShared: StateFlow<Boolean> = _hideWritableShared.asStateFlow()

    suspend fun updateHideReadOnlyShared(hide: Boolean) = withContext(Dispatchers.IO) {
        settingsStore.setHideReadOnlyShared(hide)
        _hideReadOnlyShared.value = hide
    }

    suspend fun updateHideWritableShared(hide: Boolean) = withContext(Dispatchers.IO) {
        settingsStore.setHideWritableShared(hide)
        _hideWritableShared.value = hide
    }

    fun suggestPriceTier(price: Double?): org.librecellar.data.model.WinePriceTier {
        return org.librecellar.data.model.WinePriceTier.calculateDynamicTier(price, _wines.value)
    }

    // --- Known Varietals / Grapes Management & Autocomplete ---
    private val _grapes = MutableStateFlow<List<Grape>>(emptyList())
    /** The user's grape list on the server (empty until loaded). */
    val grapes: StateFlow<List<Grape>> = _grapes.asStateFlow()

    /** Loads the grape list; false if the server could not be reached or refused. */
    suspend fun reloadGrapes(): Boolean = withContext(Dispatchers.IO) {
        val resp = try {
            apiService.getGrapes()
        } catch (e: IOException) {
            return@withContext false
        }
        if (!resp.isSuccessful) return@withContext false
        _grapes.value = resp.body().orEmpty()
        _knownGrapes.value = computeAllKnownGrapes()
        true
    }

    /** Replaces the grape list; the server refuses to remove grapes still used by wines. */
    suspend fun saveGrapes(grapes: List<Grape>): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Saving grapes", setOf(DataSet.GRAPES)) { apiService.saveGrapes(grapes) }
    }

    /** Back to the defaults; own grapes still used by wines are kept by the server. */
    suspend fun resetGrapes(): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Resetting grapes", setOf(DataSet.GRAPES)) { apiService.resetGrapes() }
    }

    // Grapes the user typed in, kept in memory and persisted in the background
    @Volatile
    private var customGrapes: Set<String> = initialSettings.customGrapes

    fun computeAllKnownGrapes(): List<String> {
        val fromWines = _wines.value.flatMap { it.varietalList }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        val custom = customGrapes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        // The user's list from the server; the built-in defaults until it is loaded
        val listed = _grapes.value.map { it.name }.ifEmpty { DEFAULT_KNOWN_GRAPES }
        val defaults = listed
            .filter { def ->
                fromWines.none { it.equals(def, ignoreCase = true) } &&
                custom.none { it.equals(def, ignoreCase = true) }
            }

        // Put cellar grapes first, then custom grapes, then standard varieties
        return (fromWines.sortedWith(String.CASE_INSENSITIVE_ORDER) +
                custom.filter { c -> fromWines.none { it.equals(c, ignoreCase = true) } }.sortedWith(String.CASE_INSENSITIVE_ORDER) +
                defaults.sortedWith(String.CASE_INSENSITIVE_ORDER))
    }

    private val _knownGrapes = MutableStateFlow<List<String>>(computeAllKnownGrapes())
    val knownGrapes: StateFlow<List<String>> = _knownGrapes.asStateFlow()

    fun registerGrape(grape: String) {
        val trimmed = grape.trim()
        if (trimmed.isBlank()) return
        if (customGrapes.none { it.equals(trimmed, ignoreCase = true) }) {
            val updated = customGrapes + trimmed
            customGrapes = updated
            scope.launch { settingsStore.setCustomGrapes(updated) }
        }
        _knownGrapes.value = computeAllKnownGrapes()
    }

    fun registerGrapes(grapes: List<String>) {
        grapes.forEach { registerGrape(it) }
    }

    companion object {
        // Backend page size used for all paginated lists
        private const val PAGE_SIZE = 20

        const val LOCAL_NETWORK_HINT =
            "The server is on your local network: allow \"Nearby devices\" for LibreCellar " +
                "(Android settings → Apps → LibreCellar → Permissions)."

        val DEFAULT_KNOWN_GRAPES = listOf(
            "Cabernet Sauvignon", "Merlot", "Pinot Noir", "Chardonnay", "Sauvignon Blanc",
            "Syrah", "Shiraz", "Nebbiolo", "Sangiovese", "Tempranillo", "Riesling",
            "Zinfandel", "Malbec", "Grenache", "Cabernet Franc", "Pinot Grigio",
            "Pinot Gris", "Chenin Blanc", "Viognier", "Gewürztraminer", "Gamay",
            "Petit Verdot", "Carménère", "Barbera", "Corvina", "Glera", "Prosecco",
            "Mourvèdre", "Primitivo", "Torrontés", "Grüner Veltliner", "Albariño",
            "Vermentino", "Sémillon", "Muscat", "Garganega", "Montepulciano", "Nero d'Avola"
        )
    }

    private var apiService: WineApiService = buildRetrofit(
        url = _backendUrl.value,
        sslVerify = _isSslVerificationEnabled.value
    )

    init {
        scope.launch { checkConnectionAndSync() }
    }

    private fun buildRetrofit(url: String, sslVerify: Boolean): WineApiService {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)

        // Send the login token; it is read per request, so logging in or out needs no rebuild
        clientBuilder.addInterceptor { chain ->
            val token = sessionToken
            val request = chain.request()
            val authorized = if (token != null && request.header("Authorization") == null) {
                request.newBuilder().header("Authorization", "Bearer $token").build()
            } else request
            val response = chain.proceed(authorized)
            if (response.code == 401 && token != null && token == sessionToken) {
                // Token expired or revoked on the server (e.g. password reset): log in again
                scope.launch { forgetToken() }
            }
            response
        }

        // HTTP logging in debug builds only, and never with bodies or credentials
        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                }
            )
        }

        // Handle SSL certificate validation toggle
        if (!sslVerify) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.socketFactory

                clientBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                Log.e("WineRepository", "Failed to configure permissive SSL", e)
            }
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WineApiService::class.java)
    }

    private suspend fun storeToken(token: String?) {
        sessionToken = token
        _isLoggedIn.value = token != null
        settingsStore.saveEncryptedToken(token?.let(TokenCipher::encrypt))
    }

    private suspend fun forgetToken() {
        storeToken(null)
        _isBackendOnline.value = false
    }

    /** Logs in with the password (which is not stored) and keeps the returned token. */
    private suspend fun obtainToken(user: String, pass: String): ConnectionTestResult? {
        return try {
            val resp = apiService.login(LoginRequest(user, pass))
            val token = resp.body()?.token
            when {
                resp.isSuccessful && token != null -> {
                    storeToken(token)
                    null
                }
                resp.code() == 401 -> ConnectionTestResult(false, "Wrong username or password.", 401)
                resp.code() == 404 -> ConnectionTestResult(
                    false,
                    "This server does not support login (POST /api/auth/login). Update the backend.",
                    404
                )
                else -> ConnectionTestResult(false, "Login failed (HTTP ${resp.code()}).", resp.code())
            }
        } catch (e: Exception) {
            Log.d("WineRepository", "Login failed: ${e.message}")
            ConnectionTestResult(false, "Could not reach server: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Saves the server settings and logs in. With an empty [newPass] the existing login is kept
     * (only possible for the same server and user); otherwise the password is exchanged for a
     * token and then discarded.
     */
    suspend fun updateSettings(
        newUrl: String,
        newUser: String,
        newPass: String,
        newSslVerify: Boolean
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val validUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        val user = newUser.trim()
        val sameAccount = validUrl == _backendUrl.value && user == _username.value

        settingsStore.saveServer(validUrl, user, newSslVerify)
        _backendUrl.value = validUrl
        _username.value = user
        _isSslVerificationEnabled.value = newSslVerify
        apiService = buildRetrofit(validUrl, newSslVerify)

        if (newPass.isNotEmpty()) {
            if (sessionToken != null) logoutQuietly()
            obtainToken(user, newPass)?.let { failure ->
                _isBackendOnline.value = false
                return@withContext failure
            }
        } else if (!sameAccount || sessionToken == null) {
            if (!sameAccount) storeToken(null)
            _isBackendOnline.value = false
            return@withContext ConnectionTestResult(false, "Enter your password to log in.")
        }
        testConnection()
    }

    /** Ends the login on the server and on this device. */
    suspend fun logout() = withContext(Dispatchers.IO) {
        logoutQuietly()
        _isBackendOnline.value = false
        _wines.value = emptyList()
        _locations.value = emptyList()
        _suppliers.value = emptyList()
        _collections.value = emptyList()
        _currentUsername.value = null
    }

    private suspend fun logoutQuietly() {
        try {
            apiService.logout()
        } catch (e: Exception) {
            Log.d("WineRepository", "Server logout failed: ${e.message}")
        }
        storeToken(null)
    }

    suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            // 1. Verify health check endpoint
            val healthResponse = apiService.checkHealth()
            if (!healthResponse.isSuccessful) {
                _isBackendOnline.value = false
                val msg = "Server health check failed (HTTP ${healthResponse.code()}). Check server URL."
                return@withContext ConnectionTestResult(isSuccess = false, message = msg, statusCode = healthResponse.code())
            }

            if (sessionToken == null) {
                _isBackendOnline.value = false
                return@withContext ConnectionTestResult(false, "Server reachable. Log in with your password.")
            }

            // 2. CRITICAL: Verify protected endpoint (get merchants / suppliers) to test authentication
            val protectedResponse = apiService.getSuppliers(page = 1, limit = 20)
            if (!protectedResponse.isSuccessful) {
                _isBackendOnline.value = false
                val msg = when (protectedResponse.code()) {
                    401 -> "401 Unauthorized: Your login expired or was revoked. Log in again with your password."
                    403 -> "403 Forbidden: Access denied to protected merchant/supplier endpoint."
                    404 -> "404 Not Found: Protected endpoint '/api/suppliers' not found on server."
                    else -> "Protected endpoint returned HTTP ${protectedResponse.code()}."
                }
                return@withContext ConnectionTestResult(isSuccess = false, message = msg, statusCode = protectedResponse.code())
            }

            // 3. Both health and protected endpoints succeeded! Mark backend as online
            _isBackendOnline.value = true

            // 4. Load everything from the server, which manages all data
            syncFromBackend()
            refreshCollections()

            val syncMsg = "Loaded ${_wines.value.size} wines, ${_locations.value.size} racks, " +
                    "${_suppliers.value.size} merchants from the server."

            ConnectionTestResult(
                isSuccess = true,
                message = "Connected & authenticated successfully! $syncMsg",
                statusCode = 200
            )
        } catch (e: Exception) {
            _isBackendOnline.value = false
            val errorMsg = e.localizedMessage ?: e.message ?: "Connection failed"
            Log.d("WineRepository", "Connection test failed: $errorMsg")
            ConnectionTestResult(
                isSuccess = false,
                message = if (errorMsg.contains("SSLHandshakeException", ignoreCase = true)) {
                    "SSL Handshake failed: Self-signed certificate? Try turning off SSL Verification in settings."
                } else if (LocalNetwork.isBlocked(context, _backendUrl.value)) {
                    LOCAL_NETWORK_HINT
                } else {
                    "Could not reach server: $errorMsg"
                }
            )
        }
    }

    suspend fun checkConnectionAndSync(): Boolean = withContext(Dispatchers.IO) {
        val result = testConnection()
        result.isSuccess
    }

    /**
     * Fetches all wines using pagination with page limit of 20
     */
    suspend fun fetchAllWinesPaginated(limit: Int = 20): List<Wine> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Wine>()
        var page = 1
        while (true) {
            try {
                val resp = apiService.getWines(page = page, limit = limit)
                if (!resp.isSuccessful) break
                val items = resp.body().orEmpty()
                results.addAll(items)
                if (items.size < limit) break
                page++
            } catch (e: Exception) {
                Log.e("WineRepository", "Error fetching wines page $page", e)
                break
            }
        }
        results
    }

    /**
     * Fetches all cellar locations/racks using pagination with page limit of 20
     */
    suspend fun fetchAllLocationsPaginated(limit: Int = 20): List<CellarLocation> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CellarLocation>()
        var page = 1
        while (true) {
            try {
                val resp = apiService.getCellarLocations(page = page, limit = limit)
                if (!resp.isSuccessful) break
                val items = resp.body().orEmpty()
                results.addAll(items)
                if (items.size < limit) break
                page++
            } catch (e: Exception) {
                Log.e("WineRepository", "Error fetching locations page $page", e)
                break
            }
        }
        results
    }

    /**
     * Fetches all suppliers/merchants using pagination with page limit of 20
     */
    suspend fun fetchAllSuppliersPaginated(limit: Int = 20): List<Supplier> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Supplier>()
        var page = 1
        while (true) {
            try {
                val resp = apiService.getSuppliers(page = page, limit = limit)
                if (!resp.isSuccessful) break
                val items = resp.body().orEmpty()
                results.addAll(items)
                if (items.size < limit) break
                page++
            } catch (e: Exception) {
                Log.e("WineRepository", "Error fetching suppliers page $page", e)
                break
            }
        }
        results
    }

    /** Loads every wine (not only the pages scrolled through), e.g. for statistics. */
    suspend fun loadAllWines() = withContext(Dispatchers.IO) {
        val load = loadPages(Int.MAX_VALUE) { apiService.getWines(page = it, limit = PAGE_SIZE) }
            ?: return@withContext
        _wines.value = load.items
        _wineCurrentPage.value = load.pages
        _hasMoreWines.value = false
        _knownGrapes.value = computeAllKnownGrapes()
    }

    private suspend fun syncFromBackend() = withContext(Dispatchers.IO) {
        try {
            val wines = fetchAllWinesPaginated(limit = PAGE_SIZE)
            _wines.value = wines
            _wineCurrentPage.value = loadedPages(1, wines.size)
            _hasMoreWines.value = false
            val locations = fetchAllLocationsPaginated(limit = PAGE_SIZE)
            _locations.value = locations
            val suppliers = fetchAllSuppliersPaginated(limit = PAGE_SIZE)
            _suppliers.value = suppliers
            _supplierCurrentPage.value = loadedPages(1, suppliers.size)
            _hasMoreSuppliers.value = false
            reloadGrapes()
            _knownGrapes.value = computeAllKnownGrapes()
        } catch (e: Exception) {
            Log.e("WineRepository", "Error syncing with backend", e)
        }
    }

    /**
     * Pull-down sync for Wines: resets to page 1, queries backend as source of truth.
     */
    suspend fun refreshWines() = withContext(Dispatchers.IO) {
        _isWinesRefreshing.value = true
        try {
            _wineCurrentPage.value = 1
            if (_isBackendOnline.value) {
                val resp = apiService.getWines(page = 1, limit = 20)
                if (resp.isSuccessful) {
                    val items = resp.body().orEmpty()
                    _wines.value = items
                    _hasMoreWines.value = (items.size >= 20)
                    _knownGrapes.value = computeAllKnownGrapes()
                }
            } else {
                // If offline, attempt quick connection check and refresh
                val health = apiService.checkHealth()
                if (health.isSuccessful) {
                    val resp = apiService.getWines(page = 1, limit = 20)
                    if (resp.isSuccessful) {
                        _isBackendOnline.value = true
                        val items = resp.body().orEmpty()
                        _wines.value = items
                        _hasMoreWines.value = (items.size >= 20)
                        _knownGrapes.value = computeAllKnownGrapes()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WineRepository", "Error refreshing wines", e)
        } finally {
            _isWinesRefreshing.value = false
        }
    }

    /**
     * Loads next batch of 20 wines from backend as user scrolls.
     */
    suspend fun loadNextWinePage() = withContext(Dispatchers.IO) {
        if (_isLoadingMoreWines.value || !_hasMoreWines.value) return@withContext
        _isLoadingMoreWines.value = true
        try {
            val nextPage = _wineCurrentPage.value + 1
            if (_isBackendOnline.value) {
                val resp = apiService.getWines(page = nextPage, limit = 20)
                if (resp.isSuccessful) {
                    val newBatch = resp.body().orEmpty()
                    if (newBatch.isNotEmpty()) {
                        val existingIds = _wines.value.map { it.id }.toSet()
                        _wines.value = _wines.value + newBatch.filter { it.id !in existingIds }
                        _wineCurrentPage.value = nextPage
                        _hasMoreWines.value = (newBatch.size >= 20)
                    } else {
                        _hasMoreWines.value = false
                    }
                } else {
                    _hasMoreWines.value = false
                }
            } else {
                _hasMoreWines.value = false
            }
        } catch (e: Exception) {
            Log.e("WineRepository", "Error loading next wine page", e)
        } finally {
            _isLoadingMoreWines.value = false
        }
    }

    /**
     * Pull-down sync for Merchants/Suppliers: resets to page 1, queries backend as source of truth.
     */
    suspend fun refreshSuppliers() = withContext(Dispatchers.IO) {
        _isSuppliersRefreshing.value = true
        try {
            _supplierCurrentPage.value = 1
            if (_isBackendOnline.value) {
                val resp = apiService.getSuppliers(page = 1, limit = 20)
                if (resp.isSuccessful) {
                    val items = resp.body().orEmpty()
                    _suppliers.value = items
                    _hasMoreSuppliers.value = (items.size >= 20)
                }
            } else {
                val health = apiService.checkHealth()
                if (health.isSuccessful) {
                    val resp = apiService.getSuppliers(page = 1, limit = 20)
                    if (resp.isSuccessful) {
                        _isBackendOnline.value = true
                        val items = resp.body().orEmpty()
                        _suppliers.value = items
                        _hasMoreSuppliers.value = (items.size >= 20)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WineRepository", "Error refreshing suppliers", e)
        } finally {
            _isSuppliersRefreshing.value = false
        }
    }

    /**
     * Loads next batch of 20 suppliers from backend as user scrolls.
     */
    suspend fun loadNextSupplierPage() = withContext(Dispatchers.IO) {
        if (_isLoadingMoreSuppliers.value || !_hasMoreSuppliers.value) return@withContext
        _isLoadingMoreSuppliers.value = true
        try {
            val nextPage = _supplierCurrentPage.value + 1
            if (_isBackendOnline.value) {
                val resp = apiService.getSuppliers(page = nextPage, limit = 20)
                if (resp.isSuccessful) {
                    val newBatch = resp.body().orEmpty()
                    if (newBatch.isNotEmpty()) {
                        val existingIds = _suppliers.value.map { it.id }.toSet()
                        _suppliers.value = _suppliers.value + newBatch.filter { it.id !in existingIds }
                        _supplierCurrentPage.value = nextPage
                        _hasMoreSuppliers.value = (newBatch.size >= 20)
                    } else {
                        _hasMoreSuppliers.value = false
                    }
                } else {
                    _hasMoreSuppliers.value = false
                }
            } else {
                _hasMoreSuppliers.value = false
            }
        } catch (e: Exception) {
            Log.e("WineRepository", "Error loading next supplier page", e)
        } finally {
            _isLoadingMoreSuppliers.value = false
        }
    }

    /**
     * Pull-down sync for Cellar Locations / Racks: queries backend as source of truth.
     */
    suspend fun refreshLocations() = withContext(Dispatchers.IO) {
        _isLocationsRefreshing.value = true
        try {
            if (_isBackendOnline.value) {
                val resp = apiService.getCellarLocations(page = 1, limit = 20)
                if (resp.isSuccessful) {
                    val items = resp.body().orEmpty()
                    _locations.value = items
                }
            } else {
                val health = apiService.checkHealth()
                if (health.isSuccessful) {
                    _isBackendOnline.value = true
                    val resp = apiService.getCellarLocations(page = 1, limit = 20)
                    if (resp.isSuccessful) {
                        _locations.value = resp.body().orEmpty()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WineRepository", "Error refreshing cellar locations", e)
        } finally {
            _isLocationsRefreshing.value = false
        }
    }

    /**
     * Uploads a bottle picture to the backend API.
     * The backend stores it in the authenticated user's collection, renamed to UUIDv7.<ext>.
     * Only the filename is returned and saved in backend DB. Throws if the upload fails.
     */
    suspend fun uploadBottleImage(pathOrUri: String): String = withContext(Dispatchers.IO) {
        if (pathOrUri.isBlank() || WineImageHelper.isPreset(pathOrUri)) {
            return@withContext pathOrUri
        }
        val sourceFile = if (pathOrUri.startsWith("content://")) {
            val (cachedFile, _) = WineImageHelper.saveUriToLocalCache(context, pathOrUri.toUri())
            cachedFile
        } else {
            val f = File(pathOrUri)
            if (f.exists()) f else WineImageHelper.getCachedImageFile(context, pathOrUri)
        }

        if (sourceFile == null || !sourceFile.exists()) {
            return@withContext pathOrUri
        }

        WineImageHelper.uploadImageToBackend(
            context = context,
            apiService = apiService,
            sourceFile = sourceFile
        )
    }

    /**
     * Requests a bottle picture from the backend API if not available in local cache.
     * When requested, caches it into the Android device's local bottle_cache folder.
     */
    suspend fun getOrFetchCachedImage(filename: String): File? = withContext(Dispatchers.IO) {
        val cached = WineImageHelper.getCachedImageFile(context, filename)
        if (cached != null && cached.exists()) {
            return@withContext cached
        }
        if (WineImageHelper.isPreset(filename) || filename.isBlank()) {
            return@withContext null
        }
        WineImageHelper.fetchAndCacheRemoteImage(
            context = context,
            filename = filename,
            apiService = apiService
        )
    }

    // --- Write operations ---
    //
    // The backend manages all data. Every operation only sends its REST request and, once the
    // server accepted it, reloads the affected lists from the server; nothing is changed locally.
    // Each returns whether the server accepted it; failures are reported via [operationErrors].

    private enum class DataSet { WINES, LOCATIONS, SUPPLIERS, GRAPES }

    private val _operationErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val operationErrors: SharedFlow<String> = _operationErrors.asSharedFlow()

    /** Shows [message] like a failed operation (e.g. a declined permission). */
    fun reportProblem(message: String) {
        _operationErrors.tryEmit(message)
    }

    private suspend fun <T> sendToBackend(
        action: String,
        reload: Set<DataSet>,
        call: suspend () -> Response<T>
    ): Boolean {
        val ok = try {
            val resp = call()
            if (!resp.isSuccessful) {
                _operationErrors.tryEmit("$action failed: ${describeError(resp)}")
            }
            resp.isSuccessful
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w("WineRepository", "$action failed", e)
            _isBackendOnline.value = false
            _operationErrors.tryEmit("$action failed: server not reachable")
            false
        } catch (e: Exception) {
            // A problem in the app (e.g. a response it cannot read), not a connection problem:
            // the server stays online and the actual error is shown
            Log.e("WineRepository", "$action failed", e)
            _operationErrors.tryEmit("$action failed: ${e.message ?: e.javaClass.simpleName}")
            false
        }
        if (ok) reloadFromBackend(reload)
        return ok
    }

    private fun describeError(resp: Response<*>): String {
        val detail = try {
            resp.errorBody()?.string()?.let { JSONObject(it).optString("detail") }
        } catch (e: Exception) {
            null
        }
        return detail?.takeIf { it.isNotBlank() } ?: "HTTP ${resp.code()}"
    }

    private suspend fun reloadFromBackend(sets: Set<DataSet>) {
        if (DataSet.WINES in sets) reloadWines()
        if (DataSet.LOCATIONS in sets) {
            _locations.value = fetchAllLocationsPaginated(limit = PAGE_SIZE)
        }
        if (DataSet.SUPPLIERS in sets) reloadSuppliers()
        if (DataSet.GRAPES in sets) reloadGrapes()
    }

    private data class PageLoad<T>(val items: List<T>, val pages: Int, val hasMore: Boolean)

    /** Loads pages 1..[pages] again, so the lists keep what the user already scrolled through. */
    private suspend fun <T> loadPages(pages: Int, fetch: suspend (Int) -> Response<List<T>>): PageLoad<T>? {
        val items = mutableListOf<T>()
        for (page in 1..pages) {
            val resp = try {
                fetch(page)
            } catch (e: Exception) {
                Log.w("WineRepository", "Reload of page $page failed", e)
                return null
            }
            if (!resp.isSuccessful) return null
            val batch = resp.body().orEmpty()
            items += batch
            if (batch.size < PAGE_SIZE) return PageLoad(items, page, hasMore = false)
        }
        return PageLoad(items, pages, hasMore = true)
    }

    private fun loadedPages(currentPage: Int, loadedItems: Int): Int =
        maxOf(1, currentPage, (loadedItems + PAGE_SIZE - 1) / PAGE_SIZE)

    private suspend fun reloadWines() {
        val load = loadPages(loadedPages(_wineCurrentPage.value, _wines.value.size)) {
            apiService.getWines(page = it, limit = PAGE_SIZE)
        } ?: return
        _wines.value = load.items
        _wineCurrentPage.value = load.pages
        _hasMoreWines.value = load.hasMore
        _knownGrapes.value = computeAllKnownGrapes()
    }

    private suspend fun reloadSuppliers() {
        val load = loadPages(loadedPages(_supplierCurrentPage.value, _suppliers.value.size)) {
            apiService.getSuppliers(page = it, limit = PAGE_SIZE)
        } ?: return
        _suppliers.value = load.items
        _supplierCurrentPage.value = load.pages
        _hasMoreSuppliers.value = load.hasMore
    }

    /** Uploads a picture that still refers to a file on the device; null if that failed. */
    /** Uploads pictures that still refer to a file on the device; null if that failed. */
    private suspend fun withUploadedPhoto(wine: Wine): Wine? {
        fun isLocal(photo: String?) = !photo.isNullOrBlank() && !WineImageHelper.isPreset(photo) &&
            (photo.startsWith("/") || photo.startsWith("file:") || photo.startsWith("content:"))
        return try {
            wine.copy(
                photoUrl = wine.photoUrl?.let { if (isLocal(it)) uploadBottleImage(it) else it },
                photoBackUrl = wine.photoBackUrl?.let { if (isLocal(it)) uploadBottleImage(it) else it }
            )
        } catch (e: Exception) {
            Log.w("WineRepository", "Picture upload failed", e)
            _operationErrors.tryEmit("Saving wine failed: picture could not be uploaded")
            null
        }
    }

    // --- Wine Operations ---
    suspend fun addWine(wine: Wine): Boolean = withContext(Dispatchers.IO) {
        val toSend = withUploadedPhoto(wine) ?: return@withContext false
        val ok = sendToBackend("Saving wine", setOf(DataSet.WINES)) { apiService.createWine(toSend) }
        if (ok) registerGrapes(wine.varietalList)
        ok
    }

    suspend fun updateWine(wine: Wine): Boolean = withContext(Dispatchers.IO) {
        val toSend = withUploadedPhoto(wine) ?: return@withContext false
        val ok = sendToBackend("Saving wine", setOf(DataSet.WINES)) {
            apiService.updateWine(toSend.id, toSend)
        }
        if (ok) registerGrapes(wine.varietalList)
        ok
    }

    suspend fun deleteWine(id: String): Boolean = withContext(Dispatchers.IO) {
        // The backend also removes the wine's pictures once nothing refers to them
        val wine = _wines.value.find { it.id == id }
        val ok = sendToBackend("Deleting wine", setOf(DataSet.WINES)) { apiService.deleteWine(id) }
        if (ok) listOf(wine?.photoUrl, wine?.photoBackUrl).forEach { WineImageHelper.removeCachedImage(context, it) }
        ok
    }

    suspend fun decrementQuantity(id: String): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Ticking off bottle", setOf(DataSet.WINES)) { apiService.decrementInventory(id) }
    }

    suspend fun incrementQuantity(id: String): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Adding bottle", setOf(DataSet.WINES)) { apiService.incrementInventory(id) }
    }

    /** Places a wine in a rack slot, or takes it out of its rack when [locationId] is null. */
    suspend fun assignSlot(wineId: String, locationId: String?, row: Int?, col: Int?): Boolean =
        withContext(Dispatchers.IO) {
            // The path names a rack; the request body decides (no location = unassign)
            val pathLocation = locationId
                ?: _wines.value.find { it.id == wineId }?.locationId
                ?: "unassigned"
            sendToBackend("Placing bottle", setOf(DataSet.WINES)) {
                apiService.assignSlot(pathLocation, AssignSlotRequest(wineId, locationId, row, col))
            }
        }

    fun findWineByBarcode(barcode: String): Wine? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        // Prefer the user's own bottle, then one of an editable shared collection
        return _wines.value.filter { it.barcode?.trim() == trimmed }
            .minByOrNull { it.collectionAccess.ordinal }
    }

    // --- Cellar Location Operations ---
    suspend fun addLocation(location: CellarLocation): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Saving rack", setOf(DataSet.LOCATIONS)) { apiService.createCellarLocation(location) }
    }

    suspend fun updateLocation(location: CellarLocation): Boolean = withContext(Dispatchers.IO) {
        // Resizing can free slots on the server, so wines are reloaded as well
        sendToBackend("Saving rack", setOf(DataSet.LOCATIONS, DataSet.WINES)) {
            apiService.updateCellarLocation(location.id, location)
        }
    }

    suspend fun deleteLocation(id: String): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Deleting rack", setOf(DataSet.LOCATIONS, DataSet.WINES)) {
            apiService.deleteCellarLocation(id)
        }
    }

    // --- Supplier Operations ---
    suspend fun addSupplier(supplier: Supplier): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Saving merchant", setOf(DataSet.SUPPLIERS)) { apiService.createSupplier(supplier) }
    }

    suspend fun updateSupplier(supplier: Supplier): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Saving merchant", setOf(DataSet.SUPPLIERS)) {
            apiService.updateSupplier(supplier.id, supplier)
        }
    }

    suspend fun deleteSupplier(id: String): Boolean = withContext(Dispatchers.IO) {
        sendToBackend("Deleting merchant", setOf(DataSet.SUPPLIERS)) { apiService.deleteSupplier(id) }
    }
}

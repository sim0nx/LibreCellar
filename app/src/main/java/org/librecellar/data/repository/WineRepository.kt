package org.librecellar.data.repository

import android.content.Context
import android.util.Log
import org.librecellar.data.api.WineApiService
import org.librecellar.data.mock.SampleData
import org.librecellar.data.model.AssignSlotRequest
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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

    private val prefs = context.getSharedPreferences("librecellar_prefs", Context.MODE_PRIVATE)

    // Backend URL settings (default server address)
    private val defaultUrl = "http://10.0.2.2:8000/"

    private val _backendUrl = MutableStateFlow(
        prefs.getString("server_backend_url", null)
            ?: prefs.getString("fastapi_backend_url", defaultUrl)
            ?: defaultUrl
    )
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _username = MutableStateFlow(
        prefs.getString("server_username", null)
            ?: prefs.getString("fastapi_username", "")
            ?: ""
    )
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow(
        prefs.getString("server_password", null)
            ?: prefs.getString("fastapi_password", "")
            ?: ""
    )
    val password: StateFlow<String> = _password.asStateFlow()

    // By default, SSL verification is ON (true)
    private val _isSslVerificationEnabled = MutableStateFlow(
        if (prefs.contains("server_ssl_verify")) prefs.getBoolean("server_ssl_verify", true)
        else prefs.getBoolean("fastapi_ssl_verify", true)
    )
    val isSslVerificationEnabled: StateFlow<Boolean> = _isSslVerificationEnabled.asStateFlow()

    private val _isBackendOnline = MutableStateFlow(false)
    val isBackendOnline: StateFlow<Boolean> = _isBackendOnline.asStateFlow()

    // State flows for UI
    private val _wines = MutableStateFlow<List<Wine>>(SampleData.wines)
    val wines: StateFlow<List<Wine>> = _wines.asStateFlow()

    private val _locations = MutableStateFlow<List<CellarLocation>>(SampleData.locations)
    val locations: StateFlow<List<CellarLocation>> = _locations.asStateFlow()

    private val _suppliers = MutableStateFlow<List<Supplier>>(SampleData.suppliers)
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    // Price Tier Marking setting (enabled by default)
    private val _isPriceTierMarkingEnabled = MutableStateFlow(
        prefs.getBoolean("wine_price_tier_marking", true)
    )
    val isPriceTierMarkingEnabled: StateFlow<Boolean> = _isPriceTierMarkingEnabled.asStateFlow()

    suspend fun updatePriceTierMarkingEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean("wine_price_tier_marking", enabled).apply()
        _isPriceTierMarkingEnabled.value = enabled
    }

    fun suggestPriceTier(price: Double?): org.librecellar.data.model.WinePriceTier {
        return org.librecellar.data.model.WinePriceTier.calculateDynamicTier(price, _wines.value)
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var apiService: WineApiService = buildRetrofit(
        url = _backendUrl.value,
        user = _username.value,
        pass = _password.value,
        sslVerify = _isSslVerificationEnabled.value
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            checkConnectionAndSync()
        }
    }

    private fun buildRetrofit(
        url: String,
        user: String,
        pass: String,
        sslVerify: Boolean
    ): WineApiService {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(logging)

        // Attach Basic Auth header if credentials are provided
        if (user.isNotBlank()) {
            clientBuilder.addInterceptor { chain ->
                val authHeader = Credentials.basic(user.trim(), pass)
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", authHeader)
                    .build()
                chain.proceed(newRequest)
            }
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
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WineApiService::class.java)
    }

    suspend fun updateSettings(
        newUrl: String,
        newUser: String,
        newPass: String,
        newSslVerify: Boolean
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val validUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        prefs.edit()
            .putString("server_backend_url", validUrl)
            .putString("server_username", newUser.trim())
            .putString("server_password", newPass)
            .putBoolean("server_ssl_verify", newSslVerify)
            .apply()

        _backendUrl.value = validUrl
        _username.value = newUser.trim()
        _password.value = newPass
        _isSslVerificationEnabled.value = newSslVerify

        apiService = buildRetrofit(validUrl, newUser.trim(), newPass, newSslVerify)
        testConnection()
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

            // 2. CRITICAL: Verify protected endpoint (get merchants / suppliers) to test authentication
            val protectedResponse = apiService.getSuppliers(page = 1, limit = 20)
            if (!protectedResponse.isSuccessful) {
                _isBackendOnline.value = false
                val msg = when (protectedResponse.code()) {
                    401 -> "401 Unauthorized: Invalid username or password for protected endpoint."
                    403 -> "403 Forbidden: Access denied to protected merchant/supplier endpoint."
                    404 -> "404 Not Found: Protected endpoint '/api/suppliers' not found on server."
                    else -> "Protected endpoint returned HTTP ${protectedResponse.code()}."
                }
                return@withContext ConnectionTestResult(isSuccess = false, message = msg, statusCode = protectedResponse.code())
            }

            // 3. Both health and protected endpoints succeeded! Mark backend as online
            _isBackendOnline.value = true

            // 4. Push all local-only data to the backend server
            val pushResult = pushLocalOnlyDataAndSync()

            val syncMsg = if (pushResult.first + pushResult.second + pushResult.third > 0) {
                "Pushed local data: ${pushResult.first} wines, ${pushResult.second} racks, ${pushResult.third} merchants."
            } else {
                "Local data is synchronized with backend server."
            }

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

    /**
     * Pushes all local-only wines, cellar locations, and merchants to the remote backend,
     * then refreshes local memory with the complete paginated remote dataset.
     * Returns Triple(pushedWines, pushedLocations, pushedSuppliers).
     */
    suspend fun pushLocalOnlyDataAndSync(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch current remote datasets with pagination (limit 20)
            val remoteSuppliers = fetchAllSuppliersPaginated(limit = 20)
            val remoteLocations = fetchAllLocationsPaginated(limit = 20)
            val remoteWines = fetchAllWinesPaginated(limit = 20)

            val remoteSupplierIds = remoteSuppliers.map { it.id }.toSet()
            val remoteSupplierNames = remoteSuppliers.map { it.name.trim().lowercase() }.toSet()

            val remoteLocationIds = remoteLocations.map { it.id }.toSet()
            val remoteLocationNames = remoteLocations.map { it.name.trim().lowercase() }.toSet()

            val remoteWineIds = remoteWines.map { it.id }.toSet()
            val remoteWineKeys = remoteWines.map { "${it.name.trim().lowercase()}_${it.vintage}" }.toSet()

            var pushedSuppliersCount = 0
            var pushedLocationsCount = 0
            var pushedWinesCount = 0

            // 2. Push local suppliers missing on backend
            for (localSup in _suppliers.value) {
                val alreadyExists = localSup.id in remoteSupplierIds || localSup.name.trim().lowercase() in remoteSupplierNames
                if (!alreadyExists) {
                    try {
                        val resp = apiService.createSupplier(localSup)
                        if (resp.isSuccessful) pushedSuppliersCount++
                    } catch (e: Exception) {
                        Log.w("WineRepository", "Error pushing local supplier ${localSup.name}", e)
                    }
                }
            }

            // 3. Push local locations/racks missing on backend
            for (localLoc in _locations.value) {
                val alreadyExists = localLoc.id in remoteLocationIds || localLoc.name.trim().lowercase() in remoteLocationNames
                if (!alreadyExists) {
                    try {
                        val resp = apiService.createCellarLocation(localLoc)
                        if (resp.isSuccessful) pushedLocationsCount++
                    } catch (e: Exception) {
                        Log.w("WineRepository", "Error pushing local location ${localLoc.name}", e)
                    }
                }
            }

            // 4. Push local wines missing on backend
            for (localWine in _wines.value) {
                val wineKey = "${localWine.name.trim().lowercase()}_${localWine.vintage}"
                val alreadyExists = localWine.id in remoteWineIds || wineKey in remoteWineKeys
                if (!alreadyExists) {
                    try {
                        val resp = apiService.createWine(localWine)
                        if (resp.isSuccessful) pushedWinesCount++
                    } catch (e: Exception) {
                        Log.w("WineRepository", "Error pushing local wine ${localWine.name}", e)
                    }
                }
            }

            // 5. Re-fetch complete merged dataset using pagination (limit 20)
            val updatedWines = fetchAllWinesPaginated(limit = 20)
            if (updatedWines.isNotEmpty()) {
                _wines.value = updatedWines
            }
            val updatedLocations = fetchAllLocationsPaginated(limit = 20)
            if (updatedLocations.isNotEmpty()) {
                _locations.value = updatedLocations
            }
            val updatedSuppliers = fetchAllSuppliersPaginated(limit = 20)
            if (updatedSuppliers.isNotEmpty()) {
                _suppliers.value = updatedSuppliers
            }

            Triple(pushedWinesCount, pushedLocationsCount, pushedSuppliersCount)
        } catch (e: Exception) {
            Log.e("WineRepository", "Error pushing local data and syncing", e)
            Triple(0, 0, 0)
        }
    }

    private suspend fun syncFromBackend() = withContext(Dispatchers.IO) {
        try {
            val wines = fetchAllWinesPaginated(limit = 20)
            if (wines.isNotEmpty()) {
                _wines.value = wines
            }
            val locations = fetchAllLocationsPaginated(limit = 20)
            if (locations.isNotEmpty()) {
                _locations.value = locations
            }
            val suppliers = fetchAllSuppliersPaginated(limit = 20)
            if (suppliers.isNotEmpty()) {
                _suppliers.value = suppliers
            }
        } catch (e: Exception) {
            Log.e("WineRepository", "Error syncing with backend", e)
        }
    }

    // --- Wine Operations ---
    suspend fun addWine(wine: Wine): Wine = withContext(Dispatchers.IO) {
        var finalWine = wine
        if (_isBackendOnline.value) {
            try {
                val resp = apiService.createWine(wine)
                if (resp.isSuccessful && resp.body() != null) {
                    finalWine = resp.body()!!
                }
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend addWine failed, applying locally", e)
            }
        }

        _wines.value = _wines.value + finalWine
        finalWine
    }

    suspend fun updateWine(wine: Wine): Wine = withContext(Dispatchers.IO) {
        if (_isBackendOnline.value) {
            try {
                apiService.updateWine(wine.id, wine)
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend updateWine failed", e)
            }
        }
        _wines.value = _wines.value.map { if (it.id == wine.id) wine else it }
        wine
    }

    suspend fun deleteWine(id: String) = withContext(Dispatchers.IO) {
        if (_isBackendOnline.value) {
            try {
                apiService.deleteWine(id)
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend deleteWine failed", e)
            }
        }
        _wines.value = _wines.value.filterNot { it.id == id }
    }

    suspend fun decrementQuantity(id: String): Int = withContext(Dispatchers.IO) {
        var newQty = 0
        val target = _wines.value.find { it.id == id }
        if (target != null) {
            newQty = maxOf(0, target.quantity - 1)
            val updated = target.copy(
                quantity = newQty,
                locationId = if (newQty == 0) null else target.locationId,
                slotRow = if (newQty == 0) null else target.slotRow,
                slotColumn = if (newQty == 0) null else target.slotColumn,
                slotLabel = if (newQty == 0) null else target.slotLabel
            )
            updateWine(updated)

            if (_isBackendOnline.value) {
                try {
                    apiService.decrementInventory(id)
                } catch (e: Exception) {
                    Log.w("WineRepository", "Backend decrement failed", e)
                }
            }
        }
        newQty
    }

    suspend fun assignSlot(wineId: String, locationId: String?, row: Int?, col: Int?): Wine? =
        withContext(Dispatchers.IO) {
            val target = _wines.value.find { it.id == wineId } ?: return@withContext null
            val label = if (row != null && col != null) "Row ${row + 1}, Slot ${col + 1}" else null
            val updated = target.copy(
                locationId = locationId,
                slotRow = row,
                slotColumn = col,
                slotLabel = label
            )

            if (_isBackendOnline.value && locationId != null) {
                try {
                    apiService.assignSlot(
                        locationId,
                        AssignSlotRequest(wineId, locationId, row, col)
                    )
                } catch (e: Exception) {
                    Log.w("WineRepository", "Backend assignSlot failed", e)
                }
            }

            updateWine(updated)
            updated
        }

    fun findWineByBarcode(barcode: String): Wine? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        return _wines.value.find { it.barcode?.trim() == trimmed }
    }

    // --- Cellar Location Operations ---
    suspend fun addLocation(location: CellarLocation): CellarLocation = withContext(Dispatchers.IO) {
        var finalLoc = location
        if (_isBackendOnline.value) {
            try {
                val resp = apiService.createCellarLocation(location)
                if (resp.isSuccessful && resp.body() != null) {
                    finalLoc = resp.body()!!
                }
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend addLocation failed", e)
            }
        }
        _locations.value = _locations.value + finalLoc
        finalLoc
    }

    suspend fun updateLocation(location: CellarLocation): CellarLocation = withContext(Dispatchers.IO) {
        if (_isBackendOnline.value) {
            try {
                apiService.updateCellarLocation(location.id, location)
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend updateCellarLocation failed", e)
            }
        }
        _locations.value = _locations.value.map { if (it.id == location.id) location else it }
        _wines.value = _wines.value.map { wine ->
            if (wine.locationId == location.id) {
                val rowOutOfBounds = wine.slotRow != null && wine.slotRow >= location.rows
                val colOutOfBounds = wine.slotColumn != null && wine.slotColumn >= location.columns
                if (rowOutOfBounds || colOutOfBounds) {
                    wine.copy(slotRow = null, slotColumn = null, slotLabel = "Unassigned slot")
                } else wine
            } else wine
        }
        location
    }

    suspend fun deleteLocation(id: String) = withContext(Dispatchers.IO) {
        if (_isBackendOnline.value) {
            try {
                apiService.deleteCellarLocation(id)
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend deleteLocation failed", e)
            }
        }
        _wines.value = _wines.value.map { wine ->
            if (wine.locationId == id) {
                wine.copy(locationId = null, slotRow = null, slotColumn = null, slotLabel = null)
            } else wine
        }
        _locations.value = _locations.value.filterNot { it.id == id }
    }

    // --- Supplier Operations ---
    suspend fun addSupplier(supplier: Supplier): Supplier = withContext(Dispatchers.IO) {
        var finalSup = supplier
        if (_isBackendOnline.value) {
            try {
                val resp = apiService.createSupplier(supplier)
                if (resp.isSuccessful && resp.body() != null) {
                    finalSup = resp.body()!!
                }
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend addSupplier failed", e)
            }
        }
        _suppliers.value = _suppliers.value + finalSup
        finalSup
    }

    suspend fun updateSupplier(supplier: Supplier): Supplier = withContext(Dispatchers.IO) {
        if (_isBackendOnline.value) {
            try {
                apiService.updateSupplier(supplier.id, supplier)
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend updateSupplier failed", e)
            }
        }
        _suppliers.value = _suppliers.value.map { if (it.id == supplier.id) supplier else it }
        supplier
    }

    suspend fun deleteSupplier(id: String) = withContext(Dispatchers.IO) {
        if (_isBackendOnline.value) {
            try {
                apiService.deleteSupplier(id)
            } catch (e: Exception) {
                Log.w("WineRepository", "Backend deleteSupplier failed", e)
            }
        }
        _suppliers.value = _suppliers.value.filterNot { it.id == id }
    }
}

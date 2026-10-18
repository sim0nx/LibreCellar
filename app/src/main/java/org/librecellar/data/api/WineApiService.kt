package org.librecellar.data.api

import org.librecellar.data.model.AssignSlotRequest
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.DecrementInventoryResponse
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

import retrofit2.http.Query

interface WineApiService {

    // --- Wines API ---
    @GET("api/wines")
    suspend fun getWines(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Wine>>

    @GET("api/wines/{id}")
    suspend fun getWineById(@Path("id") id: String): Response<Wine>

    @GET("api/wines/barcode/{barcode}")
    suspend fun getWineByBarcode(@Path("barcode") barcode: String): Response<Wine>

    @POST("api/wines")
    suspend fun createWine(@Body wine: Wine): Response<Wine>

    @PUT("api/wines/{id}")
    suspend fun updateWine(@Path("id") id: String, @Body wine: Wine): Response<Wine>

    @DELETE("api/wines/{id}")
    suspend fun deleteWine(@Path("id") id: String): Response<Unit>

    @POST("api/wines/{id}/decrement")
    suspend fun decrementInventory(@Path("id") id: String): Response<DecrementInventoryResponse>

    // --- Cellar Locations & Racks API ---
    @GET("api/cellar/locations")
    suspend fun getCellarLocations(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<CellarLocation>>

    @GET("api/racks")
    suspend fun getRacks(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<CellarLocation>>

    @POST("api/cellar/locations")
    suspend fun createCellarLocation(@Body location: CellarLocation): Response<CellarLocation>

    @PUT("api/cellar/locations/{id}")
    suspend fun updateCellarLocation(
        @Path("id") id: String,
        @Body location: CellarLocation
    ): Response<CellarLocation>

    @DELETE("api/cellar/locations/{id}")
    suspend fun deleteCellarLocation(@Path("id") id: String): Response<Unit>

    @POST("api/cellar/locations/{id}/assign-slot")
    suspend fun assignSlot(
        @Path("id") locationId: String,
        @Body request: AssignSlotRequest
    ): Response<Wine>

    // --- Suppliers & Merchants API ---
    @GET("api/suppliers")
    suspend fun getSuppliers(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Supplier>>

    @GET("api/merchants")
    suspend fun getMerchants(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<Supplier>>

    @POST("api/suppliers")
    suspend fun createSupplier(@Body supplier: Supplier): Response<Supplier>

    @PUT("api/suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: String, @Body supplier: Supplier): Response<Supplier>

    @DELETE("api/suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: String): Response<Unit>

    // --- Health check ---
    @GET("api/health")
    suspend fun checkHealth(): Response<Map<String, String>>
}

package org.librecellar.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.librecellar.data.api.WineApiService
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose
import java.io.File
import java.security.SecureRandom
import java.util.UUID

val LocalWineRepository = compositionLocalOf<WineRepository?> { null }

object WineImageHelper {

    private const val TAG = "WineImageHelper"
    private const val PREFS_NAME = "librecellar_prefs"
    private const val KEY_USER_UUID = "user_uuid_v4"

    /**
     * Retrieves or generates a persistent UUIDv4 identifying this user.
     */
    fun getUserUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_USER_UUID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val newUuid = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_USER_UUID, newUuid).apply()
        return newUuid
    }

    /**
     * Generates an RFC 9562-compliant time-ordered UUIDv7 string.
     */
    fun generateUuidV7(): String {
        val timestamp = System.currentTimeMillis()
        val randomBytes = ByteArray(10)
        SecureRandom().nextBytes(randomBytes)

        val time48 = timestamp and 0x0000_FFFF_FFFF_FFFFL
        val randA = ((randomBytes[0].toInt() and 0x0F) shl 8) or (randomBytes[1].toInt() and 0xFF)
        val msb = (time48 shl 16) or (0x7000L) or randA.toLong()

        var randB = 0L
        for (i in 2 until 10) {
            randB = (randB shl 8) or (randomBytes[i].toLong() and 0xFFL)
        }
        randB = randB and 0x3FFF_FFFF_FFFF_FFFFL
        val lsb = (0x2L shl 62) or randB

        return UUID(msb, lsb).toString()
    }

    /**
     * The primary local cache directory for bottle pictures.
     */
    fun getBottleCacheDir(context: Context): File {
        return File(context.filesDir, "bottle_cache").apply { mkdirs() }
    }

    fun isPreset(photoUrl: String?): Boolean {
        return !photoUrl.isNullOrBlank() && photoUrl.startsWith("preset_")
    }

    fun getPresetColor(presetId: String?): Color {
        return when (presetId) {
            "preset_bordeaux_red" -> Color(0xFF5B1527)
            "preset_burgundy_pinot" -> Color(0xFF7A1D36)
            "preset_champagne_brut" -> Color(0xFFC6A052)
            "preset_white_chardonnay" -> Color(0xFFB5A96A)
            "preset_rose_provence" -> Color(0xFFD47988)
            "preset_reserve_port" -> Color(0xFF3B1824)
            else -> Color(0xFF5B1527)
        }
    }

    fun getPresetName(presetId: String?): String {
        return when (presetId) {
            "preset_bordeaux_red" -> "Bordeaux Red"
            "preset_burgundy_pinot" -> "Burgundy Pinot"
            "preset_champagne_brut" -> "Champagne Brut"
            "preset_white_chardonnay" -> "Chardonnay"
            "preset_rose_provence" -> "Provence Rosé"
            "preset_reserve_port" -> "Vintage Port"
            else -> "Cellar Preset"
        }
    }

    /**
     * Checks if the photo exists in the Android device's local cache.
     * Looks in bottle_cache, bottle_photos, and camera_photos.
     */
    fun getCachedImageFile(context: Context, filenameOrPath: String?): File? {
        if (filenameOrPath.isNullOrBlank() || isPreset(filenameOrPath)) return null

        // 1. Direct path check
        val directFile = File(filenameOrPath)
        if (directFile.exists() && directFile.isFile && directFile.length() > 0) {
            return directFile
        }

        // 2. Filename check in primary bottle_cache directory
        val cacheFile = File(getBottleCacheDir(context), filenameOrPath)
        if (cacheFile.exists() && cacheFile.isFile && cacheFile.length() > 0) {
            return cacheFile
        }

        // 3. Fallback to legacy bottle_photos directory
        val legacyFile = File(File(context.filesDir, "bottle_photos"), filenameOrPath)
        if (legacyFile.exists() && legacyFile.isFile && legacyFile.length() > 0) {
            return legacyFile
        }

        // 4. Fallback to camera_photos cache directory
        val cameraCacheFile = File(File(context.cacheDir, "camera_photos"), filenameOrPath)
        if (cameraCacheFile.exists() && cameraCacheFile.isFile && cameraCacheFile.length() > 0) {
            return cameraCacheFile
        }

        return null
    }

    /**
     * Removes the cached image from the Android device when a wine is deleted.
     */
    fun removeCachedImage(context: Context, filenameOrPath: String?): Boolean {
        if (filenameOrPath.isNullOrBlank() || isPreset(filenameOrPath)) return false

        var deletedAny = false
        try {
            // Check direct path
            val direct = File(filenameOrPath)
            if (direct.exists()) {
                deletedAny = direct.delete() || deletedAny
            }

            // Check primary cache
            val inCache = File(getBottleCacheDir(context), filenameOrPath)
            if (inCache.exists()) {
                deletedAny = inCache.delete() || deletedAny
            }

            // Check legacy photos
            val inLegacy = File(File(context.filesDir, "bottle_photos"), filenameOrPath)
            if (inLegacy.exists()) {
                deletedAny = inLegacy.delete() || deletedAny
            }

            // Check camera photos
            val inCamera = File(File(context.cacheDir, "camera_photos"), filenameOrPath)
            if (inCamera.exists()) {
                deletedAny = inCamera.delete() || deletedAny
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing cached image $filenameOrPath", e)
        }
        return deletedAny
    }

    /**
     * Create a new temporary file in camera_photos and returns a content URI for TakePicture contract.
     */
    fun createCameraImageUri(context: Context): Pair<File, Uri> {
        val photosDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val tempFile = File.createTempFile("camera_raw_${System.currentTimeMillis()}_", ".jpg", photosDir)
        val uri = FileProvider.getUriForFile(
            context,
            "org.librecellar.fileprovider",
            tempFile
        )
        return Pair(tempFile, uri)
    }

    /**
     * Saves an image from a content URI into local cache with a UUIDv7 filename.
     */
    fun saveUriToLocalCache(context: Context, sourceUri: Uri): Pair<File, String> {
        val filename = "${generateUuidV7()}.jpg"
        val destFile = File(getBottleCacheDir(context), filename)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return Pair(destFile, filename)
    }

    /**
     * Saves a captured camera file into local cache with a UUIDv7 filename.
     */
    fun saveCameraFileToLocalCache(context: Context, cameraFile: File): Pair<File, String> {
        val filename = "${generateUuidV7()}.jpg"
        val destFile = File(getBottleCacheDir(context), filename)
        cameraFile.copyTo(destFile, overwrite = true)
        return Pair(destFile, filename)
    }

    /**
     * Uploads a bottle picture to the backend API.
     * The backend stores it in configurable path under the user's UUIDv4 subfolder
     * and renames it to UUIDv7.<ext>.
     * Returns the filename (UUIDv7.<ext>).
     */
    suspend fun uploadImageToBackend(
        context: Context,
        apiService: WineApiService,
        userUuid: String,
        sourceFile: File
    ): String = withContext(Dispatchers.IO) {
        try {
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestBody = sourceFile.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("file", sourceFile.name, requestBody)

            val resp = apiService.uploadImage(part, userUuid)
            if (resp.isSuccessful && resp.body() != null) {
                val serverFilename = resp.body()!!.filename
                // Copy or link into local cache under the server's UUIDv7 filename
                val cachedFile = File(getBottleCacheDir(context), serverFilename)
                if (!cachedFile.exists() || cachedFile.absolutePath != sourceFile.absolutePath) {
                    sourceFile.copyTo(cachedFile, overwrite = true)
                }
                Log.d(TAG, "Uploaded image successfully, server filename: $serverFilename")
                return@withContext serverFilename
            } else {
                Log.w(TAG, "Backend image upload returned error ${resp.code()}, keeping local cache")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload image to backend, storing in local cache", e)
        }

        // Fallback: If offline or failed, generate UUIDv7 filename locally
        val localUuid7Filename = if (sourceFile.name.startsWith("01")) {
            sourceFile.name
        } else {
            "${generateUuidV7()}.jpg"
        }
        val cachedFile = File(getBottleCacheDir(context), localUuid7Filename)
        if (cachedFile.absolutePath != sourceFile.absolutePath) {
            sourceFile.copyTo(cachedFile, overwrite = true)
        }
        localUuid7Filename
    }

    /**
     * Requests a bottle picture from the backend API if not available in local cache.
     * Caches the downloaded stream into bottle_cache/<filename>.
     */
    suspend fun fetchAndCacheRemoteImage(
        context: Context,
        filename: String,
        apiService: WineApiService,
        userUuid: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val resp = apiService.downloadImage(filename, userUuid)
            if (resp.isSuccessful && resp.body() != null) {
                val destFile = File(getBottleCacheDir(context), filename)
                resp.body()!!.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Downloaded and cached remote image: $filename")
                return@withContext destFile
            } else {
                Log.w(TAG, "Failed to download image $filename, status ${resp.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error downloading image $filename", e)
        }
        null
    }
}

@Composable
fun WineBottleImage(
    photoUrl: String?,
    wineType: String = "RED",
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 32.dp,
    contentDescription: String? = "Wine bottle photo",
    repository: WineRepository? = LocalWineRepository.current
) {
    val cleanUrl = photoUrl?.trim()
    val context = LocalContext.current

    if (cleanUrl.isNullOrBlank()) {
        DefaultWinePlaceholder(
            wineType = wineType,
            modifier = modifier.clip(shape),
            iconSize = iconSize
        )
    } else if (WineImageHelper.isPreset(cleanUrl)) {
        val presetColor = WineImageHelper.getPresetColor(cleanUrl)
        Box(
            modifier = modifier
                .clip(shape)
                .background(presetColor)
                .border(1.dp, CellarGold.copy(alpha = 0.5f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WineBar,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        // State for the cached file
        var cachedFile by remember(cleanUrl) {
            mutableStateOf(WineImageHelper.getCachedImageFile(context, cleanUrl))
        }

        // If not in cache and repository is available, request from backend
        LaunchedEffect(cleanUrl, cachedFile) {
            if (cachedFile == null && repository != null) {
                val fetched = repository.getOrFetchCachedImage(cleanUrl)
                if (fetched != null && fetched.exists()) {
                    cachedFile = fetched
                }
            }
        }

        val imageSource: Any? = cachedFile ?: if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            cleanUrl
        } else null

        if (imageSource != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier.clip(shape),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF24161C)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CellarGold
                        )
                    }
                },
                error = {
                    DefaultWinePlaceholder(
                        wineType = wineType,
                        modifier = Modifier.fillMaxSize(),
                        iconSize = iconSize
                    )
                }
            )
        } else {
            // Downloading or waiting for cache fetch
            Box(
                modifier = modifier
                    .clip(shape)
                    .background(Color(0xFF24161C)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = CellarGold
                )
            }
        }
    }
}

@Composable
fun DefaultWinePlaceholder(
    wineType: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp
) {
    val (bgColor, iconColor) = when (wineType) {
        "WHITE" -> Pair(Color(0xFF232117), Color(0xFFD4C876))
        "SPARKLING" -> Pair(Color(0xFF262013), CellarGold)
        "ROSE" -> Pair(Color(0xFF2B161B), WineRose)
        else -> Pair(Color(0xFF24161C), WineBurgundyLight)
    }

    Box(
        modifier = modifier.background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.WineBar,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

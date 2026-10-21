package org.librecellar.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundyLight
import java.io.File

data class WinePhotoPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeColor: Color
)

@Composable
fun PhotoCaptureDialog(
    onPhotoSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    repository: WineRepository? = LocalWineRepository.current
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentCameraFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Launcher for taking full-resolution photo via system camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraFile != null && currentCameraFile!!.exists()) {
            isProcessing = true
            errorMessage = null
            scope.launch {
                try {
                    val finalFilename = if (repository != null) {
                        repository.uploadBottleImage(currentCameraFile!!.absolutePath)
                    } else {
                        val (_, filename) = WineImageHelper.saveCameraFileToLocalCache(context, currentCameraFile!!)
                        filename
                    }
                    onPhotoSelected(finalFilename)
                } catch (e: Exception) {
                    errorMessage = "Failed to process photo: ${e.localizedMessage}"
                    isProcessing = false
                }
            }
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val (file, uri) = WineImageHelper.createCameraImageUri(context)
                currentCameraFile = file
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                errorMessage = "Failed to launch camera: ${e.localizedMessage}"
            }
        } else {
            errorMessage = "Camera permission is required to take photos"
        }
    }

    fun launchCamera() {
        errorMessage = null
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val (file, uri) = WineImageHelper.createCameraImageUri(context)
                currentCameraFile = file
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                errorMessage = "Failed to launch camera: ${e.localizedMessage}"
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Launcher for selecting existing photo from device photos/gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            errorMessage = null
            scope.launch {
                try {
                    val finalFilename = if (repository != null) {
                        repository.uploadBottleImage(uri.toString())
                    } else {
                        val (_, filename) = WineImageHelper.saveUriToLocalCache(context, uri)
                        filename
                    }
                    onPhotoSelected(finalFilename)
                } catch (e: Exception) {
                    errorMessage = "Failed to process selected image: ${e.localizedMessage}"
                    isProcessing = false
                }
            }
        }
    }

    val bottlePresets = listOf(
        WinePhotoPreset("preset_bordeaux_red", "Grand Cru Red", "Dark Oak & Berry label", Color(0xFF5B1527)),
        WinePhotoPreset("preset_burgundy_pinot", "Burgundy Pinot", "Gold Crest Vintage label", Color(0xFF7A1D36)),
        WinePhotoPreset("preset_champagne_brut", "Champagne Brut", "Gold Foil Cuvée", Color(0xFFC6A052)),
        WinePhotoPreset("preset_white_chardonnay", "White Chardonnay", "Crisp Pale Estate", Color(0xFFB5A96A)),
        WinePhotoPreset("preset_rose_provence", "Provence Rosé", "Blush Petal Chateau", Color(0xFFD47988)),
        WinePhotoPreset("preset_reserve_port", "Vintage Port", "Wax Sealed Reserve", Color(0xFF3B1824))
    )

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("photo_capture_dialog")
        ) {
            if (isProcessing) {
                // Uploading / processing progress state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = AppIcons.CloudUpload,
                        contentDescription = "Uploading",
                        tint = CellarGold,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        color = CellarGold,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Uploading picture to backend...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Storing in your collection with unique UUIDv7 filename and caching locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.CameraAlt,
                                contentDescription = "Bottle Photo",
                                tint = CellarGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wine Bottle Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_photo_dialog")) {
                            Icon(imageVector = AppIcons.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Error message if any
                    if (errorMessage != null) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // 1. Take Photo with Camera Button
                    Button(
                        onClick = { launchCamera() },
                        colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("take_photo_camera_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = AppIcons.CameraAlt, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo with Camera", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Select from Device Gallery Button
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choose_from_gallery_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = AppIcons.AddPhotoAlternate, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select from Device Photos", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Or choose a cellar bottle visual preset:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(bottlePresets) { preset ->
                            Card(
                                onClick = { onPhotoSelected(preset.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("photo_preset_${preset.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(preset.badgeColor)
                                            .border(1.5.dp, CellarGold.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.WineBar,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

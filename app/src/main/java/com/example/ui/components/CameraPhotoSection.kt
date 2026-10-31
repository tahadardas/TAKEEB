package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import java.io.FileOutputStream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.TruckViewModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraPhotoSection(
    viewModel: TruckViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by viewModel.capturedPhotos.collectAsState()
    val viewingPhoto by viewModel.viewingPhotoPath.collectAsState()

    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera Capture Launcher (TakePicture)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            pendingPhotoFile?.let { file ->
                viewModel.addCapturedPhoto(file.absolutePath)
                com.example.util.AppFolderManager.copyPhotoToPublic(context, file)
                Toast.makeText(context, "تم التقاط الصورة وحفظ نسخة في مجلد ملفاتي", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gallery Photo Picker Launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var addedCount = 0
            uris.forEach { uri ->
                copyUriToInternalPhotos(context, uri)?.let { file ->
                    viewModel.addCapturedPhoto(file.absolutePath)
                    com.example.util.AppFolderManager.copyPhotoToPublic(context, file)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                Toast.makeText(context, "تم تحميل $addedCount صورة من المعرض وحفظ نسخة في ملفاتي", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission Launcher for CAMERA
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera(context, { file, uri ->
                pendingPhotoFile = file
                pendingPhotoUri = uri
                takePictureLauncher.launch(uri)
            })
        } else {
            Toast.makeText(context, "يلزم إذن الكاميرا لالتقاط وتأكيد صور السيارة", Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera(context, { file, uri ->
                pendingPhotoFile = file
                pendingPhotoUri = uri
                takePictureLauncher.launch(uri)
            })
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "صور التوثيق (الكاميرا أو المعرض)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "التقاط صور حية أو اختيار صور من المعرض لتأكيد التكعيب والحمولة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "كاميرا ومعرض",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Dual Photo Action Buttons (Camera & Gallery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { checkAndLaunchCamera() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("take_camera_photo_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التقاط بالكاميرا",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                FilledTonalButton(
                    onClick = {
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pick_gallery_photo_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تحميل من المعرض",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Captured Photos List
            if (photos.isNotEmpty()) {
                Text(
                    text = "الصور الملتقطة (${photos.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    photos.forEach { path ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.openPhotoViewer(path) }
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "صورة السيارة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Delete button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { viewModel.removeCapturedPhoto(path) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف الصورة",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "لم يتم التقاط صور بعد. اضغط على الزر أعلاه لفتح الكاميرا والتقاط صورة للوحة أو الصندوق أو الحمولة.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Full-screen photo viewer dialog
    if (viewingPhoto != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closePhotoViewer() },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معاينة صورة السيارة",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { viewModel.closePhotoViewer() }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = File(viewingPhoto!!),
                        contentDescription = "صورة مكبرة",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { viewModel.closePhotoViewer() }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

internal fun copyUriToInternalPhotos(context: Context, uri: Uri): File? {
    return try {
        val photoDir = File(context.filesDir, "truck_photos")
        if (!photoDir.exists()) photoDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val randomSuffix = (1000..9999).random()
        val photoFile = File(photoDir, "IMG_GALLERY_${timeStamp}_$randomSuffix.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(photoFile).use { output ->
                input.copyTo(output)
            }
        }
        photoFile
    } catch (e: Exception) {
        null
    }
}

private fun launchCamera(context: Context, onReady: (File, Uri) -> Unit) {
    val photoDir = File(context.filesDir, "truck_photos")
    if (!photoDir.exists()) photoDir.mkdirs()

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val photoFile = File(photoDir, "IMG_CAMERA_${timeStamp}.jpg")
    val photoUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    onReady(photoFile, photoUri)
}

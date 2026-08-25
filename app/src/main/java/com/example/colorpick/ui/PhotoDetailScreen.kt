package com.example.colorpick.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full-screen gallery preview with paging, zoom/pan, share, metadata and delete actions. */
@Composable
fun PhotoDetailScreen(
    photoUri: Uri,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photos = remember(context) { queryDetailPhotos(context) }.ifEmpty { listOf(photoUri) }
    val initialPage = remember(photoUri, photos) { photos.indexOf(photoUri).coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { photos.size })
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<PhotoExifInfo?>(null) }
    var currentPhotoZoomed by remember { mutableStateOf(false) }
    val currentUri = photos.getOrNull(pagerState.currentPage) ?: photoUri
    LaunchedEffect(pagerState.currentPage) { currentPhotoZoomed = false }

    BackHandler(enabled = !deleting) { onBack() }

    LaunchedEffect(showInfo, currentUri) {
        if (showInfo) {
            info = withContext(Dispatchers.IO) { readPhotoExif(context, currentUri) }
        } else {
            info = null
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            // A zoomed photo owns the gesture; at 1x the pager owns it.
            userScrollEnabled = !currentPhotoZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomablePhoto(
                uri = photos[page],
                onZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) currentPhotoZoomed = zoomed
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "返回",
                tint = Color.White,
                modifier = Modifier.size(44.dp).clickable(onClick = onBack)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    "图片信息",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp).clickable { showInfo = true }
                )
                Icon(
                    Icons.Default.Share,
                    "分享",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp).clickable { sharePhoto(context, currentUri) }
                )
            }
        }

        Icon(
            Icons.Default.Delete,
            "删除",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp)
                .clickable(enabled = !deleting) { showDeleteConfirm = true }
        )

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { if (!deleting) showDeleteConfirm = false },
                title = { Text("删除照片？") },
                text = { Text("此操作不可恢复。") },
                confirmButton = {
                    TextButton(enabled = !deleting, onClick = {
                        showDeleteConfirm = false
                        deleting = true
                        scope.launch(Dispatchers.IO) {
                            val deleted = try {
                                context.contentResolver.delete(currentUri, null, null) > 0
                            } catch (_: Exception) { false }
                            withContext(Dispatchers.Main) {
                                deleting = false
                                if (deleted) onDeleted()
                                else Toast.makeText(context, "删除失败，请重试", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(enabled = !deleting, onClick = { showDeleteConfirm = false }) { Text("取消") }
                }
            )
        }

        if (showInfo) {
            PhotoInfoDialog(
                info = info,
                onDismiss = { showInfo = false }
            )
        }
    }
}

private data class PhotoLoadResult(val bitmap: Bitmap?, val finished: Boolean, val failed: Boolean)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomablePhoto(uri: Uri, onZoomChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val result by produceState(initialValue = PhotoLoadResult(null, finished = false, failed = false), uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val loaded = context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
                PhotoLoadResult(loaded, finished = true, failed = loaded == null)
            } catch (_: Exception) {
                PhotoLoadResult(null, finished = true, failed = true)
            }
        }
    }
    val bitmap = result.bitmap
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(uri) { mutableFloatStateOf(0f) }
    var imageVisible by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(bitmap) { if (bitmap != null) imageVisible = true }
    LaunchedEffect(scale > 1f) { onZoomChanged(scale > 1f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        if (nextScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }
    val gestureModifier = Modifier.transformable(
        state = transformState,
        canPan = { scale > 1f },
        lockRotationOnZoomPan = true,
        enabled = true
    )
    val doubleTapModifier = Modifier.pointerInput(uri) {
        detectTapGestures(onDoubleTap = {
            if (scale > 1f) {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            } else {
                scale = 2f
            }
        })
    }
    Box(
        Modifier.fillMaxSize().then(gestureModifier).then(doubleTapModifier),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            AnimatedVisibility(
                visible = imageVisible,
                enter = scaleIn(initialScale = 0.86f, animationSpec = tween(180)),
                exit = scaleOut(targetScale = 1f, animationSpec = tween(120))
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "照片预览",
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                    contentScale = ContentScale.Fit
                )
            }
        } ?: if (!result.finished) {
            // Loading is intentionally quiet: do not present an error while a new page is decoding.
            Box(Modifier.fillMaxSize())
        } else {
            Text("无法加载照片", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

private data class PhotoExifInfo(
    val fileName: String,
    val dimensions: String,
    val fileSize: String,
    val dateTime: String,
    val camera: String,
    val lens: String,
    val exposure: String,
    val iso: String,
    val orientation: String,
    val location: String
)

@Composable
private fun PhotoInfoDialog(info: PhotoExifInfo?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("图片信息") },
        text = {
            if (info == null) {
                Text("正在读取 EXIF 信息…")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    InfoRow("文件", info.fileName)
                    InfoRow("尺寸", info.dimensions)
                    InfoRow("大小", info.fileSize)
                    InfoRow("拍摄时间", info.dateTime)
                    InfoRow("相机", info.camera)
                    InfoRow("镜头", info.lens)
                    InfoRow("曝光", info.exposure)
                    InfoRow("ISO", info.iso)
                    InfoRow("方向", info.orientation)
                    if (info.location != "不可用") InfoRow("位置", info.location)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontSize = 13.sp)
    }
}

private fun readPhotoExif(context: Context, uri: Uri): PhotoExifInfo {
    fun unknown(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "不可用"
    val resolver = context.contentResolver
    val fileName = resolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE), null, null, null)?.use {
        if (it.moveToFirst()) {
            val name = it.getString(0) ?: "未知文件"
            val size = if (!it.isNull(1)) formatFileSize(it.getLong(1)) else "不可用"
            return@use name to size
        }
        null
    } ?: ("未知文件" to "不可用")
    return try {
        val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
        PhotoExifInfo(
            fileName = fileName.first,
            dimensions = listOf(exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0), exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0))
                .takeIf { it?.all { value -> value != null && value > 0 } == true }
                ?.let { "${it[0]} × ${it[1]}" } ?: "不可用",
            fileSize = fileName.second,
            dateTime = unknown(exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)),
            camera = listOf(exif?.getAttribute(ExifInterface.TAG_MAKE), exif?.getAttribute(ExifInterface.TAG_MODEL)).mapNotNull { it?.takeIf(String::isNotBlank) }.joinToString(" ").ifBlank { "不可用" },
            lens = unknown(exif?.getAttribute(ExifInterface.TAG_LENS_MODEL)),
            exposure = unknown(exif?.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)),
            iso = unknown(exif?.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: exif?.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)),
            orientation = unknown(exif?.getAttribute(ExifInterface.TAG_ORIENTATION)),
            location = if (exif?.latLong != null) "${exif.latLong!![0]}, ${exif.latLong!![1]}" else "不可用"
        )
    } catch (_: Exception) {
        PhotoExifInfo(fileName.first, "不可用", fileName.second, "不可用", "不可用", "不可用", "不可用", "不可用", "不可用", "不可用")
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}

private fun queryDetailPhotos(context: Context): List<Uri> {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val useRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val selection = if (useRelativePath) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    } else {
        "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?"
    }
    val args = if (useRelativePath) arrayOf("Pictures/ColorPick%", "Pictures/ColorBy%")
    else arrayOf("%/ColorPick/%", "%/ColorBy/%")
    return try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            buildList {
                while (cursor.moveToNext()) {
                    add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)))
                }
            }
        } ?: emptyList()
    } catch (_: Exception) { emptyList() }
}

private fun sharePhoto(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "分享照片"))
    } catch (_: Exception) {
        Toast.makeText(context, "暂无可用的分享应用", Toast.LENGTH_SHORT).show()
    }
}

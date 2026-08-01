package com.example.colorpick.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CropRatio(val title: String, val ratio: Float) {
    R4_3("4:3", 4f / 3f),
    R1_1("1:1", 1f),
    R16_9("16:9", 16f / 9f)
}

@Composable
fun CropScreen(
    photoUri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(photoUri) { loadBitmapFromUri(context, photoUri) }
    var selectedRatio by remember { mutableStateOf(CropRatio.R4_3) }
    var imageBounds by remember { mutableStateOf(Rect.Zero) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        bitmap?.let { source ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 120.dp, horizontal = 24.dp)
                    .onGloballyPositioned { coordinates ->
                        containerSize = coordinates.boundsInParent().size
                        imageBounds = calculateFitBounds(
                            containerSize.width,
                            containerSize.height,
                            source.width.toFloat(),
                            source.height.toFloat()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = source.asImageBitmap(),
                    contentDescription = "待裁剪照片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                CropFrameOverlay(
                    imageBounds = imageBounds,
                    ratio = selectedRatio.ratio,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("无法加载图片", color = Color.White, fontSize = 16.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CropRatio.entries.forEach { ratio ->
                    val selected = ratio == selectedRatio
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) Color.White.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (selected) Color.White.copy(alpha = 0.7f)
                                else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selectedRatio = ratio }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            ratio.title,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            bitmap?.let {
                                val cropRect = computeCropRect(imageBounds, selectedRatio.ratio, it.width, it.height)
                                if (cropRect.width() > 0 && cropRect.height() > 0) {
                                    saveCroppedBitmap(context, it, cropRect)
                                    onBack()
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "完成",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun CropFrameOverlay(
    imageBounds: Rect,
    ratio: Float,
    modifier: Modifier = Modifier
) {
    if (imageBounds == Rect.Zero) return

    val frameBounds = remember(imageBounds, ratio) {
        computeFrameBounds(imageBounds, ratio)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dark = Color.Black.copy(alpha = 0.55f)
            drawRect(dark, size = size)
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(frameBounds.left, frameBounds.top),
                size = Size(frameBounds.width, frameBounds.height),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(frameBounds.left, frameBounds.top),
                size = Size(frameBounds.width, frameBounds.height),
                style = Stroke(width = 2f)
            )
            val thirdW = frameBounds.width / 3f
            val thirdH = frameBounds.height / 3f
            val gridColor = Color.White.copy(alpha = 0.45f)
            for (i in 1..2) {
                drawLine(
                    gridColor,
                    Offset(frameBounds.left + thirdW * i, frameBounds.top),
                    Offset(frameBounds.left + thirdW * i, frameBounds.bottom),
                    strokeWidth = 1f
                )
                drawLine(
                    gridColor,
                    Offset(frameBounds.left, frameBounds.top + thirdH * i),
                    Offset(frameBounds.right, frameBounds.top + thirdH * i),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun calculateFitBounds(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): Rect {
    if (containerWidth <= 0 || containerHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
        return Rect.Zero
    }
    val imageRatio = imageWidth / imageHeight
    val containerRatio = containerWidth / containerHeight
    val width: Float
    val height: Float
    if (imageRatio > containerRatio) {
        width = containerWidth
        height = containerWidth / imageRatio
    } else {
        height = containerHeight
        width = containerHeight * imageRatio
    }
    val left = (containerWidth - width) / 2f
    val top = (containerHeight - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun computeFrameBounds(imageBounds: Rect, ratio: Float): Rect {
    val imageRatio = imageBounds.width / imageBounds.height
    val width: Float
    val height: Float
    if (ratio > imageRatio) {
        width = imageBounds.width
        height = imageBounds.width / ratio
    } else {
        height = imageBounds.height
        width = imageBounds.height * ratio
    }
    val left = imageBounds.left + (imageBounds.width - width) / 2f
    val top = imageBounds.top + (imageBounds.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun computeCropRect(
    imageBounds: Rect,
    ratio: Float,
    bitmapWidth: Int,
    bitmapHeight: Int
): android.graphics.Rect {
    val frame = computeFrameBounds(imageBounds, ratio)
    val scaleX = bitmapWidth / imageBounds.width
    val scaleY = bitmapHeight / imageBounds.height
    val left = ((frame.left - imageBounds.left) * scaleX).toInt().coerceIn(0, bitmapWidth)
    val top = ((frame.top - imageBounds.top) * scaleY).toInt().coerceIn(0, bitmapHeight)
    val right = ((frame.right - imageBounds.left) * scaleX).toInt().coerceIn(0, bitmapWidth)
    val bottom = ((frame.bottom - imageBounds.top) * scaleY).toInt().coerceIn(0, bitmapHeight)
    return android.graphics.Rect(left, top, right, bottom)
}

private fun saveCroppedBitmap(context: Context, source: Bitmap, cropRect: android.graphics.Rect) {
    val cropped = Bitmap.createBitmap(source, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val displayName = "IMG_${timeStamp}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ColorPick")
        }
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let { outputUri ->
        try {
            context.contentResolver.openOutputStream(outputUri, "w")?.use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                MediaScannerConnection.scanFile(context, arrayOf(outputUri.toString()), arrayOf("image/jpeg"), null)
            }
        } catch (e: Exception) {
            Log.e("CropScreen", "Save cropped bitmap failed", e)
        }
    }
    cropped.recycle()
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input)
        }
    } catch (e: Exception) {
        Log.e("CropScreen", "Load bitmap failed", e)
        null
    }
}

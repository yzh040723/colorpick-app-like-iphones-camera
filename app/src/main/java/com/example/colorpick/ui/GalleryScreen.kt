package com.example.colorpick.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class GalleryPhoto(val uri: Uri, val dateAdded: Long)

/**
 * App-internal gallery: shows every photo saved by the app (MediaStore path
 * Pictures/ColorPick), newest first, in a 5-column grid with pagination, multi-select
 * deletion and a floating camera button that returns to the camera.
 */
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    onPhotoClick: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // The full list is queried once; the grid only renders the first displayCount
    // items and reveals more as the user scrolls (paged UI without SQL LIMIT/OFFSET,
    // which is not reliably supported by every MediaStore provider).
    val pageSize = 30
    var displayCount by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    // Latest values for the drag-select gesture detector.
    val currentSelectionMode by rememberUpdatedState(selectionMode)
    val currentPhotos by rememberUpdatedState(photos)
    val currentDisplayCount by rememberUpdatedState(displayCount)

    suspend fun loadAll(): List<GalleryPhoto> =
        try {
            withContext(Dispatchers.IO) { queryColorPickPhotos(context) }
        } catch (e: Exception) {
            emptyList()
        }

    // Load the full list once; start showing the first page.
    LaunchedEffect(Unit) {
        photos = loadAll()
        displayCount = minOf(pageSize, photos.size)
    }

    // Reveal more photos when the grid scrolls near the end.
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                val target = lastIndex ?: return@collect
                if (target >= displayCount - 6 && displayCount < photos.size) {
                    displayCount = minOf(displayCount + pageSize, photos.size)
                }
            }
    }

    fun deleteSelected() {
        scope.launch(Dispatchers.IO) {
            selected.forEach { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (_: Exception) {
                }
            }
            val remaining = queryColorPickPhotos(context)
            withContext(Dispatchers.Main) {
                photos = remaining
                displayCount = minOf(pageSize, remaining.size)
                selected = emptySet()
                selectionMode = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
    ) {
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "暂无照片",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                state = gridState,
                // While multi-selecting, scrolling is disabled and a drag sweeps across
                // the grid to select/deselect photos; otherwise normal scrolling.
                userScrollEnabled = !selectionMode,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var dragSelectValue = false
                        var lastDragIndex = -1
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (!currentSelectionMode) return@detectDragGestures
                                val index = hitTestGridItem(gridState, offset)
                                if (index != null && index < currentDisplayCount) {
                                    val uri = currentPhotos[index].uri
                                    // Starting on an unselected photo selects as you drag;
                                    // starting on a selected one deselects as you drag.
                                    dragSelectValue = uri !in selected
                                    selected = if (dragSelectValue) selected + uri else selected - uri
                                    lastDragIndex = index
                                }
                            },
                            onDrag = { change, _ ->
                                if (!currentSelectionMode) return@detectDragGestures
                                val index = hitTestGridItem(gridState, change.position)
                                if (index != null && index != lastDragIndex && index < currentDisplayCount) {
                                    lastDragIndex = index
                                    val uri = currentPhotos[index].uri
                                    selected = if (dragSelectValue) selected + uri else selected - uri
                                }
                            },
                            onDragEnd = {},
                            onDragCancel = {}
                        )
                    },
                contentPadding = PaddingValues(top = 96.dp, bottom = 100.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(displayCount, key = { photos[it].uri.toString() }) { index ->
                    val photo = photos[index]
                    GalleryGridItem(
                        photo = photo,
                        isSelected = photo.uri in selected,
                        onClick = {
                            if (selectionMode) {
                                selected = if (photo.uri in selected) selected - photo.uri else selected + photo.uri
                                if (selected.isEmpty()) selectionMode = false
                            } else onPhotoClick(photo.uri)
                        },
                        onLongClick = {
                            selectionMode = true
                            selected = selected + photo.uri
                        }
                    )
                }
            }
        }

        // Frosted-black top bar: translucent gradient fading downward.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消选择",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            selected = emptySet()
                            selectionMode = false
                        }
                )
                Text(
                    text = "已选 ${selected.size}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select / deselect every currently displayed photo.
                    val displayed = photos.take(displayCount)
                    val allSelected = displayed.isNotEmpty() && displayed.all { it.uri in selected }
                    Text(
                        text = if (allSelected) "取消全选" else "全选",
                        color = Color(0xFFFFD60A),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable {
                                selected = if (allSelected) emptySet() else displayed.map { it.uri }.toSet()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = if (selected.isEmpty()) Color.White.copy(alpha = 0.3f) else Color(0xFFFFD60A),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(enabled = selected.isNotEmpty()) {
                                showDeleteConfirm = true
                            }
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack)
                )
                Text(
                    text = "相册",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Floating circular camera button.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 32.dp)
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = !selectionMode) { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "拍摄",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color(0xFF1C1C1E),
                title = { Text("删除所选照片？", color = Color.White) },
                text = { Text("将删除 ${selected.size} 张照片，此操作不可恢复。", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        deleteSelected()
                    }) { Text("删除", color = Color(0xFFFFD60A)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = Color.White) }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryGridItem(
    photo: GalleryPhoto,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, photo.uri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(photo.uri, Size(240, 240), null)
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1C1C1E))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD60A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/** Map a touch position (in the grid's viewport coordinates) to the grid item index. */
private fun hitTestGridItem(gridState: LazyGridState, position: Offset): Int? {
    val layoutInfo = gridState.layoutInfo
    val viewportStart = layoutInfo.viewportStartOffset
    for (item in layoutInfo.visibleItemsInfo) {
        val left = item.offset.x.toFloat()
        val top = (item.offset.y - viewportStart).toFloat()
        val right = left + item.size.width
        val bottom = top + item.size.height
        if (position.x in left..right && position.y in top..bottom) {
            return item.index
        }
    }
    return null
}

/** Query all photos saved by the app (MediaStore Pictures/ColorPick), newest first. */
private fun queryColorPickPhotos(context: Context): List<GalleryPhoto> {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED
    )
    val useRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val selection = if (useRelativePath) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    } else {
        "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?"
    }
    val selectionArgs = if (useRelativePath) {
        arrayOf("Pictures/ColorPick%", "Pictures/ColorBy%")
    } else {
        arrayOf("%/ColorPick/%", "%/ColorBy/%")
    }
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    val result = mutableListOf<GalleryPhoto>()
    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val date = cursor.getLong(dateCol)
                result.add(
                    GalleryPhoto(
                        uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        dateAdded = date
                    )
                )
            }
        }
    } catch (_: Exception) {
    }
    return result
}

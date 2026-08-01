package com.example.colorpick.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaActionSound
import android.media.MediaScannerConnection
import android.util.Log
import android.util.Size
import android.view.HapticFeedbackConstants
import android.content.res.Configuration
import android.view.Surface
import android.view.View
import androidx.exifinterface.media.ExifInterface
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import android.util.Range
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.colorpick.gpu.CameraFrameRenderer
import com.example.colorpick.gpu.OffscreenGlRenderer
import com.example.colorpick.gpu.RealTimeCameraView
import com.example.colorpick.ui.camera.CameraViewModel
import com.example.colorpick.ui.editor.AdjustmentParams
import com.example.colorpick.ui.editor.FilterPreset
import com.example.colorpick.ui.editor.IOS_FILTERS
import com.example.colorpick.ui.editor.SquareColorPanel
import com.example.colorpick.ui.editor.hslHue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Shift the viewfinder window upward slightly to leave more room for the bottom panel.
private val VIEWFINDER_TOP_OFFSET_DP = 24.dp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onNavigateToCrop: (Uri) -> Unit = {},
    onNavigateToGallery: () -> Unit = {}
) {
    val permissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ needs this to query MediaStore (gallery scanning).
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    if (permissionState.allPermissionsGranted) {
        CameraContent(
            onNavigateToCrop = onNavigateToCrop,
            onNavigateToGallery = onNavigateToGallery
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "需要相机与存储权限",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private enum class AspectRatio {
    P1_1, P4_3, P16_9;

    fun displayTitle(isPortrait: Boolean): String = when (this) {
        P1_1 -> "1:1"
        P4_3 -> if (isPortrait) "3:4" else "4:3"
        P16_9 -> if (isPortrait) "9:16" else "16:9"
    }

    fun displayRatio(isPortrait: Boolean): Float = when (this) {
        P1_1 -> 1f
        P4_3 -> if (isPortrait) 3f / 4f else 4f / 3f
        P16_9 -> if (isPortrait) 9f / 16f else 16f / 9f
    }
}

private enum class FlashMode {
    AUTO, ON, OFF
}

@Composable
private fun CameraContent(
    onNavigateToCrop: (Uri) -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val rootView = LocalView.current
    val shutterSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
            captureExecutor.shutdown()
        }
    }

    val viewModel: CameraViewModel = viewModel()
    val params by viewModel.params.collectAsState()
    val squarePanelOffset by viewModel.squarePanelOffset.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val filterIntensity by viewModel.filterIntensity.collectAsState()

    var showSixDotMenu by remember { mutableStateOf(false) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var filterSelectionMode by remember { mutableStateOf(false) }
    var filterCardsMinimized by remember { mutableStateOf(false) }

    // Filter baseline is applied directly in the shader; palette offsets are added on top.
    val resolvedParams = remember(params, selectedFilter, filterSelectionMode, filterIntensity) {
        if (filterSelectionMode && selectedFilter.id != "standard") {
            selectedFilter.params.scaled(filterIntensity / 100f) + params
        } else {
            params
        }
    }

    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        lastPhotoUri = queryLastPhotoUri(context)
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var aspectRatio by remember { mutableStateOf(AspectRatio.P4_3) }

    // Filter mode is always locked to 3:4 so the filter cards and bottom panel keep a stable layout.
    LaunchedEffect(filterSelectionMode) {
        if (filterSelectionMode) {
            aspectRatio = AspectRatio.P4_3
        }
    }

    val imageCapture = remember {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    androidx.camera.core.AspectRatio.RATIO_4_3,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )
            .build()
        ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setJpegQuality(100)
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = when (flashMode) {
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        (context as? android.app.Activity)?.windowManager?.defaultDisplay
    }
    val screenRotation = display?.rotation ?: Surface.ROTATION_0

    LaunchedEffect(screenRotation) {
        imageCapture.targetRotation = screenRotation
    }

    var cameraFrameRenderer by remember { mutableStateOf<CameraFrameRenderer?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    var focusPosition by remember { mutableStateOf<Offset?>(null) }
    var isFocusLocked by remember { mutableStateOf(false) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    var exposureCompensationIndex by remember { mutableIntStateOf(0) }
    var previewSize by remember { mutableStateOf(Size(0, 0)) }
    val zoomAnimatable = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var isSwitchingLens by remember { mutableStateOf(false) }

    LaunchedEffect(lensFacing) {
        zoomAnimatable.snapTo(1f)
        cameraControl?.setZoomRatio(1f)
    }

    DisposableEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(1080, 1440))
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        val frameRotation = imageProxy.imageInfo.rotationDegrees
                        // Front-camera frames from CameraX are mirrored by the renderer, but the
                        // reported rotation is relative to the mirrored sensor; flip it so the
                        // preview appears upright on screen.
                        val adjustedRotation = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            ((360 - frameRotation) % 360)
                        } else {
                            frameRotation
                        }
                        rotationDegrees = adjustedRotation
                        val renderer = cameraFrameRenderer
                        renderer?.let {
                            try {
                                it.setRotation(adjustedRotation)
                                val planes = imageProxy.planes
                                it.updateFrame(
                                    planes[0].buffer, planes[1].buffer, planes[2].buffer,
                                    imageProxy.width, imageProxy.height,
                                    planes[0].rowStride, planes[1].rowStride, planes[2].rowStride,
                                    planes[1].pixelStride.coerceAtLeast(1)
                                )
                            } catch (e: Exception) {
                                Log.e("CameraContent", "Frame processing failed", e)
                            }
                        }
                        imageProxy.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture,
                    imageAnalyzer
                )
                cameraControl = camera?.cameraControl
                // Keep the mask long enough to cover the bind and the first few frames
                // so the user never sees the brief freeze or orientation flip.
                scope.launch {
                    delay(700)
                    isSwitchingLens = false
                }
            } catch (e: Exception) {
                Log.e("CameraContent", "Camera bind failed", e)
                isSwitchingLens = false
            }
        }, executor)
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    val onTakePhoto = {
        rootView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        shutterSound.play(MediaActionSound.SHUTTER_CLICK)
        takePhoto(
            context,
            imageCapture,
            executor,
            captureExecutor,
            resolvedParams,
            aspectRatio,
            isPortrait
        ) { uri ->
            lastPhotoUri = uri
        }
    }

    val previewRotation = rotationDegrees

    var zoomAnimationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val handleZoomChange: (Float) -> Unit = { newZoom ->
        zoomAnimationJob?.cancel()
        zoomAnimationJob = scope.launch { zoomAnimatable.snapTo(newZoom) }
        cameraControl?.setZoomRatio(newZoom)
    }

    val animateZoomTo: (Float) -> Unit = { target ->
        val zoomState = camera?.cameraInfo?.zoomState?.value
        val minZoom = zoomState?.minZoomRatio ?: 1f
        val maxZoom = zoomState?.maxZoomRatio ?: 3f
        val coerced = target.coerceIn(minZoom, maxZoom)
        // Animate both the UI badge and the camera zoom ratio together to keep them
        // in sync and avoid the previous hard cut between presets.
        zoomAnimationJob?.cancel()
        zoomAnimationJob = scope.launch {
            zoomAnimatable.animateTo(coerced, animationSpec = tween(200)) {
                cameraControl?.setZoomRatio(this.value)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Control layer: default camera UI with optional filter overlay.
        CameraControls(
            aspectRatio = aspectRatio,
            isPortrait = isPortrait,
            flashMode = flashMode,
            lastPhotoUri = lastPhotoUri,
            showSixDotMenu = showSixDotMenu,
            showAspectRatioMenu = showAspectRatioMenu,
            onAspectRatioClick = { showAspectRatioMenu = true },
            onFlashClick = {
                flashMode = when (flashMode) {
                    FlashMode.OFF -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.OFF
                }
            },
            onSwitchCamera = {
                isSwitchingLens = true
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            onMenuClick = { showSixDotMenu = true },
            onShutterClick = onTakePhoto,
            onOpenGallery = onNavigateToGallery,
            camera = camera,
            cameraControl = cameraControl,
            previewSize = previewSize,
            showGrid = showGrid,
            focusPosition = focusPosition,
            isFocusLocked = isFocusLocked,
            showFocusIndicator = showFocusIndicator,
            exposureCompensationIndex = exposureCompensationIndex,
            onFocusPositionChange = { focusPosition = it },
            onFocusLockedChange = { isFocusLocked = it },
            onShowFocusIndicatorChange = { showFocusIndicator = it },
            onExposureCompensationIndexChange = { exposureCompensationIndex = it },
            onPreviewSizeChange = { previewSize = it },
            zoomRatio = zoomAnimatable.value,
            onZoomChange = handleZoomChange,
            onZoomPresetSelected = animateZoomTo,
            filterSelectionMode = filterSelectionMode,
            filterCardsMinimized = filterCardsMinimized,
            viewModel = viewModel,
            onCloseFilter = {
                viewModel.resetToDefault()
                filterSelectionMode = false
                filterCardsMinimized = false
            },
            onMinimizeFilterCards = { filterCardsMinimized = !filterCardsMinimized },
            params = resolvedParams,
            previewRotation = previewRotation,
            isMirrored = lensFacing == CameraSelector.LENS_FACING_FRONT,
            showLensSwitchMask = isSwitchingLens,
            onRendererCreated = { renderer ->
                cameraFrameRenderer = renderer
            }
        )

        // Popover / menu layers sit above everything else.
        AnimatedVisibility(
            visible = showSixDotMenu,
            enter = fadeIn(tween(200)) + slideInVertically { -it },
            exit = fadeOut(tween(200)) + slideOutVertically { -it }
        ) {
            SixDotMenu(
                showGrid = showGrid,
                isFilterActive = filterSelectionMode,
                onStyleClick = {
                    showSixDotMenu = false
                    if (filterSelectionMode) {
                        viewModel.resetToDefault()
                        filterSelectionMode = false
                        filterCardsMinimized = false
                    } else {
                        filterSelectionMode = true
                        filterCardsMinimized = false
                    }
                },
                onGridToggle = { showGrid = !showGrid },
                onDismiss = { showSixDotMenu = false }
            )
        }

        AnimatedVisibility(
            visible = showAspectRatioMenu,
            enter = fadeIn(tween(200)) + slideInVertically { -it },
            exit = fadeOut(tween(180)) + slideOutVertically { -it }
        ) {
            AspectRatioDropdown(
                selectedAspectRatio = aspectRatio,
                isPortrait = isPortrait,
                onAspectRatioSelected = { aspectRatio = it },
                onDismiss = { showAspectRatioMenu = false }
            )
        }
    }
}

@Composable
private fun ViewfinderOverlay(
    aspectRatio: AspectRatio,
    isPortrait: Boolean,
    maskAlpha: Float,
    verticalOffset: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { verticalOffset.toPx() }
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val targetRatio = aspectRatio.displayRatio(isPortrait)
        val viewfinderW: Float
        val viewfinderH: Float
        if (w / h > targetRatio) {
            viewfinderH = h
            viewfinderW = h * targetRatio
        } else {
            viewfinderW = w
            viewfinderH = w / targetRatio
        }
        val left = (w - viewfinderW) / 2f
        val top = ((h - viewfinderH) / 2f - offsetPx).coerceAtLeast(0f)
        val right = left + viewfinderW
        val bottom = top + viewfinderH

        val maskColor = Color.Black.copy(alpha = maskAlpha)
        // Top
        drawRect(maskColor, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, top))
        // Bottom
        drawRect(maskColor, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(w, h - bottom))
        // Left
        drawRect(maskColor, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, viewfinderH))
        // Right
        drawRect(maskColor, topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(w - right, viewfinderH))
    }
}

@Composable
private fun ViewfinderTapHandler(
    cameraControl: CameraControl?,
    previewSize: Size,
    focusPosition: Offset?,
    isFocusLocked: Boolean,
    showFocusIndicator: Boolean,
    exposureCompensationIndex: Int,
    onFocusPositionChange: (Offset?) -> Unit,
    onFocusLockedChange: (Boolean) -> Unit,
    onShowFocusIndicatorChange: (Boolean) -> Unit,
    onExposureCompensationIndexChange: (Int) -> Unit,
    autoHideDelayMs: Long = 3000,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Use updated-state wrappers so the gesture detector is not recreated every time
    // the camera, preview size or focus lock changes. This fixes missed taps caused by
    // pointerInput recomposition resets.
    val currentCameraControl by rememberUpdatedState(cameraControl)
    val currentPreviewSize by rememberUpdatedState(previewSize)
    val currentIsFocusLocked by rememberUpdatedState(isFocusLocked)

    fun scheduleAutoHide() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(autoHideDelayMs)
            onShowFocusIndicatorChange(false)
        }
    }

    fun cancelAutoHide() {
        hideJob?.cancel()
        hideJob = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        cancelAutoHide()
                        performFocus(
                            currentCameraControl,
                            currentPreviewSize,
                            offset.x,
                            offset.y,
                            lock = true
                        )
                        onFocusPositionChange(offset)
                        onShowFocusIndicatorChange(true)
                        onFocusLockedChange(true)
                        onExposureCompensationIndexChange(0)
                        currentCameraControl?.setExposureCompensationIndex(0)
                    },
                    onTap = { offset ->
                        if (currentIsFocusLocked) {
                            onFocusLockedChange(false)
                        }
                        performFocus(
                            currentCameraControl,
                            currentPreviewSize,
                            offset.x,
                            offset.y,
                            lock = false
                        )
                        onFocusPositionChange(offset)
                        onShowFocusIndicatorChange(true)
                        scheduleAutoHide()
                        onExposureCompensationIndexChange(0)
                        currentCameraControl?.setExposureCompensationIndex(0)
                    }
                )
            }
    )
}

@Composable
private fun FocusExposureUI(
    camera: Camera?,
    cameraControl: CameraControl?,
    previewSize: Size,
    focusPosition: Offset?,
    isFocusLocked: Boolean,
    showFocusIndicator: Boolean,
    exposureCompensationIndex: Int,
    onExposureCompensationIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val frameSize = 80.dp
    val frameHalfPx = with(density) { frameSize.toPx() / 2f }
    val edgeMargin = with(density) { 12.dp.toPx() }

    val clampedFocusPosition = remember(focusPosition, previewSize.width, previewSize.height) {
        focusPosition?.let { pos ->
            if (previewSize.width <= 0 || previewSize.height <= 0) return@let pos
            Offset(
                x = pos.x.coerceIn(frameHalfPx + edgeMargin, previewSize.width - frameHalfPx - edgeMargin),
                y = pos.y.coerceIn(frameHalfPx + edgeMargin, previewSize.height - frameHalfPx - edgeMargin)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FocusIndicator(
            position = clampedFocusPosition,
            locked = isFocusLocked,
            visible = showFocusIndicator
        )

        val exposureRange = camera?.cameraInfo?.exposureState?.exposureCompensationRange
        if (exposureRange != null && exposureRange.lower < exposureRange.upper && clampedFocusPosition != null) {
            val sliderWidth = 36.dp
            val sliderHeight = 96.dp
            val sliderMargin = 16.dp
            val sliderWidthPx = with(density) { sliderWidth.toPx() }
            val sliderHeightPx = with(density) { sliderHeight.toPx() }
            val sliderMarginPx = with(density) { sliderMargin.toPx() }

            val pos = clampedFocusPosition
            val rightX = pos.x + frameHalfPx + sliderMarginPx
            val sliderX = if (rightX + sliderWidthPx <= previewSize.width - edgeMargin) {
                rightX
            } else {
                pos.x - frameHalfPx - sliderMarginPx - sliderWidthPx
            }
            val sliderY = (pos.y - sliderHeightPx / 2f).coerceIn(
                edgeMargin,
                previewSize.height - sliderHeightPx - edgeMargin
            )

            ExposureSlider(
                exposureRange = exposureRange.lower..exposureRange.upper,
                currentIndex = exposureCompensationIndex,
                onIndexChange = {
                    onExposureCompensationIndexChange(it)
                    try {
                        cameraControl?.setExposureCompensationIndex(it)
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Exposure compensation failed", e)
                    }
                },
                locked = isFocusLocked,
                modifier = Modifier
                    .offset { IntOffset(sliderX.roundToInt(), sliderY.roundToInt()) }
                    .size(sliderWidth, sliderHeight)
            )
        }
    }
}

@Composable
private fun FilterCardPager(
    filters: List<FilterPreset>,
    selectedFilter: FilterPreset,
    onFilterSelected: (FilterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIdx = filters.indexOfFirst { it.id == selectedFilter.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIdx, pageCount = { filters.size })

    // Sync pager scroll -> filter selection immediately when the page crosses the
    // halfway point so the UI feels responsive.
    LaunchedEffect(pagerState.currentPage) {
        val filter = filters.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (filter.id != selectedFilter.id) {
            onFilterSelected(filter)
        }
    }

    // Sync external selection -> pager scroll, but only when the pager is not already
    // on the target page to avoid bounce/flash.
    LaunchedEffect(selectedFilter) {
        val target = filters.indexOfFirst { it.id == selectedFilter.id }.coerceAtLeast(0)
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                target,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 250f)
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(0.dp),
        pageSpacing = 0.dp,
        modifier = modifier
    ) { page ->
        val filter = filters[page]
        val isSelected = filter.id == selectedFilter.id
        // Colorless frosted glass: the filter's character comes from the graded image,
        // not from tinted glass, so the background uses only neutral white/black alphas.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(25.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isSelected) 0.06f else 0.03f),
                            Color.White.copy(alpha = if (isSelected) 0.03f else 0.015f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(25.dp)
                )
        )
    }
}

@Composable
private fun FilterCardsOverlay(
    viewModel: CameraViewModel,
    minimized: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // Only the glass filter card slides; the toggle button is rendered
        // separately above the zoom control so it remains tappable.
        // Showing cards slides up from the bottom; hiding slides them down.
        AnimatedVisibility(
            visible = !minimized,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FilterCardPager(
                    filters = IOS_FILTERS,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedFilter.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    filters: List<FilterPreset>,
    selectedFilter: FilterPreset,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val isSelected = filter.id == selectedFilter.id
            Box(
                modifier = Modifier
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun ColorPickerButton(modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "colorPickerScale"
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .border(2.dp, Color(0xFF3A3A3C), shape)
            .background(Color.White, shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pressed = true
                    val up = waitForUpOrCancellation()
                    pressed = false
                    if (up != null) {
                        // Visual placeholder: real pixel sampling to be added later.
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
    }
}

@Composable
private fun FilterModeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HorizontalIntensitySlider(
    value: Int,
    themeColor: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val baseHue = themeColor.hslHue
    val hue = ((baseHue) % 360f + 360f) % 360f
    val shape = RoundedCornerShape(25.dp)
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.hsl(hue, 0.18f, 0.22f),
            Color.hsl(hue, 0.72f, 0.48f)
        )
    )
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange((fraction * 100).roundToInt().coerceIn(0, 100))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange((fraction * 100).roundToInt().coerceIn(0, 100))
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val thumbSizePx = with(density) { maxHeight.toPx() }

        // Track: black is the empty-value base layer; the themed gradient covers the
        // left portion proportionally (drag right = more gradient, drag left = more black).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value / 100f)
                    .background(brush = gradientBrush)
            )
        }

        // White circular knob: diameter matches the track height (and its rounded ends),
        // sliding within the track bounds, vertically centered.
        Box(
            modifier = Modifier
                .size(with(density) { thumbSizePx.toDp() })
                .offset {
                    IntOffset(
                        ((trackWidthPx - thumbSizePx) * value / 100f).roundToInt(),
                        0
                    )
                }
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun VerticalIntensitySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(Color(0xFF1C1C1E))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(25.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        onValueChange((fraction * 100).roundToInt().coerceIn(0, 100))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        onValueChange((fraction * 100).roundToInt().coerceIn(0, 100))
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value / 100f)
                .background(Color.White.copy(alpha = 0.85f))
        )
    }
}

@Composable
private fun TopControlBar(
    aspectRatio: AspectRatio,
    isPortrait: Boolean,
    flashMode: FlashMode,
    onAspectRatioClick: () -> Unit,
    onFlashClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    onMenuClick: () -> Unit,
    isAspectRatioMenuOpen: Boolean = false,
    isSixDotMenuOpen: Boolean = false,
    filterSelectionMode: Boolean = false,
    paletteInfo: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (!filterSelectionMode) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = aspectRatio.displayTitle(isPortrait),
                    color = if (isAspectRatioMenuOpen) Color(0xFFFFD60A) else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAspectRatioClick
                        )
                        .background(if (isAspectRatioMenuOpen) Color(0xFFFFD60A).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (filterSelectionMode && !paletteInfo.isNullOrEmpty()) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = paletteInfo,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!filterSelectionMode) {
                TopBarIconButton(onClick = onFlashClick) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                            FlashMode.OFF -> Icons.Default.FlashOff
                        },
                        contentDescription = "闪光灯",
                        tint = if (flashMode == FlashMode.OFF) Color.White else Color(0xFFFFD60A),
                        modifier = Modifier.size(22.dp)
                    )
                }
                TopBarIconButton(onClick = onSwitchCamera) {
                    Icon(Icons.Default.Cameraswitch, "翻转镜头", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            TopBarIconButton(onClick = onMenuClick) {
                SixDotsIcon(tint = if (isSixDotMenuOpen) Color(0xFFFFD60A) else Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BottomControlBar(
    lastPhotoUri: Uri?,
    onShutterClick: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The thumbnail opens the app-internal gallery instead of the system gallery.
        ThumbnailView(uri = lastPhotoUri, onClick = onOpenGallery)

        ShutterButton(onClick = onShutterClick, size = 76.dp)

        // Spacer keeps the shutter visually centered by mirroring the thumbnail slot.
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun BottomControlPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun CameraControls(
    aspectRatio: AspectRatio,
    isPortrait: Boolean,
    flashMode: FlashMode,
    lastPhotoUri: Uri?,
    showSixDotMenu: Boolean,
    showAspectRatioMenu: Boolean,
    onAspectRatioClick: () -> Unit,
    onFlashClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    onMenuClick: () -> Unit,
    onShutterClick: () -> Unit,
    onOpenGallery: () -> Unit,
    camera: Camera?,
    cameraControl: CameraControl?,
    previewSize: Size,
    showGrid: Boolean,
    focusPosition: Offset?,
    isFocusLocked: Boolean,
    showFocusIndicator: Boolean,
    exposureCompensationIndex: Int,
    onFocusPositionChange: (Offset?) -> Unit,
    onFocusLockedChange: (Boolean) -> Unit,
    onShowFocusIndicatorChange: (Boolean) -> Unit,
    onExposureCompensationIndexChange: (Int) -> Unit,
    onPreviewSizeChange: (Size) -> Unit,
    zoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    onZoomPresetSelected: (Float) -> Unit,
    filterSelectionMode: Boolean,
    filterCardsMinimized: Boolean,
    viewModel: CameraViewModel,
    onCloseFilter: () -> Unit,
    onMinimizeFilterCards: () -> Unit,
    params: AdjustmentParams,
    previewRotation: Int,
    isMirrored: Boolean,
    showLensSwitchMask: Boolean,
    onRendererCreated: (CameraFrameRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPinching by remember { mutableStateOf(false) }
    // Wheel visibility is hoisted here so BOTH the zoom-bar gesture strip and the
    // full-viewfinder pinch handler can reset it when fingers lift. The strip alone
    // can miss the lift event after it relayouts while expanding the wheel, which
    // used to leave the wheel stuck on screen until the next tap.
    var showWheel by remember { mutableStateOf(false) }
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val density = LocalDensity.current
    val viewfinderRatio = aspectRatio.displayRatio(isPortrait)
    val paletteInfo = if (filterSelectionMode && selectedFilter.id != "standard") {
        "${viewModel.brightnessLabel()}   ${viewModel.saturationLabel()}   ${viewModel.intensityLabel()}"
    } else null
    val viewfinderOffsetPx = with(density) { VIEWFINDER_TOP_OFFSET_DP.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Full-screen pointer tracker: closes the wheel the moment every finger
            // lifts, no matter which gesture handler owns the touch. The zoom-bar
            // gesture strip can miss the lift event after relaying out while expanding
            // the wheel, so this layout-stable full-screen tracker is the reliable
            // guarantee that the wheel can never stay stuck after release.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    try {
                        while (true) {
                            awaitPointerEvent()
                        }
                    } finally {
                        // Runs on every exit: normal lift or cancellation when all
                        // fingers go up — the wheel is always dismissed.
                        showWheel = false
                    }
                }
            }
    ) {
        // Pure black mask blocks around the viewfinder serve as the top/bottom bars.
        ViewfinderOverlay(
            aspectRatio = aspectRatio,
            isPortrait = isPortrait,
            maskAlpha = 1f,
            verticalOffset = VIEWFINDER_TOP_OFFSET_DP,
            modifier = Modifier.fillMaxSize()
        )

        var viewfinderSize by remember { mutableStateOf(Size(0, 0)) }
        val zoomRatioState = rememberUpdatedState(zoomRatio)

        // Viewfinder window: camera preview, focus, filter cards, lens mask.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(viewfinderRatio)
                .align(Alignment.Center)
                .offset { IntOffset(0, -viewfinderOffsetPx.roundToInt()) }
                .onGloballyPositioned { coordinates ->
                    val size = Size(coordinates.size.width, coordinates.size.height)
                    viewfinderSize = size
                    onPreviewSizeChange(size)
                }
                .pointerInput(camera, cameraControl, filterSelectionMode, filterCardsMinimized) {
                    awaitEachGesture {
                        // Always wait for a real gesture first: returning before any
                        // suspension would make awaitEachGesture's internal loop spin
                        // forever and freeze the UI thread. requireUnconsumed = false so
                        // the tap-to-focus handler consuming the first down cannot prevent
                        // the two-finger pinch from starting.
                        awaitFirstDown(requireUnconsumed = false)
                        // Filter cards cover the viewfinder; pinch-zoom is disabled there.
                        if (filterSelectionMode && !filterCardsMinimized) return@awaitEachGesture

                        val cameraInfo = camera?.cameraInfo
                        val zoomState = cameraInfo?.zoomState?.value
                        val minZoom = zoomState?.minZoomRatio ?: 1f
                        val maxZoom = zoomState?.maxZoomRatio ?: 10f
                        val baseZoom = zoomRatioState.value.coerceIn(minZoom, maxZoom)

                        // Track the finger separation directly instead of calculateZoom():
                        // the zoom bar's gesture strip consumes pointer changes inside its
                        // area, which would make calculateZoom() return 1f and break the
                        // pinch whenever the gesture starts over the zoom bar.
                        var startDistance = 0f
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.size >= 2) {
                                    val dx = pressed[0].position.x - pressed[1].position.x
                                    val dy = pressed[0].position.y - pressed[1].position.y
                                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                    if (startDistance == 0f) {
                                        startDistance = distance
                                    } else if (startDistance > 0f) {
                                        val newZoom = (baseZoom * (distance / startDistance)).coerceIn(minZoom, maxZoom)
                                        onZoomChange(newZoom)
                                    }
                                    isPinching = true
                                }
                                if (event.changes.none { it.pressed }) break
                            }
                        } finally {
                            // Restore the default three-icon preset bar as soon as the
                            // fingers lift. This must run even when awaitEachGesture
                            // cancels this block the moment the last finger lifts;
                            // otherwise isPinching stays true and the preset bar never
                            // comes back until the user taps the screen again.
                            isPinching = false
                            // Redundant wheel reset: the full-viewfinder gesture reliably
                            // receives every lift event even if the zoom-bar strip lost
                            // them after relaying out, so the wheel can never stay stuck.
                            showWheel = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            RealTimeCameraView(
                params = params,
                rotationDegrees = previewRotation,
                isMirrored = isMirrored,
                viewportWidth = viewfinderSize.width,
                viewportHeight = viewfinderSize.height,
                onRendererCreated = onRendererCreated,
                modifier = Modifier.fillMaxSize()
            )

            GridOverlay(enabled = showGrid, modifier = Modifier.fillMaxSize())

            ViewfinderTapHandler(
                cameraControl = cameraControl,
                previewSize = previewSize,
                focusPosition = focusPosition,
                isFocusLocked = isFocusLocked,
                showFocusIndicator = showFocusIndicator,
                exposureCompensationIndex = exposureCompensationIndex,
                onFocusPositionChange = onFocusPositionChange,
                onFocusLockedChange = onFocusLockedChange,
                onShowFocusIndicatorChange = onShowFocusIndicatorChange,
                onExposureCompensationIndexChange = onExposureCompensationIndexChange,
                modifier = Modifier.fillMaxSize()
            )

            if (filterSelectionMode) {
                FilterCardsOverlay(
                    viewModel = viewModel,
                    minimized = filterCardsMinimized,
                    onClose = onCloseFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }

            FocusExposureUI(
                camera = camera,
                cameraControl = cameraControl,
                previewSize = previewSize,
                focusPosition = focusPosition,
                isFocusLocked = isFocusLocked,
                showFocusIndicator = showFocusIndicator,
                exposureCompensationIndex = exposureCompensationIndex,
                onExposureCompensationIndexChange = onExposureCompensationIndexChange,
                modifier = Modifier.fillMaxSize()
            )

            LensSwitchMask(visible = showLensSwitchMask)
        }

        // Floating bottom controls: fixed position from the bottom, independent of aspect ratio.
        // Rendered before the zoom overlay so the wheel's semi-transparent black background
        // can cover it while the user is dragging to zoom.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (filterSelectionMode) {
                FilterModeBottomPanel(
                    viewModel = viewModel,
                    onShutterClick = onShutterClick,
                    onCloseFilter = onCloseFilter,
                    filterCardsMinimized = filterCardsMinimized
                )
            } else {
                BottomControlBar(
                    lastPhotoUri = lastPhotoUri,
                    onShutterClick = onShutterClick,
                    onOpenGallery = onOpenGallery
                )
            }
        }

        // Shared geometry layer for the zoom bar, pinch badge and filter minimize button.
        // All three are positioned relative to the viewfinder bounds so they stay fixed
        // and independent of each other. This layer is drawn after the bottom controls so
        // the zoom wheel can cover the bottom bar while active.
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // Reference the scope's Dp members directly (outside nested lambdas) so the
            // Compose lint check "BoxWithConstraints scope is not used" stays satisfied.
            val maxW = maxWidth
            val maxH = maxHeight
            val parentW = with(density) { maxW.toPx() }
            val parentH = with(density) { maxH.toPx() }

            // Actual viewfinder bounds (used to pin floating elements).
            val actualViewfinderW = if (parentW / parentH > viewfinderRatio) parentH * viewfinderRatio else parentW
            val actualViewfinderH = if (parentW / parentH > viewfinderRatio) parentH else parentW / viewfinderRatio
            val actualViewfinderTop = (parentH - actualViewfinderH) / 2f - viewfinderOffsetPx
            val actualViewfinderBottom = actualViewfinderTop + actualViewfinderH
            val actualViewfinderLeft = (parentW - actualViewfinderW) / 2f
            val actualViewfinderRight = actualViewfinderLeft + actualViewfinderW

            // Zoom bar bottom edge: follow the actual viewfinder for 1:1 and 4:3,
            // stay fixed at the 4:3 bottom line for long aspect ratios. The 4:3 line
            // is computed with the same upward viewfinder offset so it matches exactly
            // where the bar sits when the 3:4 aspect ratio is actually selected.
            val isLongAspectRatio = viewfinderRatio < 0.65f
            val zoomBarBottomY = if (isLongAspectRatio) {
                val p4_3Ratio = AspectRatio.P4_3.displayRatio(isPortrait)
                val p4_3H = if (parentW / parentH > p4_3Ratio) parentH else parentW / p4_3Ratio
                val p4_3Top = (parentH - p4_3H) / 2f - viewfinderOffsetPx
                p4_3Top + p4_3H
            } else {
                actualViewfinderBottom
            }

            val badgeHeightPx = with(density) { 40.dp.toPx() }
            val badgeBottomPaddingPx = with(density) { 8.dp.toPx() }

            // Wheel sizing: the disk diameter equals the full-width mask rectangle (and
            // the bottom bar width). The disk center sits at the viewfinder bottom edge.
            val wheelRadiusPx = parentW / 2f
            val wheelTopPx = (zoomBarBottomY - wheelRadiusPx).coerceAtLeast(0f)
            val wheelHeightPx = parentH - wheelTopPx
            val wheelBaselineY = zoomBarBottomY - wheelTopPx

            // Zoom control: preset bar or wheel. Pinch badge is rendered separately.
            if (!filterSelectionMode || filterCardsMinimized) {
                ZoomControl(
                    currentZoom = zoomRatio,
                    onZoomChange = onZoomChange,
                    onZoomPresetSelected = onZoomPresetSelected,
                    camera = camera,
                    isPinching = isPinching,
                    showWheel = showWheel,
                    onShowWheelChange = { showWheel = it },
                    contentBaselineY = wheelBaselineY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { wheelHeightPx.toDp() })
                        .offset { IntOffset(0, wheelTopPx.roundToInt()) }
                )
            }

            // Filter-mode shutter (non-standard filters): pinned to the viewfinder bottom
            // edge and horizontally centered so it lines up with the palette in the bottom
            // bar. While the filter cards are minimized the shutter moves back to the
            // bottom bar (inside FilterModeBottomPanel) so the zoom bar stays reachable.
            if (filterSelectionMode && selectedFilter.id != "standard" && !filterCardsMinimized) {
                val shutterSizePx = with(density) { 76.dp.toPx() }
                val shutterBottomPaddingPx = with(density) { 4.dp.toPx() }
                ShutterButton(
                    onClick = onShutterClick,
                    size = 76.dp,
                    modifier = Modifier.offset {
                        IntOffset(
                            ((parentW - shutterSizePx) / 2f).roundToInt(),
                            (actualViewfinderBottom - shutterSizePx - shutterBottomPaddingPx).roundToInt()
                        )
                    }
                )
            }

            // Pinch zoom badge: fixed centered above the zoom bar, independent of ZoomControl internals.
            AnimatedVisibility(
                visible = isPinching,
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(120)),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            0,
                            (zoomBarBottomY - badgeHeightPx - badgeBottomPaddingPx).roundToInt()
                        )
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomCollapsedBadge(currentZoom = zoomRatio)
                }
            }

            // Filter cards minimize/restore button: pinned to the actual viewfinder bottom-right,
            // rendered above the zoom control so it always receives taps.
            if (filterSelectionMode) {
                val buttonSizePx = with(density) { 40.dp.toPx() }
                val buttonEndPaddingPx = with(density) { 16.dp.toPx() }
                val buttonBottomPaddingPx = with(density) { 16.dp.toPx() }
                FilterModeActionButton(
                    icon = if (filterCardsMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (filterCardsMinimized) "放大" else "缩小",
                    onClick = onMinimizeFilterCards,
                    size = 40.dp,
                    modifier = Modifier.offset {
                        IntOffset(
                            (actualViewfinderRight - buttonEndPaddingPx - buttonSizePx).roundToInt(),
                            (actualViewfinderBottom - buttonBottomPaddingPx - buttonSizePx).roundToInt()
                        )
                    }
                )
            }
        }

        // Floating top controls: fixed position from the top, independent of aspect ratio.
        // Drawn last so the zoom wheel never covers it.
        TopControlBar(
            aspectRatio = aspectRatio,
            isPortrait = isPortrait,
            flashMode = flashMode,
            onAspectRatioClick = onAspectRatioClick,
            onFlashClick = onFlashClick,
            onSwitchCamera = onSwitchCamera,
            onMenuClick = onMenuClick,
            isAspectRatioMenuOpen = showAspectRatioMenu,
            isSixDotMenuOpen = showSixDotMenu,
            filterSelectionMode = filterSelectionMode,
            paletteInfo = paletteInfo,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun LensSwitchMask(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        // Fade in almost instantly so the mask appears the moment the user taps switch,
        // covering any brief freeze before the camera re-binds.
        enter = fadeIn(tween(50)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (visible) 360f else 0f,
                animationSpec = tween(700),
                label = "lensSwitchRotation"
            )
            val scale by animateFloatAsState(
                targetValue = if (visible) 1f else 0.6f,
                animationSpec = tween(400),
                label = "lensSwitchScale"
            )
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "切换镜头",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val shutterScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "shutterScale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = shutterScale; scaleY = shutterScale }
            .clip(CircleShape)
            .border(5.dp, Color.White, CircleShape)
            .padding(4.dp)
            .background(Color.White, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { isPressed = true; onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size - 16.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color.LightGray.copy(alpha = 0.4f), CircleShape)
        )
    }
    LaunchedEffect(isPressed) { if (isPressed) { kotlinx.coroutines.delay(120); isPressed = false } }
}

@Composable
private fun FilterModeBottomPanel(
    viewModel: CameraViewModel,
    onShutterClick: () -> Unit,
    onCloseFilter: () -> Unit,
    filterCardsMinimized: Boolean,
    modifier: Modifier = Modifier
) {
    val squarePanelOffset by viewModel.squarePanelOffset.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val filterIntensity by viewModel.filterIntensity.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        FilterModeActionButton(
            icon = Icons.Default.Refresh,
            contentDescription = "重置",
            onClick = { viewModel.resetSquarePanel(); viewModel.setFilterIntensity(100) },
            size = 40.dp,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        if (selectedFilter.id != "standard") {
            val paletteSize = 100.dp
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SquareColorPanel(
                        offset = squarePanelOffset,
                        themeColor = selectedFilter.themeColor,
                        onOffsetChange = { viewModel.updateSquarePanel(it) },
                        onReset = { viewModel.resetSquarePanel() },
                        modifier = Modifier.size(paletteSize)
                    )
                    HorizontalIntensitySlider(
                        value = filterIntensity,
                        themeColor = selectedFilter.themeColor,
                        onValueChange = { viewModel.setFilterIntensity(it) },
                        modifier = Modifier
                            .width(paletteSize)
                            .height(20.dp)
                    )
                }

                // With the filter cards expanded the shutter lives on the viewfinder
                // bottom edge (rendered in CameraControls); it only shows here next to
                // the palette while the cards are minimized so the zoom bar is usable.
                if (filterCardsMinimized) {
                    ShutterButton(
                        onClick = onShutterClick,
                        size = 76.dp
                    )
                }
            }
        } else {
            // Standard style hides the palette/intensity slider but keeps the same shutter.
            ShutterButton(
                onClick = onShutterClick,
                size = 76.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        FilterModeActionButton(
            icon = Icons.Default.Close,
            contentDescription = "关闭",
            onClick = onCloseFilter,
            size = 40.dp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun ThumbnailView(uri: Uri?, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbnail = remember(uri) { uri?.let { loadThumbnail(context, it) } }
    Box(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            androidx.compose.foundation.Image(bitmap = thumbnail.asImageBitmap(), contentDescription = "最近照片", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
    }
}

@Composable
private fun FocusIndicator(position: Offset?, locked: Boolean, visible: Boolean) {
    val scale = remember { Animatable(1.3f) }
    LaunchedEffect(position, visible) {
        if (visible && position != null) {
            scale.snapTo(1.3f)
            scale.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 400f))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120))
    ) {
        position?.let { pos ->
            val density = LocalDensity.current
            val frameSize = 80.dp
            val halfSize = with(density) { frameSize.toPx() / 2 }
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (pos.x - halfSize).toDp() },
                        y = with(density) { (pos.y - halfSize).toDp() }
                    )
                    .size(frameSize)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stroke = 2.5f
                    val cornerLength = w * 0.28f
                    val inset = w * 0.12f
                    val color = if (locked) Color(0xFFFFD60A) else Color.White

                    // Top-left
                    drawLine(color, Offset(inset, inset), Offset(inset + cornerLength, inset), stroke)
                    drawLine(color, Offset(inset, inset), Offset(inset, inset + cornerLength), stroke)
                    // Top-right
                    drawLine(color, Offset(w - inset, inset), Offset(w - inset - cornerLength, inset), stroke)
                    drawLine(color, Offset(w - inset, inset), Offset(w - inset, inset + cornerLength), stroke)
                    // Bottom-left
                    drawLine(color, Offset(inset, h - inset), Offset(inset + cornerLength, h - inset), stroke)
                    drawLine(color, Offset(inset, h - inset), Offset(inset, h - inset - cornerLength), stroke)
                    // Bottom-right
                    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset - cornerLength, h - inset), stroke)
                    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset, h - inset - cornerLength), stroke)
                }
            }
        }
    }
}

@Composable
private fun ExposureSlider(
    exposureRange: IntRange,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    locked: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = remember(currentIndex, exposureRange) {
        if (exposureRange.last == exposureRange.first) 0.5f
        else (currentIndex - exposureRange.first).toFloat() / (exposureRange.last - exposureRange.first).toFloat()
    }

    Box(
        modifier = modifier
            .pointerInput(exposureRange) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val f = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        val newIndex = exposureRange.first + (f * (exposureRange.last - exposureRange.first)).roundToInt()
                        onIndexChange(newIndex.coerceIn(exposureRange.first, exposureRange.last))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f

            // Vertical track
            drawLine(Color.White.copy(alpha = 0.45f), Offset(cx, 0f), Offset(cx, h), 2f)

            // Tick marks
            val steps = 5
            for (i in 0..steps) {
                val y = h * i / steps
                drawLine(Color.White.copy(alpha = 0.22f), Offset(cx - 6f, y), Offset(cx + 6f, y), 1f)
            }

            // Handle
            val handleY = (1f - fraction) * h
            val handleColor = if (locked) Color(0xFFFFD60A) else Color.White
            drawCircle(handleColor.copy(alpha = 0.9f), 9f, Offset(cx, handleY))

            // Sun rays around the handle
            val rayCount = 8
            val innerR = 12f
            val outerR = 20f
            for (i in 0 until rayCount) {
                val angle = i * 2f * PI / rayCount
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                drawLine(
                    handleColor.copy(alpha = 0.45f),
                    Offset(cx + cosA * innerR, handleY + sinA * innerR),
                    Offset(cx + cosA * outerR, handleY + sinA * outerR),
                    1.5f
                )
            }
        }
    }
}

@Composable
private fun SixDotMenu(
    showGrid: Boolean,
    isFilterActive: Boolean,
    onStyleClick: () -> Unit,
    onGridToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // No full-screen dim layer: only the menu card itself is shown, and tapping
            // anywhere outside the card dismisses the menu.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Extend down from the top bar: the card starts at the same edge as the
                // top bar (below the status bar) so it covers the top bar entirely.
                .statusBarsPadding()
                .fillMaxWidth()
                // Square top edge (the card drops from the top bar), rounded bottom.
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 25.dp,
                        bottomEnd = 25.dp
                    )
                )
                .background(Color.Black)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Style wordmark button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isFilterActive) Color(0xFFFFD60A).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f))
                        .border(
                            width = if (isFilterActive) 1.5.dp else 0.dp,
                            color = if (isFilterActive) Color(0xFFFFD60A) else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onDismiss(); onStyleClick() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Style",
                        color = if (isFilterActive) Color(0xFFFFD60A) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Grid toggle button
                val gridActive = showGrid
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (gridActive) Color(0xFFFFD60A).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f))
                        .border(
                            width = if (gridActive) 1.5.dp else 0.dp,
                            color = if (gridActive) Color(0xFFFFD60A) else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onGridToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (gridActive) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "构图线",
                        tint = if (gridActive) Color(0xFFFFD60A) else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

        }
    }
}

@Composable
private fun AspectRatioDropdown(
    selectedAspectRatio: AspectRatio,
    isPortrait: Boolean,
    onAspectRatioSelected: (AspectRatio) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatios = listOf(AspectRatio.P1_1, AspectRatio.P4_3, AspectRatio.P16_9)
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffset = (swipeOffset + delta).coerceAtLeast(0f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { androidx.compose.ui.unit.IntOffset(0, swipeOffset.roundToInt()) }
                // Extend down from the top bar: the card starts at the same edge as the
                // top bar (below the status bar) so it covers the top bar entirely.
                .statusBarsPadding()
                .fillMaxWidth()
                // Square top edge (the card drops from the top bar), rounded bottom.
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 25.dp,
                        bottomEnd = 25.dp
                    )
                )
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (swipeOffset > 120f) onDismiss()
                        swipeOffset = 0f
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                aspectRatios.forEach { ratio ->
                    val selected = ratio == selectedAspectRatio
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                        label = "aspectRatioScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(25.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onAspectRatioSelected(ratio); onDismiss() }
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        val ratioValue = ratio.displayRatio(isPortrait)
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(ratioValue)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) Color(0xFFFFD60A).copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) Color(0xFFFFD60A) else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = ratio.displayTitle(isPortrait),
                            color = if (selected) Color(0xFFFFD60A) else Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomControl(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onZoomPresetSelected: (Float) -> Unit,
    camera: Camera?,
    isPinching: Boolean,
    showWheel: Boolean,
    onShowWheelChange: (Boolean) -> Unit,
    contentBaselineY: Float,
    modifier: Modifier = Modifier
) {
    val zoomState = camera?.cameraInfo?.zoomState?.value
    val minZoom = zoomState?.minZoomRatio ?: 0.6f
    val maxZoom = zoomState?.maxZoomRatio ?: 20f
    val presets = remember { listOf(0.6f, 1f, 2f) }
    val currentZoomState = rememberUpdatedState(currentZoom)

    // While set, the preset bar highlights this exact preset regardless of the live
    // zoom value, so tapping 0.6x -> 2x (or reverse) never flashes the middle 1x
    // during the zoom animation. Cleared as soon as zoom changes by drag/pinch.
    var selectedPreset by remember { mutableStateOf<Float?>(null) }

    // Set by a local pointer monitor while two or more fingers are down on the zoom
    // control, so the drag detector never fights the viewfinder pinch zoom.
    var multiFingerDown by remember { mutableStateOf(false) }
    val currentMultiFinger by rememberUpdatedState(multiFingerDown)

    LaunchedEffect(isPinching) {
        if (isPinching) {
            selectedPreset = null
            onShowWheelChange(false)
        }
    }

    val density = LocalDensity.current
    // Fixed container height for the preset bar so its bottom edge aligns exactly
    // with contentBaselineY (the viewfinder bottom line for 1:1/3:4, or the 3:4 line
    // for long aspect ratios) while it extends upward inside the viewfinder.
    val presetBarBoxHeightPx = with(density) { 48.dp.toPx() }

    // Keep gesture detector stable across state changes; read latest values via updated state.
    val currentIsPinching by rememberUpdatedState(isPinching)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Visual layer: preset bar or wheel. This layer has no gesture handling, so it
        // never blocks taps on anything below it.
        if (!isPinching) {
            when {
                showWheel -> ZoomWheel(
                    currentZoom = currentZoom,
                    minZoom = minZoom,
                    maxZoom = maxZoom,
                    circleCenterY = contentBaselineY
                )
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { presetBarBoxHeightPx.toDp() })
                        .offset {
                            IntOffset(
                                0,
                                (contentBaselineY - presetBarBoxHeightPx).roundToInt()
                            )
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ZoomPresetBar(currentZoom, minZoom, maxZoom, selectedPreset)
                }
            }
        }

        // Gesture layer: when collapsed it covers ONLY the preset bar, so a two-finger
        // pinch starting anywhere else in the viewfinder can never open the wheel. Once
        // the wheel is open it expands over the whole wheel region so rotation can be
        // grabbed anywhere on the wheel. It never extends over the bottom bar.
        val gestureLayerHeightPx = if (showWheel) contentBaselineY else presetBarBoxHeightPx
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { gestureLayerHeightPx.toDp() })
                .offset { IntOffset(0, (contentBaselineY - gestureLayerHeightPx).roundToInt()) }
                // Multi-finger monitor: flags when a second finger joins this control so
                // the drag detector backs off and the viewfinder pinch keeps control.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                multiFingerDown = event.changes.count { it.pressed } >= 2
                            }
                        } finally {
                            multiFingerDown = false
                        }
                    }
                }
                // Tap on a preset selects it and locks the highlight to it.
                .pointerInput(minZoom, maxZoom) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (currentIsPinching || currentMultiFinger) return@detectTapGestures
                            val columnWidth = size.width / presets.size
                            val tappedIndex = (offset.x / columnWidth).toInt().coerceIn(0, presets.size - 1)
                            val preset = presets[tappedIndex].coerceIn(minZoom, maxZoom)
                            // Lock the highlighted preset so the zoom animation to it
                            // never flashes an intermediate preset (e.g. 1x when jumping
                            // between 0.6x and 2x).
                            selectedPreset = preset
                            onZoomPresetSelected(preset)
                        }
                    )
                }
                // Wheel drag: the framework-managed drag lifecycle guarantees that
                // onDragEnd (or onDragCancel) fires whenever the drag ends, so the wheel
                // always closes on release — no hand-rolled event loop to go stale.
                .pointerInput(minZoom, maxZoom) {
                    detectDragGestures(
                        onDragStart = {
                            if (currentIsPinching || currentMultiFinger) return@detectDragGestures
                            selectedPreset = null
                            onShowWheelChange(true)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (currentIsPinching || currentMultiFinger) return@detectDragGestures
                            // Left drag (negative delta) zooms in; right drag zooms out.
                            // The wheel rotates counter-clockwise while zooming in.
                            val sensitivity = 0.012f
                            val newZoom = (currentZoomState.value - dragAmount.x * sensitivity).coerceIn(minZoom, maxZoom)
                            onZoomChange(newZoom)
                        },
                        onDragEnd = { onShowWheelChange(false) },
                        onDragCancel = { onShowWheelChange(false) }
                    )
                }
        )
    }
}

@Composable
private fun ZoomCollapsedBadge(currentZoom: Float) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.5.dp, Color(0xFFFFD60A), RoundedCornerShape(25.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format(Locale.US, "%.1fx", currentZoom),
            color = Color(0xFFFFD60A),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ZoomPresetBar(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    selectedPreset: Float? = null,
    modifier: Modifier = Modifier
) {
    val presets = listOf(0.6f, 1f, 2f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEachIndexed { index, preset ->
            val targetZoom = preset.coerceIn(minZoom, maxZoom)
            val showActualOnLast = index == presets.lastIndex && currentZoom > 2.15f
            val showActualOnFirst = index == 0 && currentZoom < 0.85f
            // When a preset was just tapped, lock the highlight to it so the zoom
            // animation never flashes an intermediate preset (e.g. 1x when jumping
            // between 0.6x and 2x). Otherwise follow the live zoom value.
            val selected = when {
                selectedPreset != null -> kotlin.math.abs(preset - selectedPreset) < 0.01f
                showActualOnLast || showActualOnFirst -> true
                else -> kotlin.math.abs(currentZoom - targetZoom) < 0.15f
            }
            val enabled = preset in minZoom..maxZoom
            val label = when {
                showActualOnLast || showActualOnFirst -> String.format(Locale.US, "%.1fx", currentZoom)
                else -> String.format(Locale.US, "%.1fx", preset)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        when {
                            !enabled -> Color.White.copy(alpha = 0.08f)
                            selected -> Color(0xFFFFD60A).copy(alpha = 0.22f)
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                    )
                    .border(
                        width = if (selected) 1.5.dp else 0.dp,
                        color = if (selected) Color(0xFFFFD60A) else Color.Transparent,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = when {
                        !enabled -> Color.White.copy(alpha = 0.35f)
                        selected -> Color(0xFFFFD60A)
                        else -> Color.White
                    },
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ZoomWheel(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    circleCenterY: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val labels = listOf(
        0.6f to "0.6x",
        1f to "1x",
        2f to "2x",
        3.2f to "3.2x",
        5f to "5x",
        10f to "10x"
    )
    // The wheel scale is fixed to 0.6x..10x regardless of the device's actual max zoom.
    val wheelMin = 0.6f
    val wheelMax = 10f

    val fraction = if (wheelMax <= wheelMin) 0.5f
    else (kotlin.math.ln(currentZoom.coerceIn(wheelMin, wheelMax) / wheelMin) /
            kotlin.math.ln(wheelMax / wheelMin)).coerceIn(0f, 1f)

    // Map zoom fraction to disk rotation so the current zoom aligns with the fixed
    // top pointer. Zooming in (left drag, fraction up) rotates the disk
    // counter-clockwise; zooming out rotates it clockwise.
    val targetRotation = -(fraction - 0.5f) * 180f
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(120),
        label = "zoomWheelRotation"
    )

    val labelTextSizePx = with(density) { 12.sp.toPx() }
    val labelPaint = remember(labelTextSizePx) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = labelTextSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    var diskRadiusPx by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { diskRadiusPx = it.width / 2f },
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = circleCenterY
            // Disk diameter equals the full-width mask rectangle (and the bottom bar width).
            val diskRadius = size.width / 2f

            // 1) Solid black circular disk with the rotating protractor scale. Drawn first
            //    so the mask rectangle can cover its lower half.
            drawCircle(
                color = Color.Black,
                radius = diskRadius,
                center = Offset(centerX, centerY)
            )

            withTransform({
                rotate(rotation, pivot = Offset(centerX, centerY))
            }) {
                labels.forEach { (zoom, label) ->
                    val f = if (wheelMax <= wheelMin) 0f
                    else (kotlin.math.ln(zoom / wheelMin) / kotlin.math.ln(wheelMax / wheelMin)).coerceIn(0f, 1f)
                    val thetaDeg = 180f - f * 180f
                    val thetaRad = Math.toRadians(thetaDeg.toDouble())
                    val cos = kotlin.math.cos(thetaRad).toFloat()
                    val sin = kotlin.math.sin(thetaRad).toFloat()

                    // Tick mark.
                    val tickInnerR = diskRadius - 12.dp.toPx()
                    val tickOuterR = diskRadius - 4.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(centerX + tickInnerR * cos, centerY - tickInnerR * sin),
                        end = Offset(centerX + tickOuterR * cos, centerY - tickOuterR * sin),
                        strokeWidth = 2f
                    )

                    // Label, radially aligned: each digit's baseline points toward the
                    // disk center, so rotating 90° - thetaDeg keeps the text upright
                    // while its vertical axis passes through the center.
                    val labelR = diskRadius - 28.dp.toPx()
                    val lx = centerX + labelR * cos
                    val ly = centerY - labelR * sin
                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        translate(lx, ly)
                        rotate(90f - thetaDeg)
                        drawText(label, 0f, 0f, labelPaint)
                        restore()
                    }
                }
            }

            // 2) Solid black rectangle drawn above the disk, masking its lower half and
            //    the entire bottom bar.
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, centerY),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - centerY)
            )

            // 3) Fixed pointer at the top of the circle, pointing down at the current zoom.
            //    The arrow sits inside the disk edge so its tip never exceeds the circle
            //    boundary: the tip touches the rim and the arrow body lies within the disk.
            val pointerSize = 10.dp.toPx()
            val pointerTipY = centerY - diskRadius
            drawPath(
                path = Path().apply {
                    moveTo(centerX, pointerTipY)
                    lineTo(centerX - pointerSize / 2, pointerTipY + pointerSize)
                    lineTo(centerX + pointerSize / 2, pointerTipY + pointerSize)
                    close()
                },
                color = Color(0xFFFFD60A)
            )
        }

        // Current zoom value floating above the fixed pointer in bright yellow.
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        0,
                        (circleCenterY - diskRadiusPx + with(density) { 8.dp.toPx() }).roundToInt()
                    )
                }
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = String.format(Locale.US, "%.1fx", currentZoom),
                color = Color(0xFFFFD60A),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GridOverlay(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = Color.White.copy(alpha = 0.55f)
        val stroke = 2f
        drawLine(color, Offset(w * 0.333f, 0f), Offset(w * 0.333f, h), stroke)
        drawLine(color, Offset(w * 0.666f, 0f), Offset(w * 0.666f, h), stroke)
        drawLine(color, Offset(0f, h * 0.333f), Offset(w, h * 0.333f), stroke)
        drawLine(color, Offset(0f, h * 0.666f), Offset(w, h * 0.666f), stroke)
    }
}

@Composable
private fun SixDotsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val rows = 3
        val cols = 2
        val dotRadius = size.minDimension / 8f
        val spacingX = size.width / (cols + 1)
        val spacingY = size.height / (rows + 1)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cx = spacingX * (col + 1)
                val cy = spacingY * (row + 1)
                drawCircle(tint, dotRadius, Offset(cx, cy))
            }
        }
    }
}

private fun extractYuvPlanes(imageProxy: ImageProxy): Triple<ByteBuffer, ByteBuffer, ByteBuffer> {
    val planes = imageProxy.planes
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val width = imageProxy.width
    val height = imageProxy.height
    val uvWidth = width / 2
    val uvHeight = height / 2

    val yCopy = ByteBuffer.allocateDirect(width * height)
    val yBuffer = yPlane.buffer
    val yRowStride = yPlane.rowStride
    for (row in 0 until height) {
        val rowStart = row * yRowStride
        // slice(index, length) requires API 34; older versions fall back to duplicate()+position().
        val rowSlice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            yBuffer.slice(rowStart, width)
        } else {
            yBuffer.duplicate().apply { position(rowStart); limit(rowStart + width) }
        }
        yCopy.put(rowSlice)
    }
    yCopy.position(0)

    val uCopy = ByteBuffer.allocateDirect(uvWidth * uvHeight)
    val vCopy = ByteBuffer.allocateDirect(uvWidth * uvHeight)
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    val uRowStride = uPlane.rowStride
    val vRowStride = vPlane.rowStride
    val uvPixelStride = uPlane.pixelStride.coerceAtLeast(1)

    if (uvPixelStride == 1) {
        // Tightly packed U/V planes: copy entire rows via bulk put.
        for (row in 0 until uvHeight) {
            val uRowStart = row * uRowStride
            val vRowStart = row * vRowStride
            val uRow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                uBuffer.slice(uRowStart, uvWidth)
            } else {
                uBuffer.duplicate().apply { position(uRowStart); limit(uRowStart + uvWidth) }
            }
            val vRow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                vBuffer.slice(vRowStart, uvWidth)
            } else {
                vBuffer.duplicate().apply { position(vRowStart); limit(vRowStart + uvWidth) }
            }
            uCopy.put(uRow)
            vCopy.put(vRow)
        }
    } else {
        // Interleaved U/V (pixelStride == 2): sample every other byte per row.
        for (row in 0 until uvHeight) {
            val uRowStart = row * uRowStride
            val vRowStart = row * vRowStride
            for (col in 0 until uvWidth) {
                val uIndex = uRowStart + col * uvPixelStride
                val vIndex = vRowStart + col * uvPixelStride
                uCopy.put(uBuffer.get(uIndex))
                vCopy.put(vBuffer.get(vIndex))
            }
        }
    }
    uCopy.position(0)
    vCopy.position(0)

    return Triple(yCopy, uCopy, vCopy)
}

private fun performFocus(
    cameraControl: CameraControl?,
    previewSize: Size,
    x: Float,
    y: Float,
    lock: Boolean
) {
    if (previewSize.width <= 0 || previewSize.height <= 0) return
    val factory = SurfaceOrientedMeteringPointFactory(
        previewSize.width.toFloat(),
        previewSize.height.toFloat()
    )
    val point = factory.createPoint(x, y)
    val action = FocusMeteringAction.Builder(point)
        .setAutoCancelDuration(if (lock) 10 else 3, if (lock) TimeUnit.MINUTES else TimeUnit.SECONDS)
        .build()
    try {
        cameraControl?.startFocusAndMetering(action)
    } catch (e: Exception) {
        Log.e("CameraScreen", "Focus failed", e)
    }
}

private fun queryLastPhotoUri(context: Context): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    // Only photos taken by the app (Pictures/ColorPick, plus legacy Pictures/ColorBy
    // folder) so the thumbnail never shows a photo from the system gallery.
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
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    }
    return null
}

private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(96, 96), null)
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeStream(input, null, options)
            }
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Load thumbnail failed", e)
        null
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Load bitmap failed", e)
        null
    }
}

private fun cropBitmapToAspect(bitmap: Bitmap, targetAspectRatio: Float): Bitmap {
    val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    if (kotlin.math.abs(currentRatio - targetAspectRatio) < 0.005f) return bitmap

    val newWidth: Int
    val newHeight: Int
    if (currentRatio > targetAspectRatio) {
        newHeight = bitmap.height
        newWidth = (bitmap.height * targetAspectRatio).roundToInt().coerceAtLeast(1)
    } else {
        newWidth = bitmap.width
        newHeight = (bitmap.width / targetAspectRatio).roundToInt().coerceAtLeast(1)
    }
    val x = (bitmap.width - newWidth) / 2
    val y = (bitmap.height - newHeight) / 2
    return Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    captureExecutor: java.util.concurrent.Executor,
    params: AdjustmentParams,
    aspectRatio: AspectRatio,
    isPortrait: Boolean,
    onComplete: (Uri?) -> Unit
) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val displayName = "IMG_${timeStamp}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ColorPick")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri
                if (savedUri == null) {
                    onComplete(null)
                    return
                }
                captureExecutor.execute {
                    try {
                        val bitmap = loadBitmapFromUri(context, savedUri)
                        if (bitmap != null) {
                            // CameraX writes correct EXIF orientation; read it and rotate the decoded bitmap upright.
                            val exifRotation = readExifRotation(context, savedUri)
                            val upright = rotateBitmap(bitmap, exifRotation)
                            val targetRatio = aspectRatio.displayRatio(isPortrait)
                            val source = cropBitmapToAspect(upright, targetRatio)
                            val processed = OffscreenGlRenderer(source.width, source.height).use { renderer ->
                                renderer.render(source, params)
                            }
                            context.contentResolver.openOutputStream(savedUri, "w")?.use { out ->
                                processed.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            }

                            context.contentResolver.openFileDescriptor(savedUri, "rw")?.use { pfd ->
                                val exif = ExifInterface(pfd.fileDescriptor)
                                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                                val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                                exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
                                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
                                val subSec = SimpleDateFormat("SSS", Locale.US).format(Date())
                                exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, subSec)
                                exif.saveAttributes()
                            }

                            if (source != upright) {
                                source.recycle()
                            }
                            if (upright != bitmap) {
                                upright.recycle()
                            }
                            if (!processed.sameAs(bitmap)) {
                                processed.recycle()
                            }
                            bitmap.recycle()
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            MediaScannerConnection.scanFile(context, arrayOf(savedUri.toString()), arrayOf("image/jpeg"), null)
                        }

                        onComplete(savedUri)
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Photo processing failed", e)
                        onComplete(savedUri)
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed", exception)
                onComplete(null)
            }
        }
    )
}

private fun readExifRotation(context: Context, uri: Uri): Int {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val exif = ExifInterface(pfd.fileDescriptor)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        Log.e("CameraScreen", "Read EXIF rotation failed", e)
        0
    }
}

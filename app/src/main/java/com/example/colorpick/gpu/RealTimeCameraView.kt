package com.example.colorpick.gpu

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.colorpick.ui.editor.AdjustmentParams

@Composable
fun RealTimeCameraView(
    params: AdjustmentParams,
    rotationDegrees: Int = 0,
    isMirrored: Boolean = false,
    viewportWidth: Int = 0,
    viewportHeight: Int = 0,
    onRendererCreated: (CameraFrameRenderer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                val renderer = CameraFrameRenderer()
                renderer.setParams(params)
                renderer.setRotation(rotationDegrees)
                renderer.setMirrored(isMirrored)
                renderer.setViewport(viewportWidth, viewportHeight)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                renderer.onFrameAvailable = { requestRender() }
                tag = renderer
                onRendererCreated(renderer)
            }
        },
        update = { view ->
            val renderer = view.tag as? CameraFrameRenderer
            renderer?.let {
                it.setParams(params)
                it.setMirrored(isMirrored)
                it.setViewport(viewportWidth, viewportHeight)
                view.requestRender()
            }
        },
        modifier = modifier
    )
}

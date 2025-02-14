package org.jetbrains.skiko.context

import kotlinx.cinterop.*
import org.jetbrains.skia.*
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.RenderException
import org.jetbrains.skiko.SkiaLayer
import platform.OpenGL.GL_DRAW_FRAMEBUFFER_BINDING
import platform.OpenGL.glGetIntegerv
import platform.OpenGLCommon.GLenum

/**
 * OpenGL context handler for MacOs (native).
 * Not used anymore, unless corresponding [GraphicsApi] is hardcoded in [SkiaLayer].
 * See [MacOsMetalContextHandler] instead.
 */
internal class MacOSOpenGLContextHandler(layer: SkiaLayer) : ContextHandler(layer, layer::draw) {
    override fun initContext(): Boolean {
        try {
            if (context == null) {
                context = DirectContext.makeGL()
            }
        } catch (e: Exception) {
            println("Failed to create Skia OpenGL context!")
            return false
        }
        return true
    }

    @ExperimentalUnsignedTypes
    private fun openglGetIntegerv(pname: GLenum): UInt {
        var result: UInt = 0U
        memScoped {
            val data = alloc<IntVar>()
            glGetIntegerv(pname, data.ptr);
            result = data.value.toUInt();
        }
        return result
    }

    private var currentWidth = 0
    private var currentHeight = 0
    private fun isSizeChanged(width: Int, height: Int): Boolean {
        if (width != currentWidth || height != currentHeight) {
            currentWidth = width
            currentHeight = height
            return true
        }
        return false
    }

    override fun initCanvas() {
        val scale = layer.contentScale
        val w = (layer.nsView.frame.useContents { size.width } * scale).toInt().coerceAtLeast(0)
        val h = (layer.nsView.frame.useContents { size.height } * scale).toInt().coerceAtLeast(0)
        if (isSizeChanged(w, h)) {
            val fbId = openglGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING.toUInt())
            renderTarget = BackendRenderTarget.makeGL(
                    w,
                    h,
                    0,
                    8,
                    fbId.toInt(),
                    FramebufferFormat.GR_GL_RGBA8
                )
            surface = Surface.makeFromBackendRenderTarget(
                    context!!,
                    renderTarget!!,
                    SurfaceOrigin.BOTTOM_LEFT,
                    SurfaceColorFormat.RGBA_8888,
                    ColorSpace.sRGB
                ) ?: throw RenderException("Cannot create surface")

            canvas = surface?.canvas
                ?: error("Could not obtain Canvas from Surface")
        }
    }
}

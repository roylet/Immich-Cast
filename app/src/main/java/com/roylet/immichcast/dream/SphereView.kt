package com.roylet.immichcast.dream

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.tan

/**
 * Shows a 360° equirectangular photo or video from the inside, turning slowly through a full circle.
 * A full-screen triangle is drawn; each pixel's view ray is turned into longitude and latitude
 * and looked up in the equirectangular image, so there's no sphere mesh.
 */
internal class SphereView(context: Context) : GLSurfaceView(context) {

    private val renderer = SphereRenderer()

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /** Shows [bitmap], turning a full circle every [turnMs], starting now. */
    fun showImage(bitmap: Bitmap, turnMs: Long) {
        queueEvent { renderer.setImage(bitmap, turnMs) }
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /**
     * A surface for a video player to draw into. Its frames replace the image once the first one
     * arrives, and keep turning on the same clock. Call [stopVideo] when playback ends.
     */
    fun videoSurface(): Surface? = renderer.videoSurface

    fun stopVideo() = queueEvent { renderer.useVideo = false }

    /** Stops drawing; the view keeps its last frame until shown again. */
    fun idle() {
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private inner class SphereRenderer : Renderer, SurfaceTexture.OnFrameAvailableListener {
        private var imageProgram = 0
        private var videoProgram = 0
        private var imageTexture = 0
        private var videoTexture = 0
        private var surfaceTexture: SurfaceTexture? = null

        @Volatile var videoSurface: Surface? = null
            private set
        @Volatile var useVideo = false

        private var image: Bitmap? = null
        private var imageDirty = false
        private var turnMs = 60_000L
        private var startedAt = 0L

        private val rotation = FloatArray(16)
        private val videoMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        private var halfTanX = 1f
        private var halfTanY = 1f

        private val triangle = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            .apply { put(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)); position(0) }

        fun setImage(bitmap: Bitmap, turnMs: Long) {
            image = bitmap
            imageDirty = true
            useVideo = false
            this.turnMs = turnMs.coerceAtLeast(1)
            startedAt = SystemClock.uptimeMillis()
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            imageProgram = program(fragment("sampler2D", ""))
            videoProgram = program(fragment("samplerExternalOES", "#extension GL_OES_EGL_image_external_essl3 : require\n"))
            val ids = IntArray(2)
            GLES30.glGenTextures(2, ids, 0)
            imageTexture = ids[0]
            videoTexture = ids[1]

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, imageTexture)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            // Longitude wraps round, so the seam at the back of the sphere filters cleanly.
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTexture)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

            surfaceTexture?.release()
            videoSurface?.release()
            surfaceTexture = SurfaceTexture(videoTexture).also { it.setOnFrameAvailableListener(this) }
            videoSurface = Surface(surfaceTexture)
            // A new context has lost the old texture.
            imageDirty = image != null
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES30.glViewport(0, 0, width, height)
            halfTanY = tan(Math.toRadians(VERTICAL_FOV / 2.0)).toFloat()
            halfTanX = halfTanY * width / height.coerceAtLeast(1)
        }

        override fun onFrameAvailable(texture: SurfaceTexture) {
            useVideo = true
        }

        override fun onDrawFrame(gl: GL10?) {
            if (imageDirty) {
                image?.let {
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, imageTexture)
                    GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, it, 0)
                }
                imageDirty = false
            }
            val video = useVideo
            if (video) {
                surfaceTexture?.updateTexImage()
                surfaceTexture?.getTransformMatrix(videoMatrix)
            }
            if (image == null && !video) {
                GLES30.glClearColor(0f, 0f, 0f, 1f)
                GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
                return
            }
            val elapsed = SystemClock.uptimeMillis() - startedAt
            val yawDegrees = 360f * (elapsed % turnMs) / turnMs
            Matrix.setRotateM(rotation, 0, yawDegrees, 0f, 1f, 0f)

            val program = if (video) videoProgram else imageProgram
            GLES30.glUseProgram(program)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(if (video) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES30.GL_TEXTURE_2D, if (video) videoTexture else imageTexture)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(program, "tex"), 0)
            GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "rotation"), 1, false, rotation, 0)
            GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, "texMatrix"), 1, false, if (video) videoMatrix else identity, 0)
            GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "halfTan"), halfTanX, halfTanY)

            val position = GLES30.glGetAttribLocation(program, "position")
            GLES30.glEnableVertexAttribArray(position)
            GLES30.glVertexAttribPointer(position, 2, GLES30.GL_FLOAT, false, 0, triangle)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
            GLES30.glDisableVertexAttribArray(position)
        }

        private fun fragment(sampler: String, extension: String) = """#version 300 es
            $extension
            precision highp float;
            uniform $sampler tex;
            uniform mat4 rotation;
            uniform mat4 texMatrix;
            uniform vec2 halfTan;
            in vec2 ndc;
            out vec4 color;
            const float PI = ${PI.toFloat()};
            void main() {
                vec3 ray = normalize((rotation * vec4(ndc * halfTan, -1.0, 0.0)).xyz);
                float lon = atan(ray.x, -ray.z);
                float lat = asin(clamp(ray.y, -1.0, 1.0));
                vec2 uv = vec2(lon / (2.0 * PI) + 0.5, 0.5 - lat / PI);
                color = texture(tex, (texMatrix * vec4(uv, 0.0, 1.0)).xy);
            }
        """.trimIndent()

        private fun program(fragmentSource: String): Int {
            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, shader(GLES30.GL_VERTEX_SHADER, VERTEX))
            GLES30.glAttachShader(program, shader(GLES30.GL_FRAGMENT_SHADER, fragmentSource))
            GLES30.glLinkProgram(program)
            val ok = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, ok, 0)
            if (ok[0] == 0) Log.w(TAG, "Sphere program failed to link: ${GLES30.glGetProgramInfoLog(program)}")
            return program
        }

        private fun shader(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            val ok = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) Log.w(TAG, "Sphere shader failed to compile: ${GLES30.glGetShaderInfoLog(shader)}")
            return shader
        }
    }

    private companion object {
        const val TAG = "ImmichCast"

        /** About what a phone's main camera sees vertically; wide enough to feel immersive without bending. */
        const val VERTICAL_FOV = 65.0

        val VERTEX = """#version 300 es
            in vec2 position;
            out vec2 ndc;
            void main() {
                ndc = position;
                gl_Position = vec4(position, 0.0, 1.0);
            }
        """.trimIndent()
    }
}

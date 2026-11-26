package com.example.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates real H.264 MP4 cinema test reels on-device via MediaCodec + MediaMuxer
 * so users can immediately test trimming, splitting, LUT color grading, text overlays,
 * and Media3 Transformer export even before importing external videos.
 */
object SampleMediaGenerator {

    enum class ReelStyle {
        SUNSET_HORIZON,
        NEON_STUDIO,
        MELON_WAVE
    }

    suspend fun ensureDemoReelsExist(context: Context): List<Pair<String, Uri>> =
        withContext(Dispatchers.IO) {
            val demoDir = File(context.filesDir, "demo").apply { mkdirs() }
            val specs = listOf(
                Triple("demo_sunset_cinema.mp4", "مشهد الغروب السينمائي", ReelStyle.SUNSET_HORIZON),
                Triple("demo_neon_pulse.mp4", "نبض الاستوديو الليلي", ReelStyle.NEON_STUDIO),
                Triple("demo_melon_motion.mp4", "موجة ميلون الحركية", ReelStyle.MELON_WAVE)
            )
            specs.mapNotNull { (fileName, title, style) ->
                val outFile = File(demoDir, fileName)
                if (!outFile.exists() || outFile.length() < 4096L) {
                    val generated = generateMp4Clip(
                        outputFile = outFile,
                        width = 640,
                        height = 360,
                        fps = 24,
                        durationSeconds = 4,
                        style = style
                    )
                    if (!generated) return@mapNotNull null
                }
                title to Uri.fromFile(outFile)
            }
        }

    private fun generateMp4Clip(
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        durationSeconds: Int,
        style: ReelStyle
    ): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null
        return try {
            if (outputFile.exists()) outputFile.delete()
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 1_800_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            val totalFrames = fps * durationSeconds

            for (frameIdx in 0 until totalFrames) {
                val canvas = inputSurface.lockCanvas(null)
                try {
                    drawFrame(canvas, width, height, frameIdx, totalFrames, style)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drainEncoder(
                    codec = codec,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    endOfStream = false,
                    trackIndex = trackIndex,
                    muxerStarted = muxerStarted
                ) { newTrack, started ->
                    trackIndex = newTrack
                    muxerStarted = started
                }
            }

            codec.signalEndOfInputStream()
            drainEncoder(
                codec = codec,
                muxer = muxer,
                bufferInfo = bufferInfo,
                endOfStream = true,
                trackIndex = trackIndex,
                muxerStarted = muxerStarted
            ) { newTrack, started ->
                trackIndex = newTrack
                muxerStarted = started
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { inputSurface?.release() }
            runCatching {
                codec?.stop()
                codec?.release()
            }
            runCatching {
                muxer?.stop()
                muxer?.release()
            }
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        trackIndex: Int,
        muxerStarted: Boolean,
        onTrackState: (Int, Boolean) -> Unit
    ) {
        var currentTrack = trackIndex
        var started = muxerStarted
        val timeoutUs = if (endOfStream) 10_000L else 0L
        while (true) {
            val status = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                status == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!started) {
                        currentTrack = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        started = true
                        onTrackState(currentTrack, started)
                    }
                }
                status >= 0 -> {
                    val encodedData = codec.getOutputBuffer(status)
                    if (encodedData != null && bufferInfo.size > 0 && started) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(currentTrack, encodedData, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(status, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                }
            }
        }
    }

    private fun drawFrame(
        canvas: Canvas,
        w: Int,
        h: Int,
        frame: Int,
        totalFrames: Int,
        style: ReelStyle
    ) {
        val t = frame.toFloat() / totalFrames.coerceAtLeast(1)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colors = when (style) {
            ReelStyle.SUNSET_HORIZON -> intArrayOf(
                Color.rgb(20, 14, 38),
                Color.rgb(185, 58, 92),
                Color.rgb(245, 166, 35)
            )
            ReelStyle.NEON_STUDIO -> intArrayOf(
                Color.rgb(11, 14, 24),
                Color.rgb(22, 78, 99),
                Color.rgb(46, 229, 157)
            )
            ReelStyle.MELON_WAVE -> intArrayOf(
                Color.rgb(18, 20, 28),
                Color.rgb(255, 77, 109),
                Color.rgb(255, 183, 3)
            )
        }
        bgPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            colors,
            floatArrayOf(0f, 0.55f + 0.15f * sin(t * 2f * PI.toFloat()), 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Cinematic moving orbs / sun
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = w * (0.25f + 0.5f * t)
        val cy = h * (0.48f - 0.15f * sin(t * PI.toFloat()))
        orbPaint.color = when (style) {
            ReelStyle.SUNSET_HORIZON -> Color.argb(210, 255, 214, 102)
            ReelStyle.NEON_STUDIO -> Color.argb(200, 56, 189, 248)
            ReelStyle.MELON_WAVE -> Color.argb(210, 46, 229, 157)
        }
        canvas.drawCircle(cx, cy, h * 0.19f, orbPaint)

        // Stylized horizon / waveform bars
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 255, 255, 255)
            strokeWidth = 4f
        }
        val bars = 28
        val step = w.toFloat() / (bars + 2)
        for (i in 1..bars) {
            val wave = 0.5f + 0.5f * sin((t * 4f * PI + i * 0.45).toFloat())
            val barH = h * (0.08f + 0.22f * wave)
            val bx = i * step
            val rect = RectF(bx, h * 0.72f - barH / 2f, bx + step * 0.45f, h * 0.72f + barH / 2f)
            canvas.drawRoundRect(rect, 6f, 6f, barPaint)
        }

        // Subtle cinema reticle + frame counter
        val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 20f
            isFakeBoldText = true
        }
        val tag = when (style) {
            ReelStyle.SUNSET_HORIZON -> "MELON CAM A • 24FPS"
            ReelStyle.NEON_STUDIO -> "RESOLVE LOG • REC709"
            ReelStyle.MELON_WAVE -> "MELON STUDIO • CLIP 03"
        }
        canvas.drawText(tag, 24f, 38f, hudPaint)

        val angle = t * 360f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            this.style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val rx = w * 0.85f
        val ry = h * 0.22f
        canvas.drawCircle(rx, ry, 28f, ringPaint)
        val dotX = rx + 28f * cos(Math.toRadians(angle.toDouble())).toFloat()
        val dotY = ry + 28f * sin(Math.toRadians(angle.toDouble())).toFloat()
        canvas.drawCircle(dotX, dotY, 5f, orbPaint)
    }
}

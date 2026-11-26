package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ColorScopesData(
    val lumaBins: FloatArray = FloatArray(32),
    val redBins: FloatArray = FloatArray(32),
    val greenBins: FloatArray = FloatArray(32),
    val blueBins: FloatArray = FloatArray(32)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorScopesData) return false
        return lumaBins.contentEquals(other.lumaBins) &&
            redBins.contentEquals(other.redBins) &&
            greenBins.contentEquals(other.greenBins) &&
            blueBins.contentEquals(other.blueBins)
    }

    override fun hashCode(): Int {
        var result = lumaBins.contentHashCode()
        result = 31 * result + redBins.contentHashCode()
        result = 31 * result + greenBins.contentHashCode()
        result = 31 * result + blueBins.contentHashCode()
        return result
    }
}

object ThumbnailExtractor {
    private val cache = object : LruCache<String, Bitmap>(64) {}

    suspend fun extractFilmstrip(
        context: Context,
        uriString: String,
        isImage: Boolean,
        trimStartMs: Long,
        trimEndMs: Long,
        frameCount: Int = 5
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Bitmap>()
        val uri = Uri.parse(uriString)

        if (isImage) {
            val cacheKey = "${uriString}_img"
            val bmp = cache.get(cacheKey) ?: decodeSampledImage(context, uri)?.also {
                cache.put(cacheKey, it)
            }
            if (bmp != null) {
                repeat(frameCount) { result.add(bmp) }
            }
            return@withContext result
        }

        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val span = (trimEndMs - trimStartMs).coerceAtLeast(100L)
            for (i in 0 until frameCount) {
                val timeMs = trimStartMs + (span * i) / frameCount.coerceAtLeast(1)
                val key = "${uriString}_${timeMs / 250}"
                val cached = cache.get(key)
                if (cached != null) {
                    result.add(cached)
                } else {
                    val frame = retriever.getFrameAtTime(
                        timeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (frame != null) {
                        val scaled = Bitmap.createScaledBitmap(frame, 120, 72, true)
                        if (scaled != frame) frame.recycle()
                        cache.put(key, scaled)
                        result.add(scaled)
                    }
                }
            }
        } catch (_: Exception) {
            // Gracefully return whatever frames succeeded
        } finally {
            runCatching { retriever.release() }
        }
        result
    }

    suspend fun sampleScopesAtPosition(
        context: Context,
        uriString: String,
        isImage: Boolean,
        sourceTimeMs: Long
    ): ColorScopesData = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        var bitmap: Bitmap? = null
        var shouldRecycle = false
        try {
            if (isImage) {
                bitmap = decodeSampledImage(context, uri)
                shouldRecycle = true
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    if (uri.scheme == "file" && uri.path != null) {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(context, uri)
                    }
                    bitmap = retriever.getFrameAtTime(
                        sourceTimeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    shouldRecycle = true
                } finally {
                    runCatching { retriever.release() }
                }
            }
            val bmp = bitmap ?: return@withContext ColorScopesData()
            val w = bmp.width
            val h = bmp.height
            val luma = IntArray(32)
            val red = IntArray(32)
            val green = IntArray(32)
            val blue = IntArray(32)
            val stepX = (w / 24).coerceAtLeast(1)
            val stepY = (h / 24).coerceAtLeast(1)
            var samples = 0
            var y = 0
            while (y < h) {
                var x = 0
                while (x < w) {
                    val pixel = bmp.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
                    luma[(lum * 31) / 255]++
                    red[(r * 31) / 255]++
                    green[(g * 31) / 255]++
                    blue[(b * 31) / 255]++
                    samples++
                    x += stepX
                }
                y += stepY
            }
            if (samples == 0) return@withContext ColorScopesData()
            val maxLuma = luma.maxOrNull()?.coerceAtLeast(1) ?: 1
            val maxRed = red.maxOrNull()?.coerceAtLeast(1) ?: 1
            val maxGreen = green.maxOrNull()?.coerceAtLeast(1) ?: 1
            val maxBlue = blue.maxOrNull()?.coerceAtLeast(1) ?: 1
            ColorScopesData(
                lumaBins = FloatArray(32) { luma[it].toFloat() / maxLuma },
                redBins = FloatArray(32) { red[it].toFloat() / maxRed },
                greenBins = FloatArray(32) { green[it].toFloat() / maxGreen },
                blueBins = FloatArray(32) { blue[it].toFloat() / maxBlue }
            )
        } catch (_: Exception) {
            ColorScopesData()
        } finally {
            if (shouldRecycle) {
                runCatching { bitmap?.recycle() }
            }
        }
    }

    private fun decodeSampledImage(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }
}

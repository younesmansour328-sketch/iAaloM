package com.example.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.model.ExportCodec
import com.example.data.model.ExportResolution
import com.example.data.model.ProjectWithDetails
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progressPercent: Int) : ExportUiState
    data class Completed(
        val shareUri: Uri,
        val gallerySaved: Boolean,
        val fileSizeMb: Float,
        val outputPath: String
    ) : ExportUiState
    data class Failed(val message: String) : ExportUiState
}

@UnstableApi
class VideoExporter(private val context: Context) {

    private var activeTransformer: Transformer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    fun startExport(
        projectWithDetails: ProjectWithDetails,
        resolution: ExportResolution,
        codec: ExportCodec,
        onStateChanged: (ExportUiState) -> Unit
    ) {
        cancel()
        val orderedClips = projectWithDetails.orderedClips
        if (orderedClips.isEmpty()) {
            onStateChanged(ExportUiState.Failed("أضف مقطعاً واحداً على الأقل قبل التصدير"))
            return
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = projectWithDetails.project.name
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "MelonProject" }
        val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.mp4")

        val editedVideoItems = orderedClips.mapIndexed { index, clip ->
            val uri = Uri.parse(clip.sourceUri)
            val mediaItemBuilder = MediaItem.Builder().setUri(uri)
            if (clip.isImage) {
                mediaItemBuilder.setImageDurationMs(clip.effectiveDurationMs)
            } else {
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
            }

            val videoEffects = MediaEffectFactory.buildColorAndTransformEffects(
                project = projectWithDetails.project,
                clip = clip,
                includePresentation = true,
                exportResolution = resolution,
                overlays = if (index == 0) projectWithDetails.orderedOverlays else emptyList()
            ).toMutableList()

            val audioProcessors = mutableListOf<androidx.media3.common.audio.AudioProcessor>()
            if (!clip.isImage && abs(clip.speed - 1.0f) > 0.05f) {
                videoEffects.add(SpeedChangeEffect(clip.speed))
                val sonic = SonicAudioProcessor().apply {
                    setSpeed(clip.speed)
                }
                audioProcessors.add(sonic)
            }

            val itemBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
                .setEffects(Effects(audioProcessors, videoEffects))
                .setRemoveAudio(clip.isMuted || projectWithDetails.project.masterVolume <= 0.02f)

            if (clip.isImage) {
                itemBuilder.setFrameRate(projectWithDetails.project.fps.coerceIn(24, 60))
            }
            itemBuilder.build()
        }

        val videoSequence = EditedMediaItemSequence.Builder(editedVideoItems).build()
        val sequences = mutableListOf(videoSequence)

        val musicUriStr = projectWithDetails.project.musicUri
        if (!musicUriStr.isNullOrBlank() && !projectWithDetails.project.musicMuted) {
            val musicItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(musicUriStr)))
                .build()
            val musicSequence = EditedMediaItemSequence.Builder(listOf(musicItem))
                .setIsLooping(true)
                .build()
            sequences.add(musicSequence)
        }

        val voiceoverUriStr = projectWithDetails.project.voiceoverUri
        if (!voiceoverUriStr.isNullOrBlank()) {
            val voiceoverItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(voiceoverUriStr)))
                .build()
            val voiceoverSequence = EditedMediaItemSequence.Builder(listOf(voiceoverItem)).build()
            sequences.add(voiceoverSequence)
        }

        val composition = Composition.Builder(sequences).build()

        val videoMime = if (codec == ExportCodec.H265) {
            MimeTypes.VIDEO_H265
        } else {
            MimeTypes.VIDEO_H264
        }

        onStateChanged(ExportUiState.Exporting(0))

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(videoMime)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    stopProgressPolling()
                    val savedToGallery = saveToMediaStore(outFile)
                    val shareUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outFile
                    )
                    val sizeMb = (outFile.length() / (1024f * 1024f)).coerceAtLeast(0.1f)
                    onStateChanged(
                        ExportUiState.Completed(
                            shareUri = shareUri,
                            gallerySaved = savedToGallery,
                            fileSizeMb = sizeMb,
                            outputPath = outFile.absolutePath
                        )
                    )
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    stopProgressPolling()
                    onStateChanged(
                        ExportUiState.Failed(
                            exportException.localizedMessage ?: "تعذر إتمام التصدير"
                        )
                    )
                }
            })
            .build()

        activeTransformer = transformer
        try {
            transformer.start(composition, outFile.absolutePath)
            startProgressPolling(transformer, onStateChanged)
        } catch (e: Exception) {
            stopProgressPolling()
            onStateChanged(ExportUiState.Failed(e.localizedMessage ?: "خطأ في بدء المعالجة"))
        }
    }

    fun cancel() {
        stopProgressPolling()
        runCatching { activeTransformer?.cancel() }
        activeTransformer = null
    }

    private fun startProgressPolling(
        transformer: Transformer,
        onStateChanged: (ExportUiState) -> Unit
    ) {
        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onStateChanged(ExportUiState.Exporting(holder.progress.coerceIn(0, 99)))
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 250L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun saveToMediaStore(sourceFile: File): Boolean {
        if (!sourceFile.exists()) return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/MelonEditor"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    ?.apply { mkdirs() } ?: return false
                val target = File(moviesDir, sourceFile.name)
                sourceFile.copyTo(target, overwrite = true)
                true
            }
        }.getOrDefault(false)
    }
}

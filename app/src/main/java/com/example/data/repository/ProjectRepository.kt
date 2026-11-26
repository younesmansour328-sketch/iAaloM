package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.ProjectDao
import com.example.data.model.AspectRatioOption
import com.example.data.model.LutPreset
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectWithDetails
import com.example.data.model.TextOverlayEntity
import com.example.data.model.TitleStyle
import com.example.data.model.VideoClipEntity
import com.example.media.SampleMediaGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val context: Context,
    private val projectDao: ProjectDao
) {

    fun observeAllProjects(): Flow<List<ProjectWithDetails>> =
        projectDao.observeAllProjectsWithDetails()

    fun observeProject(projectId: Long): Flow<ProjectWithDetails?> =
        projectDao.observeProjectWithDetails(projectId)

    suspend fun getProject(projectId: Long): ProjectWithDetails? =
        projectDao.getProjectWithDetails(projectId)

    suspend fun createEmptyProject(
        name: String,
        aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_16_9
    ): Long {
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name,
                aspectRatio = aspectRatio.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        return projectId
    }

    suspend fun createProjectFromUris(
        name: String,
        uris: List<Uri>,
        aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_16_9
    ): Long = withContext(Dispatchers.IO) {
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name,
                aspectRatio = aspectRatio.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        val clips = uris.mapIndexed { index, uri ->
            buildClipEntityFromUri(projectId, index, uri)
        }
        if (clips.isNotEmpty()) {
            projectDao.insertClips(clips)
        }
        projectId
    }

    suspend fun createDemoProject(): Long = withContext(Dispatchers.IO) {
        val reels = SampleMediaGenerator.ensureDemoReelsExist(context)
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = "فيلم ميلون التجريبي",
                aspectRatio = AspectRatioOption.RATIO_16_9.name,
                fps = 24,
                lutPreset = LutPreset.MELON_POP.name,
                contrast = 0.08f,
                saturation = 12f,
                updatedAt = System.currentTimeMillis()
            )
        )
        val clips = reels.mapIndexed { index, (title, uri) ->
            val duration = probeVideoDurationMs(uri).coerceAtLeast(3500L)
            VideoClipEntity(
                projectId = projectId,
                orderIndex = index,
                sourceUri = uri.toString(),
                displayName = title,
                isImage = false,
                sourceDurationMs = duration,
                trimStartMs = 0L,
                trimEndMs = duration
            )
        }
        if (clips.isNotEmpty()) {
            projectDao.insertClips(clips)
        }
        projectDao.insertOverlay(
            TextOverlayEntity(
                projectId = projectId,
                text = "MELON EDITOR STUDIO",
                startMs = 400L,
                endMs = 3600L,
                posX = 0.5f,
                posY = 0.82f,
                fontSizeSp = 22f,
                colorArgb = 0xFFFFFFFF,
                styleName = TitleStyle.CINEMATIC.name
            )
        )
        projectId
    }

    suspend fun addMediaToProject(projectId: Long, uris: List<Uri>, currentClipCount: Int) =
        withContext(Dispatchers.IO) {
            val newClips = uris.mapIndexed { index, uri ->
                buildClipEntityFromUri(projectId, currentClipCount + index, uri)
            }
            if (newClips.isNotEmpty()) {
                projectDao.insertClips(newClips)
                touchProject(projectId)
            }
        }

    suspend fun addDemoReelToProject(projectId: Long, currentClipCount: Int) =
        withContext(Dispatchers.IO) {
            val reels = SampleMediaGenerator.ensureDemoReelsExist(context)
            val chosen = reels.getOrNull(currentClipCount % reels.size.coerceAtLeast(1))
                ?: return@withContext
            val duration = probeVideoDurationMs(chosen.second).coerceAtLeast(3500L)
            projectDao.insertClip(
                VideoClipEntity(
                    projectId = projectId,
                    orderIndex = currentClipCount,
                    sourceUri = chosen.second.toString(),
                    displayName = chosen.first,
                    isImage = false,
                    sourceDurationMs = duration,
                    trimStartMs = 0L,
                    trimEndMs = duration
                )
            )
            touchProject(projectId)
        }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun duplicateProject(source: ProjectWithDetails): Long {
        val newId = projectDao.insertProject(
            source.project.copy(
                id = 0L,
                name = "${source.project.name} (نسخة)",
                updatedAt = System.currentTimeMillis()
            )
        )
        val clonedClips = source.orderedClips.mapIndexed { index, clip ->
            clip.copy(id = 0L, projectId = newId, orderIndex = index)
        }
        if (clonedClips.isNotEmpty()) {
            projectDao.insertClips(clonedClips)
        }
        source.orderedOverlays.forEach { overlay ->
            projectDao.insertOverlay(overlay.copy(id = 0L, projectId = newId))
        }
        return newId
    }

    suspend fun updateClip(clip: VideoClipEntity) {
        projectDao.updateClip(clip)
        touchProject(clip.projectId)
    }

    suspend fun updateClipsOrder(projectId: Long, reordered: List<VideoClipEntity>) {
        val indexed = reordered.mapIndexed { index, clip -> clip.copy(orderIndex = index) }
        projectDao.updateClips(indexed)
        touchProject(projectId)
    }

    suspend fun splitClipAt(
        projectId: Long,
        orderedClips: List<VideoClipEntity>,
        targetClip: VideoClipEntity,
        splitSourceMs: Long
    ): Long? {
        val safeSplit = splitSourceMs.coerceIn(targetClip.trimStartMs + 250L, targetClip.trimEndMs - 250L)
        if (safeSplit <= targetClip.trimStartMs + 200L || safeSplit >= targetClip.trimEndMs - 200L) {
            return null
        }
        val targetIndex = orderedClips.indexOfFirst { it.id == targetClip.id }
        if (targetIndex < 0) return null

        val leftClip = targetClip.copy(trimEndMs = safeSplit)
        val rightClip = targetClip.copy(
            id = 0L,
            trimStartMs = safeSplit,
            displayName = "${targetClip.displayName} (B)"
        )

        val updatedList = orderedClips.toMutableList()
        updatedList[targetIndex] = leftClip
        updatedList.add(targetIndex + 1, rightClip)

        projectDao.deleteAllClipsForProject(projectId)
        val reindexed = updatedList.mapIndexed { idx, c -> c.copy(id = 0L, orderIndex = idx) }
        projectDao.insertClips(reindexed)
        touchProject(projectId)
        return rightClip.orderIndex.toLong()
    }

    suspend fun duplicateClip(
        projectId: Long,
        orderedClips: List<VideoClipEntity>,
        targetClip: VideoClipEntity
    ) {
        val idx = orderedClips.indexOfFirst { it.id == targetClip.id }
        if (idx < 0) return
        val clone = targetClip.copy(id = 0L, displayName = "${targetClip.displayName} (نسخة)")
        val updated = orderedClips.toMutableList().apply { add(idx + 1, clone) }
        projectDao.deleteAllClipsForProject(projectId)
        projectDao.insertClips(updated.mapIndexed { index, clip -> clip.copy(id = 0L, orderIndex = index) })
        touchProject(projectId)
    }

    suspend fun deleteClip(projectId: Long, orderedClips: List<VideoClipEntity>, targetClip: VideoClipEntity) {
        val remaining = orderedClips.filterNot { it.id == targetClip.id }
        projectDao.deleteAllClipsForProject(projectId)
        if (remaining.isNotEmpty()) {
            projectDao.insertClips(remaining.mapIndexed { index, clip -> clip.copy(id = 0L, orderIndex = index) })
        }
        touchProject(projectId)
    }

    suspend fun insertOverlay(overlay: TextOverlayEntity): Long {
        val id = projectDao.insertOverlay(overlay)
        touchProject(overlay.projectId)
        return id
    }

    suspend fun updateOverlay(overlay: TextOverlayEntity) {
        projectDao.updateOverlay(overlay)
        touchProject(overlay.projectId)
    }

    suspend fun deleteOverlay(overlay: TextOverlayEntity) {
        projectDao.deleteOverlay(overlay)
        touchProject(overlay.projectId)
    }

    suspend fun restoreSnapshot(
        project: ProjectEntity,
        clips: List<VideoClipEntity>,
        overlays: List<TextOverlayEntity>
    ) {
        projectDao.replaceProjectSnapshot(project, clips, overlays)
    }

    private suspend fun touchProject(projectId: Long) {
        projectDao.getProjectWithDetails(projectId)?.project?.let { proj ->
            projectDao.updateProject(proj.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    private fun buildClipEntityFromUri(projectId: Long, orderIndex: Int, uri: Uri): VideoClipEntity {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val isImage = mime.startsWith("image/")
        val name = queryDisplayName(uri) ?: if (isImage) "صورة ${orderIndex + 1}" else "مقطع ${orderIndex + 1}"
        val duration = if (isImage) 4000L else probeVideoDurationMs(uri).coerceAtLeast(1000L)
        return VideoClipEntity(
            projectId = projectId,
            orderIndex = orderIndex,
            sourceUri = uri.toString(),
            displayName = name,
            isImage = isImage,
            sourceDurationMs = duration,
            trimStartMs = 0L,
            trimEndMs = duration
        )
    }

    private fun probeVideoDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 5000L
        } catch (_: Exception) {
            5000L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment?.substringBeforeLast('.')
        }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx)?.substringBeforeLast('.') else null
                    } else null
                }
        }.getOrNull()
    }
}

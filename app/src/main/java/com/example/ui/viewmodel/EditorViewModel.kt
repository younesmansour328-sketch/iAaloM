package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.MelonDatabase
import com.example.data.model.AspectRatioOption
import com.example.data.model.ExportCodec
import com.example.data.model.ExportResolution
import com.example.data.model.AudioEffectPreset
import com.example.data.model.LutPreset
import com.example.data.model.VisualEffectPreset
import com.example.data.model.ProjectWithDetails
import com.example.data.model.TextOverlayEntity
import com.example.data.model.TitleStyle
import com.example.data.model.VideoClipEntity
import com.example.data.model.WorkspaceMode
import com.example.data.repository.ProjectRepository
import com.example.media.ExportUiState
import com.example.media.MediaEffectFactory
import com.example.media.ThumbnailExtractor
import com.example.media.VideoExporter
import com.example.ui.state.EditorSnapshot
import com.example.ui.state.EditorUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(
        context = application,
        projectDao = MelonDatabase.getInstance(application).projectDao()
    )
    private val exporter = VideoExporter(application)

    val videoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    private val musicPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var loadedProjectId: Long = -1L
    private var observeJob: Job? = null
    private var tickerJob: Job? = null
    private var scopeJob: Job? = null
    private var lastClipSignature: String = ""
    private var lastMusicSignature: String = ""
    private var lastEffectSignature: String = ""

    init {
        videoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    syncMusicPlayback(true)
                    startPlayheadTicker()
                } else {
                    syncMusicPlayback(false)
                    tickerJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoPlayer.pause()
                    musicPlayer.pause()
                    seekToTimelineMs(0L)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshActiveEffects()
            }
        })
    }

    fun loadProject(projectId: Long) {
        if (loadedProjectId == projectId) return
        loadedProjectId = projectId
        undoStack.clear()
        redoStack.clear()
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeProject(projectId).collect { details ->
                if (details != null) {
                    onProjectUpdated(details)
                } else {
                    _uiState.update { it.copy(isLoading = false, projectDetails = null) }
                }
            }
        }
    }

    private fun onProjectUpdated(details: ProjectWithDetails) {
        val clips = details.orderedClips
        val currentSelected = _uiState.value.selectedClipId
        val resolvedClipId = if (clips.any { it.id == currentSelected }) currentSelected else clips.firstOrNull()?.id

        val overlays = details.orderedOverlays
        val currentOverlay = _uiState.value.selectedOverlayId
        val resolvedOverlayId = if (overlays.any { it.id == currentOverlay }) currentOverlay else overlays.firstOrNull()?.id

        val clampedPlayhead = _uiState.value.playheadMs.coerceIn(0L, details.totalDurationMs.coerceAtLeast(0L))

        _uiState.update {
            it.copy(
                isLoading = false,
                projectDetails = details,
                selectedClipId = resolvedClipId,
                selectedOverlayId = resolvedOverlayId,
                playheadMs = clampedPlayhead,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }

        rebuildPlaylistIfNeeded(details)
        syncMusicSourceIfNeeded(details)
        refreshActiveEffects(details)
        extractMissingThumbnails(clips)
        refreshScopesSample(details, clampedPlayhead)
    }

    private fun rebuildPlaylistIfNeeded(details: ProjectWithDetails) {
        val clips = details.orderedClips
        val sig = clips.joinToString("|") {
            "${it.id}:${it.sourceUri}:${it.trimStartMs}:${it.trimEndMs}:${it.speed}:${it.isMuted}:${it.volume}"
        }
        if (sig == lastClipSignature) return
        lastClipSignature = sig

        val wasPlaying = videoPlayer.isPlaying
        val targetTimelineMs = _uiState.value.playheadMs
        if (clips.isEmpty()) {
            videoPlayer.clearMediaItems()
            return
        }

        val mediaItems = clips.map { clip ->
            val builder = MediaItem.Builder().setUri(Uri.parse(clip.sourceUri))
            if (clip.isImage) {
                builder.setImageDurationMs(clip.effectiveDurationMs)
            } else {
                builder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
            }
            builder.build()
        }

        videoPlayer.setMediaItems(mediaItems, false)
        videoPlayer.prepare()
        applyCurrentClipPlaybackParams(details, targetTimelineMs)
        seekPlayerOnly(details, targetTimelineMs)
        if (wasPlaying) videoPlayer.play()
    }

    private fun syncMusicSourceIfNeeded(details: ProjectWithDetails) {
        val musicUri = details.project.musicUri
        val sig = "${musicUri.orEmpty()}:${details.project.musicMuted}:${details.project.musicVolume}"
        if (sig == lastMusicSignature) return
        lastMusicSignature = sig

        if (musicUri.isNullOrBlank()) {
            musicPlayer.clearMediaItems()
            return
        }
        musicPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(musicUri)))
        musicPlayer.prepare()
        musicPlayer.volume = if (details.project.musicMuted) 0f else details.project.musicVolume.coerceIn(0f, 1f)
    }

    private fun syncMusicPlayback(shouldPlay: Boolean) {
        val proj = _uiState.value.projectDetails?.project ?: return
        if (proj.musicUri.isNullOrBlank() || proj.musicMuted) {
            musicPlayer.pause()
            return
        }
        if (shouldPlay) {
            val total = musicPlayer.duration.coerceAtLeast(1L)
            musicPlayer.seekTo(_uiState.value.playheadMs % total)
            musicPlayer.play()
        } else {
            musicPlayer.pause()
        }
    }

    private fun refreshActiveEffects(detailsOverride: ProjectWithDetails? = null) {
        val details = detailsOverride ?: _uiState.value.projectDetails ?: return
        val activeIdx = videoPlayer.currentMediaItemIndex.coerceAtLeast(0)
        val activeClip = details.orderedClips.getOrNull(activeIdx)
        val p = details.project
        val sig = "${p.lutPreset}:${p.visualEffect}:${p.brightness}:${p.contrast}:${p.saturation}:${p.warmth}:${p.tint}:${activeClip?.rotationDegrees ?: 0}:${activeClip?.speed ?: 1f}:${activeClip?.isMuted}:${activeClip?.volume}:${p.masterVolume}"
        if (sig == lastEffectSignature) return
        lastEffectSignature = sig

        val effects = MediaEffectFactory.buildColorAndTransformEffects(
            project = p,
            clip = activeClip,
            includePresentation = false
        )
        videoPlayer.setVideoEffects(effects)
        applyCurrentClipPlaybackParams(details, _uiState.value.playheadMs)
    }

    private fun applyCurrentClipPlaybackParams(details: ProjectWithDetails, timelineMs: Long) {
        val pos = details.findClipAtTimelineMs(timelineMs) ?: return
        val clip = pos.clip
        videoPlayer.setPlaybackSpeed(clip.speed.coerceIn(0.25f, 4.0f))
        val vol = if (clip.isMuted) 0f else (clip.volume * details.project.masterVolume).coerceIn(0f, 1f)
        videoPlayer.volume = vol
    }

    private fun extractMissingThumbnails(clips: List<VideoClipEntity>) {
        val existing = _uiState.value.filmstrips
        val missing = clips.filter { !existing.containsKey(it.id) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val updated = existing.toMutableMap()
            missing.forEach { clip ->
                val frames = ThumbnailExtractor.extractFilmstrip(
                    context = getApplication(),
                    uriString = clip.sourceUri,
                    isImage = clip.isImage,
                    trimStartMs = clip.trimStartMs,
                    trimEndMs = clip.trimEndMs,
                    frameCount = 5
                )
                if (frames.isNotEmpty()) {
                    updated[clip.id] = frames
                }
            }
            _uiState.update { it.copy(filmstrips = updated) }
        }
    }

    private fun refreshScopesSample(details: ProjectWithDetails, timelineMs: Long) {
        if (_uiState.value.workspaceMode != WorkspaceMode.COLOR) return
        val pos = details.findClipAtTimelineMs(timelineMs) ?: return
        scopeJob?.cancel()
        scopeJob = viewModelScope.launch {
            val scopes = ThumbnailExtractor.sampleScopesAtPosition(
                context = getApplication(),
                uriString = pos.clip.sourceUri,
                isImage = pos.clip.isImage,
                sourceTimeMs = pos.sourcePositionMs
            )
            _uiState.update { it.copy(scopes = scopes) }
        }
    }

    private fun startPlayheadTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && videoPlayer.isPlaying) {
                val details = _uiState.value.projectDetails
                if (details != null) {
                    val ranges = details.clipTimelineRanges()
                    val idx = videoPlayer.currentMediaItemIndex
                    val range = ranges.getOrNull(idx)
                    if (range != null) {
                        val playerPos = videoPlayer.currentPosition.coerceAtLeast(0L)
                        val globalMs = (range.timelineStartMs + (playerPos / range.clip.speed).toLong())
                            .coerceIn(0L, details.totalDurationMs)
                        _uiState.update { it.copy(playheadMs = globalMs) }
                    }
                }
                delay(33L)
            }
        }
    }

    fun togglePlayPause() {
        val details = _uiState.value.projectDetails ?: return
        if (details.orderedClips.isEmpty()) return
        if (videoPlayer.isPlaying) {
            videoPlayer.pause()
        } else {
            if (_uiState.value.playheadMs >= details.totalDurationMs - 40L) {
                seekToTimelineMs(0L)
            }
            videoPlayer.play()
        }
    }

    fun seekToTimelineMs(timelineMs: Long) {
        val details = _uiState.value.projectDetails ?: return
        val clamped = timelineMs.coerceIn(0L, details.totalDurationMs.coerceAtLeast(0L))
        _uiState.update { it.copy(playheadMs = clamped) }
        seekPlayerOnly(details, clamped)
        applyCurrentClipPlaybackParams(details, clamped)
        refreshScopesSample(details, clamped)
    }

    private fun seekPlayerOnly(details: ProjectWithDetails, timelineMs: Long) {
        val pos = details.findClipAtTimelineMs(timelineMs) ?: return
        val localSourcePos = (pos.localOffsetMs * pos.clip.speed).toLong()
            .coerceIn(0L, pos.clip.trimmedSourceDurationMs)
        if (pos.clipIndex in 0 until videoPlayer.mediaItemCount) {
            videoPlayer.seekTo(pos.clipIndex, localSourcePos)
        }
    }

    fun stepFrame(deltaFrames: Int) {
        videoPlayer.pause()
        val fps = _uiState.value.projectDetails?.project?.fps ?: 30
        val frameMs = (1000L / fps.coerceIn(1, 60)).coerceAtLeast(16L)
        seekToTimelineMs(_uiState.value.playheadMs + deltaFrames * frameMs)
    }

    fun setWorkspaceMode(mode: WorkspaceMode) {
        _uiState.update { it.copy(workspaceMode = mode) }
        if (mode == WorkspaceMode.COLOR) {
            _uiState.value.projectDetails?.let { refreshScopesSample(it, _uiState.value.playheadMs) }
        }
    }

    fun selectClip(clipId: Long) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun selectOverlay(overlayId: Long) {
        _uiState.update { it.copy(selectedOverlayId = overlayId) }
    }

    fun adjustZoom(delta: Float) {
        _uiState.update { it.copy(timelineZoom = (it.timelineZoom + delta).coerceIn(0.5f, 3.0f)) }
    }

    private fun pushUndoSnapshot() {
        val details = _uiState.value.projectDetails ?: return
        if (undoStack.size >= 25) undoStack.removeFirst()
        undoStack.addLast(
            EditorSnapshot(
                project = details.project,
                clips = details.orderedClips,
                overlays = details.orderedOverlays
            )
        )
        redoStack.clear()
    }

    fun undo() {
        val current = _uiState.value.projectDetails ?: return
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(EditorSnapshot(current.project, current.orderedClips, current.orderedOverlays))
        viewModelScope.launch {
            repository.restoreSnapshot(prev.project, prev.clips, prev.overlays)
        }
    }

    fun redo() {
        val current = _uiState.value.projectDetails ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(EditorSnapshot(current.project, current.orderedClips, current.orderedOverlays))
        viewModelScope.launch {
            repository.restoreSnapshot(next.project, next.clips, next.overlays)
        }
    }

    fun addMediaClips(uris: List<Uri>) {
        val details = _uiState.value.projectDetails ?: return
        if (uris.isEmpty()) return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.addMediaToProject(details.project.id, uris, details.orderedClips.size)
        }
    }

    fun addDemoReel() {
        val details = _uiState.value.projectDetails ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.addDemoReelToProject(details.project.id, details.orderedClips.size)
        }
    }

    fun splitAtPlayhead() {
        val details = _uiState.value.projectDetails ?: return
        val pos = details.findClipAtTimelineMs(_uiState.value.playheadMs) ?: return
        if (pos.clip.isImage) {
            showToast("لا يمكن تقسيم الصور الثابتة؛ يمكنك تعديل مدتها")
            return
        }
        val splitPoint = pos.sourcePositionMs
        if (splitPoint <= pos.clip.trimStartMs + 250L || splitPoint >= pos.clip.trimEndMs - 250L) {
            showToast("حرّك مؤشر التشغيل بعيداً عن حافة المقطع للتقسيم")
            return
        }
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.splitClipAt(details.project.id, details.orderedClips, pos.clip, splitPoint)
            showToast("تم تقسيم المقطع عند المؤشر")
        }
    }

    fun updateSelectedClipTrim(trimStartMs: Long, trimEndMs: Long) {
        val clip = _uiState.value.selectedClip ?: return
        val safeStart = trimStartMs.coerceIn(0L, (clip.sourceDurationMs - 300L).coerceAtLeast(0L))
        val safeEnd = trimEndMs.coerceIn(safeStart + 300L, clip.sourceDurationMs.coerceAtLeast(safeStart + 300L))
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateClip(clip.copy(trimStartMs = safeStart, trimEndMs = safeEnd))
        }
    }

    fun updateSelectedClipSpeed(speed: Float) {
        val clip = _uiState.value.selectedClip ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateClip(clip.copy(speed = speed.coerceIn(0.25f, 4.0f)))
        }
    }

    fun rotateSelectedClip() {
        val clip = _uiState.value.selectedClip ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateClip(clip.copy(rotationDegrees = (clip.rotationDegrees + 90) % 360))
        }
    }

    fun moveSelectedClip(direction: Int) {
        val details = _uiState.value.projectDetails ?: return
        val clips = details.orderedClips.toMutableList()
        val selected = _uiState.value.selectedClip ?: return
        val idx = clips.indexOfFirst { it.id == selected.id }
        val target = idx + direction
        if (idx < 0 || target !in clips.indices) return
        pushUndoSnapshot()
        clips.removeAt(idx)
        clips.add(target, selected)
        viewModelScope.launch {
            repository.updateClipsOrder(details.project.id, clips)
        }
    }

    fun duplicateSelectedClip() {
        val details = _uiState.value.projectDetails ?: return
        val clip = _uiState.value.selectedClip ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.duplicateClip(details.project.id, details.orderedClips, clip)
        }
    }

    fun deleteSelectedClip() {
        val details = _uiState.value.projectDetails ?: return
        val clip = _uiState.value.selectedClip ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.deleteClip(details.project.id, details.orderedClips, clip)
        }
    }

    fun toggleClipMute() {
        val clip = _uiState.value.selectedClip ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateClip(clip.copy(isMuted = !clip.isMuted))
        }
    }

    fun updateClipVolume(volume: Float) {
        val clip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            repository.updateClip(clip.copy(volume = volume.coerceIn(0f, 1.5f), isMuted = volume <= 0.01f))
        }
    }

    fun setLutPreset(lut: LutPreset) {
        val proj = _uiState.value.projectDetails?.project ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateProject(proj.copy(lutPreset = lut.name))
        }
    }

    fun setVisualEffect(preset: VisualEffectPreset) {
        val proj = _uiState.value.projectDetails?.project ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateProject(proj.copy(visualEffect = preset.name))
        }
    }

    fun setAudioEffect(preset: AudioEffectPreset) {
        val proj = _uiState.value.projectDetails?.project ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateProject(proj.copy(audioEffect = preset.name))
        }
    }

    fun setVoiceoverUri(uri: Uri?) {
        val proj = _uiState.value.projectDetails?.project ?: return
        pushUndoSnapshot()
        val title = uri?.let { repository.queryDisplayName(it) ?: "تسجيل صوتي (Voiceover)" }
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    voiceoverUri = uri?.toString(),
                    voiceoverTitle = title
                )
            )
        }
    }

    fun updateColorGrading(
        brightness: Float? = null,
        contrast: Float? = null,
        saturation: Float? = null,
        warmth: Float? = null,
        tint: Float? = null
    ) {
        val proj = _uiState.value.projectDetails?.project ?: return
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    brightness = brightness ?: proj.brightness,
                    contrast = contrast ?: proj.contrast,
                    saturation = saturation ?: proj.saturation,
                    warmth = warmth ?: proj.warmth,
                    tint = tint ?: proj.tint
                )
            )
        }
    }

    fun resetColorGrading() {
        val proj = _uiState.value.projectDetails?.project ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    lutPreset = LutPreset.NONE.name,
                    brightness = 0f,
                    contrast = 0f,
                    saturation = 0f,
                    warmth = 0f,
                    tint = 0f
                )
            )
        }
    }

    fun updateMasterAudio(masterVolume: Float? = null, musicVolume: Float? = null, musicMuted: Boolean? = null) {
        val proj = _uiState.value.projectDetails?.project ?: return
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    masterVolume = masterVolume ?: proj.masterVolume,
                    musicVolume = musicVolume ?: proj.musicVolume,
                    musicMuted = musicMuted ?: proj.musicMuted
                )
            )
        }
    }

    fun setBackgroundMusic(uri: Uri?) {
        val proj = _uiState.value.projectDetails?.project ?: return
        pushUndoSnapshot()
        val title = uri?.let { repository.queryDisplayName(it) ?: "موسيقى خلفية" }
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    musicUri = uri?.toString(),
                    musicTitle = title,
                    musicMuted = false
                )
            )
        }
    }

    fun addTextOverlay(text: String, style: TitleStyle = TitleStyle.CINEMATIC) {
        val details = _uiState.value.projectDetails ?: return
        pushUndoSnapshot()
        val start = _uiState.value.playheadMs.coerceIn(0L, (details.totalDurationMs - 500L).coerceAtLeast(0L))
        val end = (start + 3000L).coerceAtMost(details.totalDurationMs.coerceAtLeast(3000L))
        viewModelScope.launch {
            val id = repository.insertOverlay(
                TextOverlayEntity(
                    projectId = details.project.id,
                    text = text.ifBlank { "عنوان جديد" },
                    startMs = start,
                    endMs = end,
                    styleName = style.name
                )
            )
            _uiState.update { it.copy(selectedOverlayId = id) }
        }
    }

    fun updateSelectedOverlay(transform: (TextOverlayEntity) -> TextOverlayEntity) {
        val overlay = _uiState.value.selectedOverlay ?: return
        viewModelScope.launch {
            repository.updateOverlay(transform(overlay))
        }
    }

    fun deleteSelectedOverlay() {
        val overlay = _uiState.value.selectedOverlay ?: return
        pushUndoSnapshot()
        viewModelScope.launch {
            repository.deleteOverlay(overlay)
        }
    }

    fun updateProjectCanvas(
        name: String? = null,
        aspectRatio: AspectRatioOption? = null,
        fps: Int? = null,
        showSafeGuides: Boolean? = null
    ) {
        val proj = _uiState.value.projectDetails?.project ?: return
        viewModelScope.launch {
            repository.updateProject(
                proj.copy(
                    name = name ?: proj.name,
                    aspectRatio = aspectRatio?.name ?: proj.aspectRatio,
                    fps = fps ?: proj.fps,
                    showSafeGuides = showSafeGuides ?: proj.showSafeGuides
                )
            )
        }
    }

    fun openExportSheet() {
        videoPlayer.pause()
        _uiState.update { it.copy(showExportSheet = true, exportState = ExportUiState.Idle) }
    }

    fun closeExportSheet() {
        if (_uiState.value.exportState is ExportUiState.Exporting) {
            exporter.cancel()
        }
        _uiState.update { it.copy(showExportSheet = false, exportState = ExportUiState.Idle) }
    }

    fun setExportResolution(resolution: ExportResolution) {
        _uiState.update { it.copy(exportResolution = resolution) }
    }

    fun setExportCodec(codec: ExportCodec) {
        _uiState.update { it.copy(exportCodec = codec) }
    }

    fun startExport() {
        val details = _uiState.value.projectDetails ?: return
        exporter.startExport(
            projectWithDetails = details,
            resolution = _uiState.value.exportResolution,
            codec = _uiState.value.exportCodec
        ) { state ->
            _uiState.update { it.copy(exportState = state) }
        }
    }

    fun cancelExport() {
        exporter.cancel()
        _uiState.update { it.copy(exportState = ExportUiState.Idle) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        scopeJob?.cancel()
        exporter.cancel()
        videoPlayer.release()
        musicPlayer.release()
    }
}

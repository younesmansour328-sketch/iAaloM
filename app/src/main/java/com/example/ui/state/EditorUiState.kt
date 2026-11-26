package com.example.ui.state

import android.graphics.Bitmap
import com.example.data.model.ExportCodec
import com.example.data.model.ExportResolution
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectWithDetails
import com.example.data.model.TextOverlayEntity
import com.example.data.model.VideoClipEntity
import com.example.data.model.WorkspaceMode
import com.example.media.ColorScopesData
import com.example.media.ExportUiState

sealed interface ProjectsHubUiState {
    data object Loading : ProjectsHubUiState
    data class Success(
        val projects: List<ProjectWithDetails>,
        val isBusy: Boolean = false
    ) : ProjectsHubUiState
    data class Error(val message: String) : ProjectsHubUiState
}

data class EditorSnapshot(
    val project: ProjectEntity,
    val clips: List<VideoClipEntity>,
    val overlays: List<TextOverlayEntity>
)

data class EditorUiState(
    val isLoading: Boolean = true,
    val projectDetails: ProjectWithDetails? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedClipId: Long? = null,
    val selectedOverlayId: Long? = null,
    val workspaceMode: WorkspaceMode = WorkspaceMode.EDIT,
    val timelineZoom: Float = 1.0f,
    val filmstrips: Map<Long, List<Bitmap>> = emptyMap(),
    val scopes: ColorScopesData = ColorScopesData(),
    val exportResolution: ExportResolution = ExportResolution.FHD_1080P,
    val exportCodec: ExportCodec = ExportCodec.H264,
    val exportState: ExportUiState = ExportUiState.Idle,
    val showExportSheet: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusMessage: String? = null
) {
    val selectedClip: VideoClipEntity?
        get() {
            val clips = projectDetails?.orderedClips ?: return null
            return clips.find { it.id == selectedClipId } ?: clips.firstOrNull()
        }

    val selectedOverlay: TextOverlayEntity?
        get() {
            val overlays = projectDetails?.orderedOverlays ?: return null
            return overlays.find { it.id == selectedOverlayId } ?: overlays.firstOrNull()
        }

    val totalDurationMs: Long
        get() = projectDetails?.totalDurationMs ?: 0L
}

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.example.data.model.WorkspaceMode
import com.example.ui.components.ExportSheet
import com.example.ui.components.InspectorContainer
import com.example.ui.components.ProgramMonitor
import com.example.ui.components.TimelinePanel
import com.example.ui.theme.MelonCoral
import com.example.ui.theme.MelonMint
import com.example.ui.theme.ResolveAmber
import com.example.ui.theme.ResolveCyan
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.EditorViewModel

@UnstableApi
@Composable
fun EditorScreen(
    projectId: Long,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddMenu by remember { mutableStateOf(false) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addMediaClips(uris)
        }
    }

    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setBackgroundMusic(uri)
        }
    }

    var showVoiceRecordDialog by remember { mutableStateOf(false) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoiceRecordDialog = true
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    BackHandler {
        viewModel.videoPlayer.pause()
        onNavigateBack()
    }

    Scaffold(
        containerColor = StudioBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MelonCoral)
            }
            return@Scaffold
        }

        val details = uiState.projectDetails
        if (details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("تعذر فتح المشروع", color = TextSecondary)
            }
            return@Scaffold
        }

        val triggerAddMediaMenu: () -> Unit = { showAddMenu = true }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Studio Action Header
            StudioTopBar(
                projectName = details.project.name,
                aspectLabel = details.project.aspectRatioEnum.label,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onBack = {
                    viewModel.videoPlayer.pause()
                    onNavigateBack()
                },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onAspectClick = { viewModel.setWorkspaceMode(WorkspaceMode.CANVAS) },
                onExportClick = viewModel::openExportSheet
            )

            // Adaptive Studio Layout (Portrait Phone vs Wide/Tablet Split Pane)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isWideLayout = maxWidth >= 720.dp

                if (isWideLayout) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Navigation Rail on Wide Screens
                        NavigationRail(
                            containerColor = StudioSurface,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            workspaceTabs().forEach { tab ->
                                val selected = uiState.workspaceMode == tab.mode
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.setWorkspaceMode(tab.mode) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = tab.accent,
                                        selectedTextColor = tab.accent,
                                        indicatorColor = tab.accent.copy(alpha = 0.16f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }

                        // Monitor + Timeline Column
                        Column(
                            modifier = Modifier
                                .weight(1.35f)
                                .fillMaxHeight()
                        ) {
                            ProgramMonitor(
                                player = viewModel.videoPlayer,
                                projectDetails = details,
                                playheadMs = uiState.playheadMs,
                                isPlaying = uiState.isPlaying,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekStart = { viewModel.seekToTimelineMs(0L) },
                                onSeekEnd = { viewModel.seekToTimelineMs(details.totalDurationMs) },
                                onStepFrame = viewModel::stepFrame,
                                onToggleSafeGuides = {
                                    viewModel.updateProjectCanvas(showSafeGuides = !details.project.showSafeGuides)
                                },
                                onImportClick = triggerAddMediaMenu,
                                modifier = Modifier.weight(1f)
                            )
                            TimelinePanel(
                                projectDetails = details,
                                playheadMs = uiState.playheadMs,
                                selectedClipId = uiState.selectedClip?.id,
                                selectedOverlayId = uiState.selectedOverlay?.id,
                                timelineZoom = uiState.timelineZoom,
                                filmstrips = uiState.filmstrips,
                                onSeekMs = viewModel::seekToTimelineMs,
                                onSelectClip = viewModel::selectClip,
                                onSelectOverlay = viewModel::selectOverlay,
                                onZoomChange = viewModel::adjustZoom,
                                onAddMediaClick = triggerAddMediaMenu,
                                onSplitClick = viewModel::splitAtPlayhead,
                                onRotateClick = viewModel::rotateSelectedClip,
                                onDuplicateClick = viewModel::duplicateSelectedClip,
                                onDeleteClick = viewModel::deleteSelectedClip
                            )
                        }

                        // Side Inspector Dock
                        InspectorContainer(
                            uiState = uiState,
                            onUpdateTrim = viewModel::updateSelectedClipTrim,
                            onUpdateSpeed = viewModel::updateSelectedClipSpeed,
                            onMoveClip = viewModel::moveSelectedClip,
                            onToggleClipMute = viewModel::toggleClipMute,
                            onUpdateClipVolume = viewModel::updateClipVolume,
                            onSelectLut = viewModel::setLutPreset,
                            onSelectVisualEffect = viewModel::setVisualEffect,
                            onSelectAudioEffect = viewModel::setAudioEffect,
                            onRecordVoiceClick = { recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                            onClearVoiceClick = { viewModel.setVoiceoverUri(null) },
                            onUpdateColor = viewModel::updateColorGrading,
                            onResetColor = viewModel::resetColorGrading,
                            onUpdateMasterAudio = viewModel::updateMasterAudio,
                            onPickMusicClick = { musicPickerLauncher.launch(arrayOf("audio/*")) },
                            onClearMusicClick = { viewModel.setBackgroundMusic(null) },
                            onAddOverlay = viewModel::addTextOverlay,
                            onUpdateOverlay = viewModel::updateSelectedOverlay,
                            onDeleteOverlay = viewModel::deleteSelectedOverlay,
                            onSelectOverlay = viewModel::selectOverlay,
                            onUpdateAspectRatio = { viewModel.updateProjectCanvas(aspectRatio = it) },
                            onUpdateFps = { viewModel.updateProjectCanvas(fps = it) },
                            onToggleSafeGuides = {
                                viewModel.updateProjectCanvas(showSafeGuides = !details.project.showSafeGuides)
                            },
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .navigationBarsPadding()
                        )
                    }
                } else {
                    // Compact Phone Portrait Studio Layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProgramMonitor(
                            player = viewModel.videoPlayer,
                            projectDetails = details,
                            playheadMs = uiState.playheadMs,
                            isPlaying = uiState.isPlaying,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSeekStart = { viewModel.seekToTimelineMs(0L) },
                            onSeekEnd = { viewModel.seekToTimelineMs(details.totalDurationMs) },
                            onStepFrame = viewModel::stepFrame,
                            onToggleSafeGuides = {
                                viewModel.updateProjectCanvas(showSafeGuides = !details.project.showSafeGuides)
                            },
                            onImportClick = triggerAddMediaMenu,
                            modifier = Modifier.weight(0.38f)
                        )

                        TimelinePanel(
                            projectDetails = details,
                            playheadMs = uiState.playheadMs,
                            selectedClipId = uiState.selectedClip?.id,
                            selectedOverlayId = uiState.selectedOverlay?.id,
                            timelineZoom = uiState.timelineZoom,
                            filmstrips = uiState.filmstrips,
                            onSeekMs = viewModel::seekToTimelineMs,
                            onSelectClip = viewModel::selectClip,
                            onSelectOverlay = viewModel::selectOverlay,
                            onZoomChange = viewModel::adjustZoom,
                            onAddMediaClick = triggerAddMediaMenu,
                            onSplitClick = viewModel::splitAtPlayhead,
                            onRotateClick = viewModel::rotateSelectedClip,
                            onDuplicateClick = viewModel::duplicateSelectedClip,
                            onDeleteClick = viewModel::deleteSelectedClip
                        )

                        InspectorContainer(
                            uiState = uiState,
                            onUpdateTrim = viewModel::updateSelectedClipTrim,
                            onUpdateSpeed = viewModel::updateSelectedClipSpeed,
                            onMoveClip = viewModel::moveSelectedClip,
                            onToggleClipMute = viewModel::toggleClipMute,
                            onUpdateClipVolume = viewModel::updateClipVolume,
                            onSelectLut = viewModel::setLutPreset,
                            onSelectVisualEffect = viewModel::setVisualEffect,
                            onSelectAudioEffect = viewModel::setAudioEffect,
                            onRecordVoiceClick = { recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                            onClearVoiceClick = { viewModel.setVoiceoverUri(null) },
                            onUpdateColor = viewModel::updateColorGrading,
                            onResetColor = viewModel::resetColorGrading,
                            onUpdateMasterAudio = viewModel::updateMasterAudio,
                            onPickMusicClick = { musicPickerLauncher.launch(arrayOf("audio/*")) },
                            onClearMusicClick = { viewModel.setBackgroundMusic(null) },
                            onAddOverlay = viewModel::addTextOverlay,
                            onUpdateOverlay = viewModel::updateSelectedOverlay,
                            onDeleteOverlay = viewModel::deleteSelectedOverlay,
                            onSelectOverlay = viewModel::selectOverlay,
                            onUpdateAspectRatio = { viewModel.updateProjectCanvas(aspectRatio = it) },
                            onUpdateFps = { viewModel.updateProjectCanvas(fps = it) },
                            onToggleSafeGuides = {
                                viewModel.updateProjectCanvas(showSafeGuides = !details.project.showSafeGuides)
                            },
                            modifier = Modifier.weight(0.36f)
                        )

                        // Bottom Studio Workspace Switcher (Premiere / DaVinci Page Tabs)
                        NavigationBar(
                            containerColor = StudioSurfaceElevated,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            workspaceTabs().forEach { tab ->
                                val selected = uiState.workspaceMode == tab.mode
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.setWorkspaceMode(tab.mode) },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = tab.accent,
                                        selectedTextColor = tab.accent,
                                        indicatorColor = tab.accent.copy(alpha = 0.16f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("workspace_tab_${tab.mode.name}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Media Import Choice Menu (Gallery Photo Picker vs Built-in Cinema Reel)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DropdownMenu(
                expanded = showAddMenu,
                onDismissRequest = { showAddMenu = false },
                containerColor = StudioSurfaceElevated
            ) {
                DropdownMenuItem(
                    text = { Text("استيراد من المعرض (فيديو / صور)", color = TextPrimary) },
                    onClick = {
                        showAddMenu = false
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("إضافة مقطع استوديو تجريبي (MP4)", color = MelonMint) },
                    onClick = {
                        showAddMenu = false
                        viewModel.addDemoReel()
                    }
                )
            }
        }

        if (uiState.showExportSheet) {
            ExportSheet(
                projectDetails = details,
                selectedResolution = uiState.exportResolution,
                selectedCodec = uiState.exportCodec,
                exportState = uiState.exportState,
                onSelectResolution = viewModel::setExportResolution,
                onSelectCodec = viewModel::setExportCodec,
                onStartExport = viewModel::startExport,
                onCancelExport = viewModel::cancelExport,
                onDismiss = viewModel::closeExportSheet
            )
        }

        if (showVoiceRecordDialog) {
            com.example.ui.components.VoiceRecordDialog(
                onDismiss = { showVoiceRecordDialog = false },
                onRecordingSaved = { uri -> viewModel.setVoiceoverUri(uri) }
            )
        }
    }
}

@Composable
private fun StudioTopBar(
    projectName: String,
    aspectLabel: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAspectClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        color = StudioSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع للمشاريع",
                        tint = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                            .clickable(onClick = onAspectClick)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = aspectLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MelonMint
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("editor_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "تراجع",
                        tint = if (canUndo) TextPrimary else TextMuted
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("editor_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "إعادة",
                        tint = if (canRedo) TextPrimary else TextMuted
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onExportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MelonMint,
                        contentColor = StudioBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("editor_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Publish,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تصدير",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class WorkspaceTabItem(
    val mode: WorkspaceMode,
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

private fun workspaceTabs(): List<WorkspaceTabItem> = listOf(
    WorkspaceTabItem(WorkspaceMode.EDIT, "القص", Icons.Default.ContentCut, MelonCoral),
    WorkspaceTabItem(WorkspaceMode.COLOR, "الألوان", Icons.Default.ColorLens, ResolveAmber),
    WorkspaceTabItem(WorkspaceMode.AUDIO, "الصوت", Icons.Default.GraphicEq, MelonMint),
    WorkspaceTabItem(WorkspaceMode.TITLES, "النصوص", Icons.Default.Title, ResolveCyan),
    WorkspaceTabItem(WorkspaceMode.CANVAS, "الإطار", Icons.Default.AspectRatio, TextPrimary)
)

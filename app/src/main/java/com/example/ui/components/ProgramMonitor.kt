package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ProjectWithDetails
import com.example.data.model.TitleStyle
import com.example.ui.state.TimecodeFormatter
import com.example.ui.theme.MelonCoral
import com.example.ui.theme.MelonMint
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@UnstableApi
@Composable
fun ProgramMonitor(
    player: ExoPlayer,
    projectDetails: ProjectWithDetails?,
    playheadMs: Long,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    onStepFrame: (Int) -> Unit,
    onToggleSafeGuides: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project = projectDetails?.project
    val clips = projectDetails?.orderedClips.orEmpty()
    val aspectRatio = project?.aspectRatioEnum?.ratio ?: (16f / 9f)
    val fps = project?.fps ?: 30
    val totalMs = projectDetails?.totalDurationMs ?: 0L
    val activeOverlays = projectDetails?.orderedOverlays
        ?.filter { playheadMs in it.startMs..it.endMs }
        .orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBg)
    ) {
        // Monitor Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            ) {
                if (clips.isNotEmpty()) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        update = { view ->
                            if (view.player != player) {
                                view.player = player
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("program_monitor_player")
                    )
                } else {
                    // Empty monitor prompt
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onImportClick)
                            .padding(16.dp)
                            .testTag("empty_monitor_import_button"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "استيراد وسائط",
                            tint = MelonCoral,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "أضف فيديو أو صورة للبدء",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Live Text Overlays HUD
                activeOverlays.forEach { overlay ->
                    val alignment = when {
                        overlay.posY < 0.35f -> Alignment.TopCenter
                        overlay.posY > 0.65f -> Alignment.BottomCenter
                        else -> Alignment.Center
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = alignment
                    ) {
                        val bgColor = when (overlay.styleEnum) {
                            TitleStyle.LOWER_THIRD -> StudioSurface.copy(alpha = 0.85f)
                            TitleStyle.NEON_PILL -> MelonCoral.copy(alpha = 0.9f)
                            TitleStyle.CINEMATIC -> Color.Black.copy(alpha = 0.45f)
                            TitleStyle.MINIMAL -> Color.Transparent
                        }
                        Text(
                            text = overlay.text,
                            color = Color(overlay.colorArgb),
                            fontSize = (overlay.fontSizeSp * 0.72f).sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // DaVinci / Premiere Safe Title & Action Guides
                androidx.compose.animation.AnimatedVisibility(
                    visible = project?.showSafeGuides == true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val actionMarginX = size.width * 0.05f
                        val actionMarginY = size.height * 0.05f
                        drawRect(
                            color = MelonMint.copy(alpha = 0.45f),
                            topLeft = Offset(actionMarginX, actionMarginY),
                            size = Size(size.width - actionMarginX * 2, size.height - actionMarginY * 2),
                            style = Stroke(width = 1.5f)
                        )
                        val titleMarginX = size.width * 0.1f
                        val titleMarginY = size.height * 0.1f
                        drawRect(
                            color = Color.White.copy(alpha = 0.35f),
                            topLeft = Offset(titleMarginX, titleMarginY),
                            size = Size(size.width - titleMarginX * 2, size.height - titleMarginY * 2),
                            style = Stroke(width = 1f)
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }

        // Transport Control & SMPTE Timecode Bar (always LTR for industry timecode accuracy)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(
                color = StudioSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Timecode Readout
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = TimecodeFormatter.formatSmpte(playheadMs, fps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MelonCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / ${TimecodeFormatter.formatSmpte(totalMs, fps)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Pro Transport Cluster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onSeekStart,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("transport_start_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "العودة للبداية",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onStepFrame(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("transport_prev_frame_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "إطار للخلف",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MelonCoral)
                                .clickable(onClick = onTogglePlayPause)
                                .testTag("transport_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { onStepFrame(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("transport_next_frame_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "إطار للأمام",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onSeekEnd,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .testTag("transport_end_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "الانتقال للنهاية",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Safe Guides Toggle
                    IconButton(
                        onClick = onToggleSafeGuides,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (project?.showSafeGuides == true) StudioSurfaceElevated else Color.Transparent
                            )
                            .testTag("safe_guides_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "إظهار خطوط الأمان",
                            tint = if (project?.showSafeGuides == true) MelonMint else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectWithDetails
import com.example.data.model.VideoClipEntity
import com.example.ui.state.TimecodeFormatter
import com.example.ui.theme.MelonCoral
import com.example.ui.theme.MelonMint
import com.example.ui.theme.ResolveAmber
import com.example.ui.theme.ResolveCyan
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TimelinePanel(
    projectDetails: ProjectWithDetails?,
    playheadMs: Long,
    selectedClipId: Long?,
    selectedOverlayId: Long?,
    timelineZoom: Float,
    filmstrips: Map<Long, List<Bitmap>>,
    onSeekMs: (Long) -> Unit,
    onSelectClip: (Long) -> Unit,
    onSelectOverlay: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onAddMediaClick: () -> Unit,
    onSplitClick: () -> Unit,
    onRotateClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = (projectDetails?.totalDurationMs ?: 0L).coerceAtLeast(5000L)
    val pxPerSecondDp = (46f * timelineZoom).dp
    val density = LocalDensity.current
    val pxPerMs = with(density) { (pxPerSecondDp.toPx()) / 1000f }
    val timelineWidthDp: Dp = ((totalMs / 1000f) * pxPerSecondDp.value).dp.coerceAtLeast(320.dp) + 80.dp
    val scrollState = rememberScrollState()

    // Auto-follow playhead softly during playback
    LaunchedEffect(playheadMs) {
        val playheadPx = (playheadMs * pxPerMs).roundToInt()
        if (playheadPx > scrollState.value + 520 || playheadPx < scrollState.value) {
            scrollState.scrollTo((playheadPx - 140).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
    ) {
        // Premiere-style Razor & Timeline Quick Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurfaceElevated)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimelineToolChip(
                    icon = Icons.Default.Add,
                    label = "وسائط",
                    accent = MelonMint,
                    tag = "timeline_add_media_button",
                    onClick = onAddMediaClick
                )
                TimelineToolChip(
                    icon = Icons.Default.ContentCut,
                    label = "قص",
                    accent = MelonCoral,
                    tag = "timeline_split_button",
                    onClick = onSplitClick
                )
                TimelineToolChip(
                    icon = Icons.Default.RotateRight,
                    label = "تدوير",
                    accent = TextPrimary,
                    tag = "timeline_rotate_button",
                    onClick = onRotateClick
                )
                TimelineToolChip(
                    icon = Icons.Default.ContentCopy,
                    label = "تكرار",
                    accent = TextPrimary,
                    tag = "timeline_duplicate_button",
                    onClick = onDuplicateClick
                )
                TimelineToolChip(
                    icon = Icons.Default.DeleteOutline,
                    label = "حذف",
                    accent = MelonCoral,
                    tag = "timeline_delete_button",
                    onClick = onDeleteClick
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onZoomChange(-0.25f) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(34.dp)
                        .testTag("timeline_zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "تصغير الخط الزمني",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { onZoomChange(0.25f) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(34.dp)
                        .testTag("timeline_zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "تكبير الخط الزمني",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Multi-Track Timeline Canvas (always LTR)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioBg)
                    .padding(vertical = 6.dp)
            ) {
                // Track Headers Column (T1 / V1 / A2)
                Column(
                    modifier = Modifier
                        .width(38.dp)
                        .padding(start = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp)) // Ruler spacer
                    TrackBadge(label = "T1", color = ResolveCyan, height = 24.dp)
                    TrackBadge(label = "V1", color = MelonCoral, height = 56.dp)
                    TrackBadge(label = "A1", color = MelonMint, height = 30.dp)
                }

                // Scrollable Tracks + Timecode Ruler + Playhead Needle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .pointerInput(totalMs, pxPerMs) {
                            detectTapGestures { offset ->
                                val tappedMs = (offset.x / pxPerMs).toLong().coerceIn(0L, totalMs)
                                onSeekMs(tappedMs)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .width(timelineWidthDp)
                            .padding(end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Timecode Ruler
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        ) {
                            val totalSeconds = (totalMs / 1000L).toInt() + 2
                            for (sec in 0..totalSeconds) {
                                val x = sec * 1000f * pxPerMs
                                if (x <= size.width) {
                                    drawLine(
                                        color = TextMuted,
                                        start = Offset(x, size.height * 0.45f),
                                        end = Offset(x, size.height),
                                        strokeWidth = 1.5f
                                    )
                                    val halfX = (sec + 0.5f) * 1000f * pxPerMs
                                    if (halfX <= size.width) {
                                        drawLine(
                                            color = TextMuted.copy(alpha = 0.4f),
                                            start = Offset(halfX, size.height * 0.72f),
                                            end = Offset(halfX, size.height),
                                            strokeWidth = 1f
                                        )
                                    }
                                }
                            }
                        }

                        // 2. T1 Titles Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(StudioSurface.copy(alpha = 0.6f))
                        ) {
                            projectDetails?.orderedOverlays?.forEach { overlay ->
                                val startDp = (overlay.startMs * pxPerMs / density.density).dp
                                val widthDp = (((overlay.endMs - overlay.startMs).coerceAtLeast(300L)) * pxPerMs / density.density).dp
                                val isSelected = overlay.id == selectedOverlayId
                                Row(
                                    modifier = Modifier
                                        .offset(x = startDp)
                                        .width(widthDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(ResolveCyan.copy(alpha = 0.25f))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) ResolveCyan else ResolveCyan.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                        .clickable { onSelectOverlay(overlay.id) }
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Title,
                                        contentDescription = null,
                                        tint = ResolveCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = overlay.text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // 3. V1 Main Video / Image Filmstrip Track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val clips = projectDetails?.orderedClips.orEmpty()
                            if (clips.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StudioSurfaceHighlight)
                                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                        .clickable(onClick = onAddMediaClick),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ اضغط لإضافة مقاطع",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                clips.forEach { clip ->
                                    val clipWidthDp = ((clip.effectiveDurationMs * pxPerMs) / density.density)
                                        .dp.coerceAtLeast(48.dp)
                                    TimelineClipBlock(
                                        clip = clip,
                                        widthDp = clipWidthDp,
                                        isSelected = clip.id == selectedClipId,
                                        frames = filmstrips[clip.id].orEmpty(),
                                        onClick = { onSelectClip(clip.id) }
                                    )
                                }
                            }
                        }

                        // 4. A1 / A2 Audio Waveform Track
                        val hasMusic = !projectDetails?.project?.musicUri.isNullOrBlank()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hasMusic) MelonMint.copy(alpha = 0.14f)
                                    else StudioSurface.copy(alpha = 0.65f)
                                )
                                .border(
                                    1.dp,
                                    if (hasMusic) MelonMint.copy(alpha = 0.45f) else StudioBorder.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val bars = (size.width / 6f).toInt().coerceIn(20, 220)
                                val barColor = if (hasMusic) MelonMint else MelonMint.copy(alpha = 0.45f)
                                for (i in 0 until bars) {
                                    val amp = 0.25f + 0.7f * kotlin.math.abs(sin(i * 0.47f))
                                    val barH = size.height * 0.75f * amp
                                    val x = i * 6f + 3f
                                    drawLine(
                                        color = barColor,
                                        start = Offset(x, (size.height - barH) / 2f),
                                        end = Offset(x, (size.height + barH) / 2f),
                                        strokeWidth = 2.5f
                                    )
                                }
                            }
                            if (hasMusic) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MelonMint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = projectDetails?.project?.musicTitle ?: "Music",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Coral Playhead Needle
                    val playheadOffsetPx = (playheadMs * pxPerMs).roundToInt()
                    Column(
                        modifier = Modifier
                            .offset { IntOffset(playheadOffsetPx - 6, 0) }
                            .width(12.dp)
                            .height(152.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(MelonCoral)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(MelonCoral)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackBadge(label: String, color: Color, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimelineClipBlock(
    clip: VideoClipEntity,
    widthDp: Dp,
    isSelected: Boolean,
    frames: List<Bitmap>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurfaceHighlight)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MelonCoral else StudioBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag("timeline_clip_${clip.id}")
    ) {
        if (frames.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                frames.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = clip.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Scrim & Clip Metadata Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = clip.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimecodeFormatter.formatDuration(clip.effectiveDurationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MelonMint
                )
                if (kotlin.math.abs(clip.speed - 1.0f) > 0.05f) {
                    Text(
                        text = "${String.format("%.1f", clip.speed)}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ResolveAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineToolChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = StudioSurfaceHighlight,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(34.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

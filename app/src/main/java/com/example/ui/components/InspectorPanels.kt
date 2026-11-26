package com.example.ui.components

import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatioOption
import com.example.data.model.AudioEffectPreset
import com.example.data.model.LutPreset
import com.example.data.model.ProjectWithDetails
import com.example.data.model.TextOverlayEntity
import com.example.data.model.TitleStyle
import com.example.data.model.VideoClipEntity
import com.example.data.model.VisualEffectPreset
import com.example.data.model.WorkspaceMode
import com.example.media.ColorScopesData
import com.example.ui.state.EditorUiState
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun InspectorContainer(
    uiState: EditorUiState,
    onUpdateTrim: (Long, Long) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onMoveClip: (Int) -> Unit,
    onToggleClipMute: () -> Unit,
    onUpdateClipVolume: (Float) -> Unit,
    onSelectLut: (LutPreset) -> Unit,
    onSelectVisualEffect: (VisualEffectPreset) -> Unit,
    onSelectAudioEffect: (AudioEffectPreset) -> Unit,
    onRecordVoiceClick: () -> Unit,
    onClearVoiceClick: () -> Unit,
    onUpdateColor: (Float?, Float?, Float?, Float?, Float?) -> Unit,
    onResetColor: () -> Unit,
    onUpdateMasterAudio: (Float?, Float?, Boolean?) -> Unit,
    onPickMusicClick: () -> Unit,
    onClearMusicClick: () -> Unit,
    onAddOverlay: (String, TitleStyle) -> Unit,
    onUpdateOverlay: ((TextOverlayEntity) -> TextOverlayEntity) -> Unit,
    onDeleteOverlay: () -> Unit,
    onSelectOverlay: (Long) -> Unit,
    onUpdateAspectRatio: (AspectRatioOption) -> Unit,
    onUpdateFps: (Int) -> Unit,
    onToggleSafeGuides: () -> Unit,
    modifier: Modifier = Modifier
) {
    val details = uiState.projectDetails ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (uiState.workspaceMode) {
            WorkspaceMode.EDIT -> EditClipInspector(
                clip = uiState.selectedClip,
                totalClips = details.orderedClips.size,
                onUpdateTrim = onUpdateTrim,
                onUpdateSpeed = onUpdateSpeed,
                onMoveClip = onMoveClip,
                onToggleMute = onToggleClipMute
            )
            WorkspaceMode.COLOR -> ColorGradingInspector(
                details = details,
                scopes = uiState.scopes,
                onSelectLut = onSelectLut,
                onSelectVisualEffect = onSelectVisualEffect,
                onUpdateColor = onUpdateColor,
                onResetColor = onResetColor
            )
            WorkspaceMode.AUDIO -> AudioMixerInspector(
                details = details,
                selectedClip = uiState.selectedClip,
                isPlaying = uiState.isPlaying,
                onUpdateClipVolume = onUpdateClipVolume,
                onUpdateMasterAudio = onUpdateMasterAudio,
                onPickMusicClick = onPickMusicClick,
                onClearMusicClick = onClearMusicClick,
                onSelectAudioEffect = onSelectAudioEffect,
                onRecordVoiceClick = onRecordVoiceClick,
                onClearVoiceClick = onClearVoiceClick
            )
            WorkspaceMode.TITLES -> TitlesInspector(
                details = details,
                selectedOverlay = uiState.selectedOverlay,
                onAddOverlay = onAddOverlay,
                onSelectOverlay = onSelectOverlay,
                onUpdateOverlay = onUpdateOverlay,
                onDeleteOverlay = onDeleteOverlay
            )
            WorkspaceMode.CANVAS -> CanvasSettingsInspector(
                details = details,
                onUpdateAspectRatio = onUpdateAspectRatio,
                onUpdateFps = onUpdateFps,
                onToggleSafeGuides = onToggleSafeGuides
            )
        }
    }
}

@Composable
private fun EditClipInspector(
    clip: VideoClipEntity?,
    totalClips: Int,
    onUpdateTrim: (Long, Long) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onMoveClip: (Int) -> Unit,
    onToggleMute: () -> Unit
) {
    if (clip == null) {
        Text(
            text = "اختر مقطعاً من الخط الزمني لتعديل القص والسرعة",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clip.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "المدة الصافية: ${TimecodeFormatter.formatDuration(clip.effectiveDurationMs)} • الترتيب ${clip.orderIndex + 1}/$totalClips",
                style = MaterialTheme.typography.labelSmall,
                color = MelonMint
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = { onMoveClip(-1) },
                enabled = clip.orderIndex > 0,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clip_move_left_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "تحريك لليسار", tint = TextPrimary)
            }
            IconButton(
                onClick = { onMoveClip(1) },
                enabled = clip.orderIndex < totalClips - 1,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clip_move_right_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "تحريك لليمين", tint = TextPrimary)
            }
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clip_mute_button")
            ) {
                Icon(
                    imageVector = if (clip.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "كتم صوت المقطع",
                    tint = if (clip.isMuted) MelonCoral else MelonMint
                )
            }
        }
    }

    // Precision In/Out Trim RangeSlider
    if (!clip.isImage && clip.sourceDurationMs > 600L) {
        val maxDur = clip.sourceDurationMs.toFloat()
        var trimRange by remember(clip.id, clip.trimStartMs, clip.trimEndMs) {
            mutableStateOf(clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat())
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("نطاق التشذيب (In / Out)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    text = "${TimecodeFormatter.formatDuration(trimRange.start.toLong())} - ${TimecodeFormatter.formatDuration(trimRange.endInclusive.toLong())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MelonCoral
                )
            }
            RangeSlider(
                value = trimRange,
                onValueChange = { trimRange = it },
                onValueChangeFinished = {
                    onUpdateTrim(trimRange.start.toLong(), trimRange.endInclusive.toLong())
                },
                valueRange = 0f..maxDur,
                colors = SliderDefaults.colors(
                    thumbColor = MelonCoral,
                    activeTrackColor = MelonCoral,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.testTag("clip_trim_range_slider")
            )
        }
    }

    // Speed Control Chips + Slider
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("سرعة التشغيل", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text("${String.format("%.2f", clip.speed)}x", style = MaterialTheme.typography.labelSmall, color = ResolveAmber)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { spd ->
                val active = kotlin.math.abs(clip.speed - spd) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) ResolveAmber.copy(alpha = 0.22f) else StudioBg)
                        .border(1.dp, if (active) ResolveAmber else StudioBorder, RoundedCornerShape(8.dp))
                        .clickable { onUpdateSpeed(spd) }
                        .testTag("speed_preset_${spd}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${spd}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) ResolveAmber else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorGradingInspector(
    details: ProjectWithDetails,
    scopes: ColorScopesData,
    onSelectLut: (LutPreset) -> Unit,
    onSelectVisualEffect: (VisualEffectPreset) -> Unit,
    onUpdateColor: (Float?, Float?, Float?, Float?, Float?) -> Unit,
    onResetColor: () -> Unit
) {
    val p = details.project
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "استوديو الألوان وتأثيرات الفيديو",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        OutlinedButton(
            onClick = onResetColor,
            modifier = Modifier
                .height(36.dp)
                .testTag("color_reset_button")
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary)
            Spacer(modifier = Modifier.width(4.dp))
            Text("إعادة ضبط", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }

    // Live DaVinci RGB Parade & Luma Scope
    ColorScopeCanvas(scopes = scopes)

    // Cinema LUT Presets Strip
    Text("تدرج الألوان (LUTs)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LutPreset.entries.forEach { lut ->
            val selected = p.lutPresetEnum == lut
            Column(
                modifier = Modifier
                    .width(78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) StudioSurfaceHighlight else StudioSurfaceElevated)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ResolveAmber else StudioBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectLut(lut) }
                    .padding(6.dp)
                    .testTag("lut_preset_${lut.name}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(lut.previewAccentHex), Color(lut.secondaryAccentHex))
                            )
                        )
                )
                Text(
                    text = lut.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) ResolveAmber else TextPrimary,
                    maxLines = 1
                )
            }
        }
    }

    // Visual Effect Presets Strip
    Text("المؤثرات البصرية (Visual FX)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VisualEffectPreset.entries.forEach { vfx ->
            val selected = p.visualEffectEnum == vfx
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MelonCoral.copy(alpha = 0.22f) else StudioSurfaceElevated)
                    .border(1.dp, if (selected) MelonCoral else StudioBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelectVisualEffect(vfx) }
                    .padding(horizontal = 12.dp)
                    .testTag("vfx_preset_${vfx.name}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vfx.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MelonCoral else TextPrimary
                )
            }
        }
    }

    // Primary Color Correction Sliders
    StudioSliderRow(
        label = "التعرض / الإضاءة (Exposure)",
        value = p.brightness,
        range = -0.5f..0.5f,
        displayValue = String.format("%+.2f", p.brightness),
        accent = ResolveAmber,
        onValueChange = { onUpdateColor(it, null, null, null, null) }
    )
    StudioSliderRow(
        label = "التباين (Contrast)",
        value = p.contrast,
        range = -0.5f..0.5f,
        displayValue = String.format("%+.2f", p.contrast),
        accent = MelonCoral,
        onValueChange = { onUpdateColor(null, it, null, null, null) }
    )
    StudioSliderRow(
        label = "التشبع اللوني (Saturation)",
        value = p.saturation,
        range = -100f..100f,
        displayValue = String.format("%+.0f", p.saturation),
        accent = MelonMint,
        onValueChange = { onUpdateColor(null, null, it, null, null) }
    )
}

@Composable
private fun AudioMixerInspector(
    details: ProjectWithDetails,
    selectedClip: VideoClipEntity?,
    isPlaying: Boolean,
    onUpdateClipVolume: (Float) -> Unit,
    onUpdateMasterAudio: (Float?, Float?, Boolean?) -> Unit,
    onPickMusicClick: () -> Unit,
    onClearMusicClick: () -> Unit,
    onSelectAudioEffect: (AudioEffectPreset) -> Unit,
    onRecordVoiceClick: () -> Unit,
    onClearVoiceClick: () -> Unit
) {
    val p = details.project
    val transition = rememberInfiniteTransition(label = "vu_meter")
    val pulse by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "vu_level"
    )

    // Animated Stereo VU Meter Strip
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("MASTER dB", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val meterLevel = if (isPlaying) (p.masterVolume * pulse).coerceIn(0.05f, 1f) else 0.05f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StudioSurfaceHighlight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(meterLevel)
                        .height(6.dp)
                        .background(Brush.horizontalGradient(listOf(MelonMint, ResolveAmber, MelonCoral)))
                )
            }
        }
    }

    StudioSliderRow(
        label = "مستوى الصوت العام (Master)",
        value = p.masterVolume,
        range = 0f..1.5f,
        displayValue = "${(p.masterVolume * 100).toInt()}%",
        accent = MelonMint,
        onValueChange = { onUpdateMasterAudio(it, null, null) }
    )

    // Voiceover Recording Card (Prominent Voice Recording Button)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = MelonCoral, modifier = Modifier.size(18.dp))
                Text(
                    text = p.voiceoverTitle ?: "التسجيل الصوتي (Voiceover)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onRecordVoiceClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MelonCoral, contentColor = Color.White),
                    modifier = Modifier.height(36.dp).testTag("record_voice_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (p.voiceoverUri == null) "تسجيل صوتي" else "إعادة تسجيل")
                }
                if (p.voiceoverUri != null) {
                    IconButton(
                        onClick = onClearVoiceClick,
                        modifier = Modifier.minimumInteractiveComponentSize().size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف التسجيل", tint = MelonCoral)
                    }
                }
            }
        }
    }

    // Audio Effect Presets Strip
    Text("المؤثرات الصوتية (Audio FX)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AudioEffectPreset.entries.forEach { afx ->
            val selected = p.audioEffectEnum == afx
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MelonMint.copy(alpha = 0.22f) else StudioSurfaceElevated)
                    .border(1.dp, if (selected) MelonMint else StudioBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelectAudioEffect(afx) }
                    .padding(horizontal = 12.dp)
                    .testTag("afx_preset_${afx.name}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = afx.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MelonMint else TextPrimary
                )
            }
        }
    }

    // Background Music Track A2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MelonMint, modifier = Modifier.size(18.dp))
                Text(
                    text = p.musicTitle ?: "موسيقى خلفية (مكتبة)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onPickMusicClick,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("pick_music_button")
                ) {
                    Text(if (p.musicUri == null) "اختيار ملف" else "تغيير", style = MaterialTheme.typography.labelMedium, color = MelonMint)
                }
                if (p.musicUri != null) {
                    IconButton(
                        onClick = onClearMusicClick,
                        modifier = Modifier.minimumInteractiveComponentSize().size(36.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "إزالة الموسيقى", tint = MelonCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun TitlesInspector(
    details: ProjectWithDetails,
    selectedOverlay: TextOverlayEntity?,
    onAddOverlay: (String, TitleStyle) -> Unit,
    onSelectOverlay: (Long) -> Unit,
    onUpdateOverlay: ((TextOverlayEntity) -> TextOverlayEntity) -> Unit,
    onDeleteOverlay: () -> Unit
) {
    var newTitleInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = newTitleInput,
            onValueChange = { newTitleInput = it },
            placeholder = { Text("اكتب نص العنوان أو الترجمة...", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("new_title_input")
        )
        Button(
            onClick = {
                onAddOverlay(newTitleInput.ifBlank { "MELON STUDIO" }, TitleStyle.CINEMATIC)
                newTitleInput = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = ResolveCyan, contentColor = StudioBg),
            modifier = Modifier
                .height(52.dp)
                .testTag("add_title_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("إضافة")
        }
    }

    if (details.orderedOverlays.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            details.orderedOverlays.forEach { overlay ->
                val active = overlay.id == selectedOverlay?.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) ResolveCyan.copy(alpha = 0.22f) else StudioSurfaceElevated)
                        .border(1.dp, if (active) ResolveCyan else StudioBorder, RoundedCornerShape(8.dp))
                        .clickable { onSelectOverlay(overlay.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(overlay.text, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                }
            }
        }
    }

    if (selectedOverlay != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StudioSurfaceElevated)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تعديل العنوان المحدد", style = MaterialTheme.typography.titleMedium, color = ResolveCyan)
                IconButton(
                    onClick = onDeleteOverlay,
                    modifier = Modifier.minimumInteractiveComponentSize().size(36.dp).testTag("delete_title_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف العنوان", tint = MelonCoral)
                }
            }

            // Style Presets
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TitleStyle.entries.forEach { style ->
                    val active = selectedOverlay.styleEnum == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) ResolveCyan.copy(alpha = 0.22f) else StudioBg)
                            .border(1.dp, if (active) ResolveCyan else StudioBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                onUpdateOverlay { it.copy(styleName = style.name) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(style.displayName, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
            }

            // Color Swatches
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("اللون:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                listOf(0xFFFFFFFF, 0xFFFF4D6D, 0xFF2EE59D, 0xFFFFB703, 0xFF38BDF8).forEach { argb ->
                    val active = selectedOverlay.colorArgb == argb
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(if (active) 2.5.dp else 1.dp, if (active) Color.White else StudioBorder, CircleShape)
                            .clickable { onUpdateOverlay { it.copy(colorArgb = argb) } }
                    )
                }
            }

            StudioSliderRow(
                label = "حجم الخط",
                value = selectedOverlay.fontSizeSp,
                range = 14f..42f,
                displayValue = "${selectedOverlay.fontSizeSp.toInt()}sp",
                accent = ResolveCyan,
                onValueChange = { sz -> onUpdateOverlay { it.copy(fontSizeSp = sz) } }
            )

            StudioSliderRow(
                label = "الموضع العمودي",
                value = selectedOverlay.posY,
                range = 0.15f..0.88f,
                displayValue = "${(selectedOverlay.posY * 100).toInt()}%",
                accent = ResolveCyan,
                onValueChange = { y -> onUpdateOverlay { it.copy(posY = y) } }
            )
        }
    }
}

@Composable
private fun CanvasSettingsInspector(
    details: ProjectWithDetails,
    onUpdateAspectRatio: (AspectRatioOption) -> Unit,
    onUpdateFps: (Int) -> Unit,
    onToggleSafeGuides: () -> Unit
) {
    val p = details.project
    Text("نسبة العرض (Aspect Ratio)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AspectRatioOption.entries.forEach { ratio ->
            val selected = p.aspectRatioEnum == ratio
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MelonCoral.copy(alpha = 0.2f) else StudioSurfaceElevated)
                    .border(1.dp, if (selected) MelonCoral else StudioBorder, RoundedCornerShape(10.dp))
                    .clickable { onUpdateAspectRatio(ratio) }
                    .testTag("aspect_ratio_${ratio.name}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ratio.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MelonCoral else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Text("معدل الإطارات (Frame Rate)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(24, 30, 60).forEach { fps ->
            val selected = p.fps == fps
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MelonMint.copy(alpha = 0.2f) else StudioSurfaceElevated)
                    .border(1.dp, if (selected) MelonMint else StudioBorder, RoundedCornerShape(10.dp))
                    .clickable { onUpdateFps(fps) }
                    .testTag("fps_option_$fps"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${fps}fps",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MelonMint else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("خطوط الأمان السينمائية (Safe Guides)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("تحديد هوامش العناوين والحركة", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = p.showSafeGuides,
            onCheckedChange = { onToggleSafeGuides() },
            colors = SwitchDefaults.colors(checkedThumbColor = StudioBg, checkedTrackColor = MelonMint)
        )
    }
}

@Composable
fun VoiceRecordDialog(
    onDismiss: () -> Unit,
    onRecordingSaved: (Uri) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (isRecording) {
                runCatching {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                }
            }
            onDismiss()
        },
        containerColor = StudioSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("تسجيل تعليق صوتي (Voiceover)") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isRecording) "جاري التسجيل الآن... تحدث بوضوح عبر الميكروفون" else "اضغط على زر الميكروفون للبدء بالتسجيل الصوتي للمشروع",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) MelonCoral else TextSecondary,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) MelonCoral.copy(alpha = 0.25f) else StudioSurfaceElevated)
                        .border(2.5.dp, if (isRecording) MelonCoral else StudioBorder, CircleShape)
                        .clickable {
                            if (!isRecording) {
                                val file = File(context.cacheDir, "voiceover_${System.currentTimeMillis()}.m4a")
                                outputFile = file
                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setOutputFile(file.absolutePath)
                                    prepare()
                                    start()
                                }
                                mediaRecorder = recorder
                                isRecording = true
                            } else {
                                runCatching {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                }
                                mediaRecorder = null
                                isRecording = false
                                outputFile?.let {
                                    onRecordingSaved(Uri.fromFile(it))
                                }
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "تسجيل",
                        tint = if (isRecording) MelonCoral else MelonMint,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Text(
                    text = if (isRecording) "اضغط مرة أخرى للإيقاف والحفظ" else "اضغط لبدء التسجيل",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                if (isRecording) {
                    runCatching {
                        mediaRecorder?.stop()
                        mediaRecorder?.release()
                    }
                }
                onDismiss()
            }) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun StudioSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(text = displayValue, style = MaterialTheme.typography.labelSmall, color = accent)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = StudioBorder
            )
        )
    }
}

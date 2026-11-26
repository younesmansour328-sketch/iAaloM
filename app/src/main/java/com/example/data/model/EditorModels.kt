package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class AspectRatioOption(
    val label: String,
    val ratio: Float,
    val exportWidth1080: Int,
    val exportHeight1080: Int
) {
    RATIO_16_9("16:9", 16f / 9f, 1920, 1080),
    RATIO_9_16("9:16", 9f / 16f, 1080, 1920),
    RATIO_1_1("1:1", 1f, 1080, 1080),
    RATIO_4_5("4:5", 4f / 5f, 1080, 1350),
    RATIO_21_9("21:9", 21f / 9f, 1920, 822)
}

enum class LutPreset(
    val displayName: String,
    val previewAccentHex: Long,
    val secondaryAccentHex: Long
) {
    NONE("أصلي", 0xFFA0A6B8, 0xFF6B7285),
    TEAL_ORANGE("Teal & Orange", 0xFF00B4D8, 0xFFFF7B00),
    KODAK_2383("Kodak 2383", 0xFFE09F3E, 0xFF335C67),
    NOIR_BW("Noir B&W", 0xFFE5E7EB, 0xFF374151),
    MELON_POP("Melon Pop", 0xFFFF4D6D, 0xFF2EE59D),
    CYBER_NEON("Cyber Neon", 0xFFD946EF, 0xFF06B6D4),
    GOLDEN_HOUR("Golden Hour", 0xFFFFB703, 0xFFFB8500),
    BLEACH_BYPASS("Bleach Bypass", 0xFF94A3B8, 0xFF1E293B)
}

enum class WorkspaceMode {
    EDIT,
    COLOR,
    AUDIO,
    TITLES,
    CANVAS
}

enum class TitleStyle(val displayName: String) {
    CINEMATIC("سينمائي"),
    LOWER_THIRD("شريط سفلي"),
    NEON_PILL("بطاقة نيون"),
    MINIMAL("بسيط")
}

enum class ExportResolution(val label: String, val shortSidePx: Int) {
    HD_720P("720p HD", 720),
    FHD_1080P("1080p FHD", 1080),
    UHD_4K("4K UHD", 2160)
}

enum class ExportCodec(val label: String, val mimeType: String) {
    H264("H.264 / AVC", "video/avc"),
    H265("H.265 / HEVC", "video/hevc")
}

enum class VisualEffectPreset(val displayName: String) {
    NONE("بدون تأثير بصرية"),
    GLITCH("تشويش رقمي (Glitch)"),
    BLOOM("توهج سينمائي (Bloom)"),
    VINTAGE_GRAV("حبيبات سينمائية (Film Grain)")
}

enum class AudioEffectPreset(val displayName: String) {
    NORMAL("صوت نقي عادي"),
    ECHO("صدى استوديو (Echo)"),
    DEEP("صوت سينمائي عميق (Deep)"),
    HIGH("صوت حاد (High)")
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val aspectRatio: String = AspectRatioOption.RATIO_16_9.name,
    val fps: Int = 30,
    val lutPreset: String = LutPreset.NONE.name,
    val brightness: Float = 0f,      // -0.5f .. 0.5f
    val contrast: Float = 0f,        // -0.5f .. 0.5f
    val saturation: Float = 0f,      // -100f .. 100f
    val warmth: Float = 0f,          // -1f .. 1f
    val tint: Float = 0f,            // -1f .. 1f
    val masterVolume: Float = 1f,    // 0f .. 1.5f
    val musicUri: String? = null,
    val musicTitle: String? = null,
    val musicVolume: Float = 0.65f,
    val musicMuted: Boolean = false,
    val musicFadeInMs: Long = 500L,
    val musicFadeOutMs: Long = 1000L,
    val voiceoverUri: String? = null,
    val voiceoverTitle: String? = null,
    val voiceoverVolume: Float = 0.85f,
    val visualEffect: String = VisualEffectPreset.NONE.name,
    val audioEffect: String = AudioEffectPreset.NORMAL.name,
    val showSafeGuides: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val aspectRatioEnum: AspectRatioOption
        get() = runCatching { AspectRatioOption.valueOf(aspectRatio) }
            .getOrDefault(AspectRatioOption.RATIO_16_9)

    val lutPresetEnum: LutPreset
        get() = runCatching { LutPreset.valueOf(lutPreset) }
            .getOrDefault(LutPreset.NONE)

    val visualEffectEnum: VisualEffectPreset
        get() = runCatching { VisualEffectPreset.valueOf(visualEffect) }
            .getOrDefault(VisualEffectPreset.NONE)

    val audioEffectEnum: AudioEffectPreset
        get() = runCatching { AudioEffectPreset.valueOf(audioEffect) }
            .getOrDefault(AudioEffectPreset.NORMAL)
}

@Entity(
    tableName = "video_clips",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class VideoClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val orderIndex: Int,
    val sourceUri: String,
    val displayName: String,
    val isImage: Boolean = false,
    val sourceDurationMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val speed: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
) {
    val trimmedSourceDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(200L)

    val effectiveDurationMs: Long
        get() = (trimmedSourceDurationMs / speed.coerceIn(0.25f, 4.0f)).toLong().coerceAtLeast(200L)
}

@Entity(
    tableName = "text_overlays",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TextOverlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 3000L,
    val posX: Float = 0.5f,
    val posY: Float = 0.8f,
    val fontSizeSp: Float = 22f,
    val colorArgb: Long = 0xFFFFFFFF,
    val styleName: String = TitleStyle.CINEMATIC.name
) {
    val styleEnum: TitleStyle
        get() = runCatching { TitleStyle.valueOf(styleName) }
            .getOrDefault(TitleStyle.CINEMATIC)
}

data class ClipTimelineRange(
    val clip: VideoClipEntity,
    val timelineStartMs: Long,
    val timelineEndMs: Long
)

data class ClipTimelinePosition(
    val clipIndex: Int,
    val clip: VideoClipEntity,
    val localOffsetMs: Long,
    val sourcePositionMs: Long
)

data class ProjectWithDetails(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val clips: List<VideoClipEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val textOverlays: List<TextOverlayEntity> = emptyList()
) {
    val orderedClips: List<VideoClipEntity>
        get() = clips.sortedBy { it.orderIndex }

    val orderedOverlays: List<TextOverlayEntity>
        get() = textOverlays.sortedBy { it.startMs }

    val totalDurationMs: Long
        get() = orderedClips.sumOf { it.effectiveDurationMs }

    fun clipTimelineRanges(): List<ClipTimelineRange> {
        var cursor = 0L
        return orderedClips.map { clip ->
            val end = cursor + clip.effectiveDurationMs
            val range = ClipTimelineRange(clip, cursor, end)
            cursor = end
            range
        }
    }

    fun findClipAtTimelineMs(timelineMs: Long): ClipTimelinePosition? {
        val ranges = clipTimelineRanges()
        if (ranges.isEmpty()) return null
        val clamped = timelineMs.coerceIn(0L, totalDurationMs.coerceAtLeast(1L))
        ranges.forEachIndexed { index, range ->
            val isLast = index == ranges.lastIndex
            if (clamped < range.timelineEndMs || isLast) {
                val localOffset = (clamped - range.timelineStartMs).coerceIn(0L, range.clip.effectiveDurationMs)
                val sourcePos = (range.clip.trimStartMs + (localOffset * range.clip.speed).toLong())
                    .coerceIn(range.clip.trimStartMs, range.clip.trimEndMs)
                return ClipTimelinePosition(
                    clipIndex = index,
                    clip = range.clip,
                    localOffsetMs = localOffset,
                    sourcePositionMs = sourcePos
                )
            }
        }
        return null
    }
}

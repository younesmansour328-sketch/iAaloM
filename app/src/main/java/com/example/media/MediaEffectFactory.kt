package com.example.media

import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import com.example.data.model.AspectRatioOption
import com.example.data.model.ExportResolution
import com.example.data.model.LutPreset
import com.example.data.model.ProjectEntity
import com.example.data.model.TextOverlayEntity
import com.example.data.model.TitleStyle
import com.example.data.model.VideoClipEntity
import com.example.data.model.VisualEffectPreset
import kotlin.math.abs

@UnstableApi
object MediaEffectFactory {

    /**
     * Builds the list of Media3 GL video effects for live ExoPlayer preview or Transformer export.
     */
    fun buildColorAndTransformEffects(
        project: ProjectEntity,
        clip: VideoClipEntity? = null,
        includePresentation: Boolean = false,
        exportResolution: ExportResolution = ExportResolution.FHD_1080P,
        overlays: List<TextOverlayEntity> = emptyList()
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        // 1. Clip Rotation
        val rotation = clip?.rotationDegrees ?: 0
        if (rotation % 360 != 0) {
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees((-rotation).toFloat())
                    .build()
            )
        }

        // 2. DaVinci-style Primary Color Grading (Brightness, Contrast, Saturation/Hue)
        val totalBrightness = project.brightness.coerceIn(-0.6f, 0.6f)
        if (abs(totalBrightness) > 0.01f) {
            effects.add(Brightness(totalBrightness))
        }

        val lutContrastBoost = when (project.lutPresetEnum) {
            LutPreset.NOIR_BW -> 0.22f
            LutPreset.BLEACH_BYPASS -> 0.28f
            LutPreset.KODAK_2383 -> 0.14f
            LutPreset.TEAL_ORANGE -> 0.12f
            LutPreset.MELON_POP -> 0.10f
            else -> 0f
        }
        val totalContrast = (project.contrast + lutContrastBoost).coerceIn(-0.8f, 0.8f)
        if (abs(totalContrast) > 0.01f) {
            effects.add(Contrast(totalContrast))
        }

        val lutSatBoost = when (project.lutPresetEnum) {
            LutPreset.BLEACH_BYPASS -> -45f
            LutPreset.MELON_POP -> 25f
            LutPreset.CYBER_NEON -> 35f
            LutPreset.GOLDEN_HOUR -> 18f
            else -> 0f
        }
        val totalSaturation = (project.saturation + lutSatBoost).coerceIn(-100f, 100f)
        if (abs(totalSaturation) > 0.5f) {
            effects.add(
                HslAdjustment.Builder()
                    .adjustSaturation(totalSaturation)
                    .build()
            )
        }

        // 3. Cinema LUTs & Color Temperature / Tint via RgbMatrix / RgbFilter
        if (project.lutPresetEnum == LutPreset.NOIR_BW) {
            effects.add(RgbFilter.createGrayscaleFilter())
        } else {
            val matrix = computeColorGradeMatrix(
                lut = project.lutPresetEnum,
                warmth = project.warmth,
                tint = project.tint
            )
            if (matrix != null) {
                effects.add(RgbMatrix { _, _ -> matrix })
            }
        }

        // 3b. Visual Effect Preset (Glitch, Bloom, Vintage Grain)
        when (project.visualEffectEnum) {
            VisualEffectPreset.GLITCH -> {
                // Cyberpunk neon tint glitch matrix
                val glitchMatrix = floatArrayOf(
                    1.25f, 0.05f, 0f, 0f,
                    0f, 0.90f, 0.15f, 0f,
                    0.05f, 0f, 1.35f, 0f,
                    0f, 0f, 0f, 1f
                )
                effects.add(RgbMatrix { _, _ -> glitchMatrix })
            }
            VisualEffectPreset.BLOOM -> {
                effects.add(Brightness(0.18f))
                effects.add(Contrast(0.15f))
            }
            VisualEffectPreset.VINTAGE_GRAV -> {
                val vintageMatrix = floatArrayOf(
                    1.10f, 0.02f, 0.05f, 0f,
                    0.03f, 1.02f, 0.02f, 0f,
                    0.01f, 0.02f, 0.88f, 0f,
                    0.02f, 0.01f, -0.01f, 1f
                )
                effects.add(RgbMatrix { _, _ -> vintageMatrix })
            }
            VisualEffectPreset.NONE -> Unit
        }

        // 4. Burned-in Text Overlays for Export
        if (overlays.isNotEmpty()) {
            val textureOverlays = overlays.map { overlay ->
                buildTextOverlay(overlay)
            }
            effects.add(OverlayEffect(textureOverlays))
        }

        // 5. Export Output Resolution & Aspect Ratio
        if (includePresentation) {
            val (w, h) = computeTargetDimensions(project.aspectRatioEnum, exportResolution)
            effects.add(Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        }

        return effects
    }

    fun computeTargetDimensions(
        aspectRatio: AspectRatioOption,
        resolution: ExportResolution
    ): Pair<Int, Int> {
        val shortSide = resolution.shortSidePx
        return when (aspectRatio) {
            AspectRatioOption.RATIO_16_9 -> (shortSide * 16 / 9).makeEven() to shortSide.makeEven()
            AspectRatioOption.RATIO_9_16 -> shortSide.makeEven() to (shortSide * 16 / 9).makeEven()
            AspectRatioOption.RATIO_1_1 -> shortSide.makeEven() to shortSide.makeEven()
            AspectRatioOption.RATIO_4_5 -> shortSide.makeEven() to (shortSide * 5 / 4).makeEven()
            AspectRatioOption.RATIO_21_9 -> (shortSide * 21 / 9).makeEven() to shortSide.makeEven()
        }
    }

    private fun Int.makeEven(): Int = if (this % 2 == 0) this else this - 1

    private fun buildTextOverlay(overlay: TextOverlayEntity): TextOverlay {
        val spannable = SpannableString(overlay.text).apply {
            setSpan(ForegroundColorSpan(overlay.colorArgb.toInt()), 0, length, 0)
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
            setSpan(RelativeSizeSpan((overlay.fontSizeSp / 18f).coerceIn(0.8f, 2.5f)), 0, length, 0)
            when (overlay.styleEnum) {
                TitleStyle.LOWER_THIRD -> setSpan(
                    BackgroundColorSpan(Color.argb(200, 18, 20, 28)),
                    0,
                    length,
                    0
                )
                TitleStyle.NEON_PILL -> setSpan(
                    BackgroundColorSpan(Color.argb(210, 255, 77, 109)),
                    0,
                    length,
                    0
                )
                else -> Unit
            }
        }
        // Convert normalized 0..1 UI coordinates to Media3 NDC -1..1 coordinates (y inverted)
        val ndcX = (overlay.posX * 2f - 1f).coerceIn(-0.85f, 0.85f)
        val ndcY = (1f - overlay.posY * 2f).coerceIn(-0.85f, 0.85f)
        val settings = StaticOverlaySettings.Builder()
            .setBackgroundFrameAnchor(ndcX, ndcY)
            .setOverlayFrameAnchor(0f, 0f)
            .build()
        return TextOverlay.createStaticTextOverlay(spannable, settings)
    }

    private fun computeColorGradeMatrix(
        lut: LutPreset,
        warmth: Float,
        tint: Float
    ): FloatArray? {
        val hasWarmth = abs(warmth) > 0.01f
        val hasTint = abs(tint) > 0.01f
        if (lut == LutPreset.NONE && !hasWarmth && !hasTint) return null

        var rScale = 1f
        var gScale = 1f
        var bScale = 1f
        var rToB = 0f
        var bToR = 0f
        var rOffset = 0f
        var gOffset = 0f
        var bOffset = 0f

        when (lut) {
            LutPreset.TEAL_ORANGE -> {
                rScale = 1.12f; gScale = 0.99f; bScale = 1.08f
                rToB = -0.06f; bToR = -0.04f
                rOffset = 0.02f; bOffset = 0.04f
            }
            LutPreset.KODAK_2383 -> {
                rScale = 1.10f; gScale = 1.03f; bScale = 0.90f
                rOffset = 0.02f; gOffset = 0.01f; bOffset = -0.02f
            }
            LutPreset.MELON_POP -> {
                rScale = 1.12f; gScale = 1.06f; bScale = 1.04f
                rOffset = 0.02f
            }
            LutPreset.CYBER_NEON -> {
                rScale = 1.14f; gScale = 0.92f; bScale = 1.22f
                bOffset = 0.04f
            }
            LutPreset.GOLDEN_HOUR -> {
                rScale = 1.18f; gScale = 1.05f; bScale = 0.82f
                rOffset = 0.03f; gOffset = 0.01f
            }
            LutPreset.BLEACH_BYPASS -> {
                rScale = 0.96f; gScale = 0.98f; bScale = 1.04f
            }
            LutPreset.NONE, LutPreset.NOIR_BW -> Unit
        }

        // Apply user warmth (-1 cool blue .. +1 warm amber) & tint (-1 green .. +1 magenta)
        rScale += warmth * 0.18f + tint * 0.08f
        gScale -= tint * 0.14f
        bScale -= warmth * 0.18f - tint * 0.08f

        // 4x4 column-major matrix for OpenGL ES
        return floatArrayOf(
            rScale, 0f, rToB, 0f,
            0f, gScale, 0f, 0f,
            bToR, 0f, bScale, 0f,
            rOffset, gOffset, bOffset, 1f
        )
    }
}

package com.example

import com.example.data.model.AspectRatioOption
import com.example.data.model.ExportResolution
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectWithDetails
import com.example.data.model.VideoClipEntity
import com.example.media.MediaEffectFactory
import com.example.ui.state.TimecodeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EditorTimelineMathTest {

    @Test
    fun `timeline resolves multi-clip speed and trim positions accurately`() {
        val project = ProjectEntity(id = 1L, name = "Test")
        val clip1 = VideoClipEntity(
            id = 10L,
            projectId = 1L,
            orderIndex = 0,
            sourceUri = "file:///a.mp4",
            displayName = "A",
            sourceDurationMs = 8000L,
            trimStartMs = 1000L,
            trimEndMs = 5000L, // 4000ms trimmed
            speed = 2.0f       // 2000ms effective
        )
        val clip2 = VideoClipEntity(
            id = 11L,
            projectId = 1L,
            orderIndex = 1,
            sourceUri = "file:///b.mp4",
            displayName = "B",
            sourceDurationMs = 6000L,
            trimStartMs = 0L,
            trimEndMs = 3000L, // 3000ms effective
            speed = 1.0f
        )
        val details = ProjectWithDetails(project = project, clips = listOf(clip2, clip1))

        assertEquals(5000L, details.totalDurationMs)

        val posInClip1 = details.findClipAtTimelineMs(1000L)
        assertNotNull(posInClip1)
        assertEquals(10L, posInClip1!!.clip.id)
        assertEquals(3000L, posInClip1.sourcePositionMs) // 1000ms trimStart + 1000ms * 2.0 speed

        val posInClip2 = details.findClipAtTimelineMs(3500L)
        assertNotNull(posInClip2)
        assertEquals(11L, posInClip2!!.clip.id)
        assertEquals(1500L, posInClip2.sourcePositionMs)
    }

    @Test
    fun `smpte timecode formats minutes seconds frames`() {
        assertEquals("00:02:15", TimecodeFormatter.formatSmpte(2500L, fps = 30))
        assertEquals("01:05:00", TimecodeFormatter.formatSmpte(65_000L, fps = 30))
    }

    @Test
    fun `target export dimensions stay even and respect aspect ratio`() {
        val (w16, h16) = MediaEffectFactory.computeTargetDimensions(
            AspectRatioOption.RATIO_16_9,
            ExportResolution.FHD_1080P
        )
        assertEquals(1920, w16)
        assertEquals(1080, h16)

        val (w9, h9) = MediaEffectFactory.computeTargetDimensions(
            AspectRatioOption.RATIO_9_16,
            ExportResolution.HD_720P
        )
        assertEquals(720, w9)
        assertEquals(1280, h9)
    }
}

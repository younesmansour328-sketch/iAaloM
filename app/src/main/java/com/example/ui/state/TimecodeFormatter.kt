package com.example.ui.state

object TimecodeFormatter {
    fun formatSmpte(ms: Long, fps: Int = 30): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val frames = (((safeMs % 1000L) * fps.coerceIn(1, 60)) / 1000L)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)
        } else {
            String.format("%02d:%02d:%02d", minutes, seconds, frames)
        }
    }

    fun formatDuration(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val tenths = (safeMs % 1000L) / 100L
        return if (minutes > 0) {
            String.format("%d:%02d", minutes, seconds)
        } else {
            String.format("%d.%ds", seconds, tenths)
        }
    }
}

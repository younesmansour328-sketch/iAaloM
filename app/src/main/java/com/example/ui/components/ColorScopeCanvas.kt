package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.media.ColorScopesData
import com.example.ui.theme.MelonCoral
import com.example.ui.theme.MelonMint
import com.example.ui.theme.ResolveCyan
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.TextMuted

/**
 * DaVinci Resolve-inspired live RGB Parade & Luma Waveform scope visualizer.
 */
@Composable
fun ColorScopeCanvas(
    scopes: ColorScopesData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RGB PARADE & LUMA SCOPE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeChannelLabel("R", MelonCoral)
                ScopeChannelLabel("G", MelonMint)
                ScopeChannelLabel("B", ResolveCyan)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // IRE Grid lines (25%, 50%, 75%)
                listOf(0.25f, 0.5f, 0.75f).forEach { yFrac ->
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.6f),
                        start = Offset(0f, size.height * yFrac),
                        end = Offset(size.width, size.height * yFrac),
                        strokeWidth = 1f
                    )
                }

                drawChannelCurve(scopes.redBins, MelonCoral.copy(alpha = 0.85f))
                drawChannelCurve(scopes.greenBins, MelonMint.copy(alpha = 0.85f))
                drawChannelCurve(scopes.blueBins, ResolveCyan.copy(alpha = 0.85f))
                drawChannelCurve(scopes.lumaBins, Color.White.copy(alpha = 0.65f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChannelCurve(
    bins: FloatArray,
    color: Color
) {
    if (bins.isEmpty()) return
    val path = Path()
    val stepX = size.width / (bins.size - 1).coerceAtLeast(1)
    bins.forEachIndexed { idx, v ->
        val x = idx * stepX
        val y = size.height - (v.coerceIn(0f, 1f) * size.height * 0.92f) - 2f
        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
    )
}

@Composable
private fun ScopeChannelLabel(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

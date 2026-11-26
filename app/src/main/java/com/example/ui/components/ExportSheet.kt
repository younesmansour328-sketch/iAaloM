package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExportCodec
import com.example.data.model.ExportResolution
import com.example.data.model.ProjectWithDetails
import com.example.media.ExportUiState
import com.example.ui.state.TimecodeFormatter
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MelonCoral
import com.example.ui.theme.MelonMint
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    projectDetails: ProjectWithDetails,
    selectedResolution: ExportResolution,
    selectedCodec: ExportCodec,
    exportState: ExportUiState,
    onSelectResolution: (ExportResolution) -> Unit,
    onSelectCodec: (ExportCodec) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MovieCreation,
                    contentDescription = null,
                    tint = MelonMint,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "تصدير الفيديو النهائي",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${projectDetails.project.aspectRatioEnum.label} • ${projectDetails.project.fps}fps • المدة ${TimecodeFormatter.formatDuration(projectDetails.totalDurationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            when (exportState) {
                is ExportUiState.Idle -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "الدقة",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportResolution.entries.forEach { res ->
                                val selected = res == selectedResolution
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MelonCoral.copy(alpha = 0.2f) else StudioSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (selected) MelonCoral else StudioBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectResolution(res) }
                                        .testTag("export_res_${res.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = res.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MelonCoral else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "الترميز (Codec)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportCodec.entries.forEach { codec ->
                                val selected = codec == selectedCodec
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MelonMint.copy(alpha = 0.18f) else StudioSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (selected) MelonMint else StudioBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectCodec(codec) }
                                        .testTag("export_codec_${codec.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = codec.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MelonMint else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onStartExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MelonMint,
                            contentColor = StudioBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_export_button")
                    ) {
                        Text(
                            text = "بدء المعالجة والتصدير (MP4)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is ExportUiState.Exporting -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioSurfaceElevated)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { exportState.progressPercent / 100f },
                            color = MelonMint,
                            trackColor = StudioBorder,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "جاري التصيير... ${exportState.progressPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progressPercent / 100f },
                            color = MelonMint,
                            trackColor = StudioBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        OutlinedButton(
                            onClick = onCancelExport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("cancel_export_button")
                        ) {
                            Text("إلغاء التصدير", color = MelonCoral)
                        }
                    }
                }

                is ExportUiState.Completed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioSurfaceElevated)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "اكتمل التصدير",
                            tint = MelonMint,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "تم التصدير بنجاح!",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (exportState.gallerySaved) {
                                "تم الحفظ في Movies/MelonEditor (${String.format("%.1f", exportState.fileSizeMb)} MB)"
                            } else {
                                "جاهز للمشاركة (${String.format("%.1f", exportState.fileSizeMb)} MB)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, exportState.shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "مشاركة الفيديو")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MelonCoral,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_exported_video_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة")
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("close_export_sheet_button")
                            ) {
                                Text("تم", color = TextPrimary)
                            }
                        }
                    }
                }

                is ExportUiState.Failed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioSurfaceElevated)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "خطأ في التصدير",
                            tint = DangerRed,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Button(
                            onClick = onStartExport,
                            colors = ButtonDefaults.buttonColors(containerColor = MelonCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("إعادة المحاولة")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

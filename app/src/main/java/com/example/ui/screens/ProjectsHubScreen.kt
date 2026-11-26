package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AspectRatioOption
import com.example.data.model.ProjectWithDetails
import com.example.ui.state.ProjectsHubUiState
import com.example.ui.state.TimecodeFormatter
import com.example.ui.theme.MelonCoral
import com.example.ui.theme.MelonMint
import com.example.ui.theme.ResolveAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ProjectsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsHubScreen(
    viewModel: ProjectsViewModel,
    onOpenProject: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var selectedAspectRatio by remember { mutableStateOf(AspectRatioOption.RATIO_16_9) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.createProjectFromMedia(uris, selectedAspectRatio, onOpenProject)
        }
    }

    Scaffold(
        containerColor = StudioBg
    ) { innerPadding ->
        when (val state = uiState) {
            is ProjectsHubUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MelonCoral)
                }
            }

            is ProjectsHubUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = MelonCoral, style = MaterialTheme.typography.bodyLarge)
                }
            }

            is ProjectsHubUiState.Success -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 260.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 12.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Studio Brand Header
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_melon_logo_1795193030018),
                                        contentDescription = "شعار MelonEditor",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Column {
                                        Text(
                                            text = "MelonEditor",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Premiere • Resolve Mobile Studio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MelonMint
                                        )
                                    }
                                }
                                Surface(
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                                ) {
                                    Text(
                                        text = "${state.projects.size} مشاريع",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Hero Studio Card with Generated Visual Art
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_studio_hero_1795193044334),
                                    contentDescription = "استوديو المونتاج الاحترافي",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    StudioBg.copy(alpha = 0.25f),
                                                    StudioBg.copy(alpha = 0.92f)
                                                )
                                            )
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.align(Alignment.BottomStart),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "مونتاج سينمائي وتصحيح ألوان احترافي",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = TextPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    mediaPickerLauncher.launch(
                                                        PickVisualMediaRequest(
                                                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                        )
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MelonCoral,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("hub_import_media_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VideoLibrary,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("استيراد وسائط", maxLines = 1)
                                            }

                                            OutlinedButton(
                                                onClick = { showNewProjectDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = StudioSurface.copy(alpha = 0.78f),
                                                    contentColor = TextPrimary
                                                ),
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .testTag("hub_new_project_button")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MelonMint)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("جديد")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Instant Demo Reel Action Strip
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                onClick = { viewModel.createDemoProject(onOpenProject) },
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MelonMint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .testTag("hub_demo_project_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MelonMint.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MelonMint
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "تجربة مشروع سينمائي جاهز (Demo)",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "يولّد 3 مقاطع MP4 حقيقية لاختبار القص والألوان والتصدير فوراً",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = "فتح المشروع التجريبي",
                                        tint = MelonMint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Projects Section Header
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "المشاريع المحفوظة",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (state.projects.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(StudioSurface)
                                        .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "لا توجد مشاريع بعد",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "استورد فيديو من جهازك أو اضغط على المشروع التجريبي أعلاه",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            items(state.projects, key = { it.project.id }) { details ->
                                ProjectCard(
                                    details = details,
                                    onClick = { onOpenProject(details.project.id) },
                                    onDuplicate = { viewModel.duplicateProject(details) },
                                    onDelete = { viewModel.deleteProject(details.project.id) }
                                )
                            }
                        }
                    }

                    if (state.isBusy) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StudioBg.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MelonMint)
                                Text(
                                    text = "جاري تجهيز مساحة العمل...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        var projectName by remember { mutableStateOf("مشروع ميلون") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = StudioSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("مشروع مونتاج جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("اسم المشروع") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_project_name_input")
                    )
                    Text("نسبة العرض", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            AspectRatioOption.RATIO_16_9,
                            AspectRatioOption.RATIO_9_16,
                            AspectRatioOption.RATIO_1_1
                        ).forEach { ratio ->
                            val selected = selectedAspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MelonCoral.copy(alpha = 0.2f) else StudioSurfaceElevated)
                                    .border(1.dp, if (selected) MelonCoral else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedAspectRatio = ratio },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MelonCoral else TextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewProjectDialog = false
                        viewModel.createEmptyProject(projectName, selectedAspectRatio, onOpenProject)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MelonCoral),
                    modifier = Modifier.testTag("confirm_new_project_button")
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    details: ProjectWithDetails,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateLabel = remember(details.project.updatedAt) {
        SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()).format(Date(details.project.updatedAt))
    }

    Surface(
        onClick = onClick,
        color = StudioSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .testTag("project_card_${details.project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MelonCoral.copy(alpha = 0.35f), StudioSurfaceElevated, MelonMint.copy(alpha = 0.25f))
                        )
                    )
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = details.project.aspectRatioEnum.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = details.project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimecodeFormatter.formatDuration(details.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MelonMint
                    )
                    Text(
                        text = "${details.orderedClips.size} مقاطع",
                        style = MaterialTheme.typography.labelMedium,
                        color = ResolveAmber
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("project_menu_button_${details.project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات المشروع",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = StudioSurfaceElevated
                ) {
                    DropdownMenuItem(
                        text = { Text("تكرار المشروع", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MelonMint) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف", color = MelonCoral) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MelonCoral) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

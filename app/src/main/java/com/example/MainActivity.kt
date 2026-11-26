package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ProjectsHubScreen
import com.example.ui.theme.MelonEditorTheme
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.ProjectsViewModel

object MelonRoutes {
    const val ROUTE_PROJECTS_HUB = "projects_hub"
    const val ROUTE_EDITOR = "editor/{projectId}"

    fun editorRoute(projectId: Long): String = "editor/$projectId"
}

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MelonEditorTheme {
                MelonEditorNavApp()
            }
        }
    }
}

@UnstableApi
@Composable
fun MelonEditorNavApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MelonRoutes.ROUTE_PROJECTS_HUB
    ) {
        composable(MelonRoutes.ROUTE_PROJECTS_HUB) {
            val projectsViewModel: ProjectsViewModel = viewModel()
            ProjectsHubScreen(
                viewModel = projectsViewModel,
                onOpenProject = { projectId ->
                    navController.navigate(MelonRoutes.editorRoute(projectId))
                }
            )
        }

        composable(
            route = MelonRoutes.ROUTE_EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val editorViewModel: EditorViewModel = viewModel()
            EditorScreen(
                projectId = projectId,
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

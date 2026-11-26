package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MelonDatabase
import com.example.data.model.AspectRatioOption
import com.example.data.model.ProjectWithDetails
import com.example.data.repository.ProjectRepository
import com.example.ui.state.ProjectsHubUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(
        context = application,
        projectDao = MelonDatabase.getInstance(application).projectDao()
    )

    private val isBusy = MutableStateFlow(false)

    val uiState: StateFlow<ProjectsHubUiState> = combine(
        repository.observeAllProjects(),
        isBusy
    ) { projects, busy ->
        ProjectsHubUiState.Success(projects = projects, isBusy = busy) as ProjectsHubUiState
    }.catch { error ->
        emit(ProjectsHubUiState.Error(error.localizedMessage ?: "تعذر تحميل المشاريع"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProjectsHubUiState.Loading
    )

    fun createEmptyProject(
        name: String,
        aspectRatio: AspectRatioOption,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val id = repository.createEmptyProject(name.ifBlank { "مشروع جديد" }, aspectRatio)
                onCreated(id)
            } finally {
                isBusy.value = false
            }
        }
    }

    fun createProjectFromMedia(
        uris: List<Uri>,
        aspectRatio: AspectRatioOption,
        onCreated: (Long) -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                val id = repository.createProjectFromUris(
                    name = "مونتاج جديد",
                    uris = uris,
                    aspectRatio = aspectRatio
                )
                onCreated(id)
            } finally {
                isBusy.value = false
            }
        }
    }

    fun createDemoProject(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            isBusy.value = true
            try {
                val id = repository.createDemoProject()
                onCreated(id)
            } finally {
                isBusy.value = false
            }
        }
    }

    fun duplicateProject(project: ProjectWithDetails) {
        viewModelScope.launch {
            repository.duplicateProject(project)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }
}

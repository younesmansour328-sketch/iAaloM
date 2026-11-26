package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectWithDetails
import com.example.data.model.TextOverlayEntity
import com.example.data.model.VideoClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Transaction
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjectsWithDetails(): Flow<List<ProjectWithDetails>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectWithDetails(projectId: Long): Flow<ProjectWithDetails?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectWithDetails(projectId: Long): ProjectWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: VideoClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<VideoClipEntity>)

    @Update
    suspend fun updateClip(clip: VideoClipEntity)

    @Update
    suspend fun updateClips(clips: List<VideoClipEntity>)

    @Delete
    suspend fun deleteClip(clip: VideoClipEntity)

    @Query("DELETE FROM video_clips WHERE projectId = :projectId")
    suspend fun deleteAllClipsForProject(projectId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverlay(overlay: TextOverlayEntity): Long

    @Update
    suspend fun updateOverlay(overlay: TextOverlayEntity)

    @Delete
    suspend fun deleteOverlay(overlay: TextOverlayEntity)

    @Query("DELETE FROM text_overlays WHERE projectId = :projectId")
    suspend fun deleteAllOverlaysForProject(projectId: Long)

    @Transaction
    suspend fun replaceProjectSnapshot(
        project: ProjectEntity,
        clips: List<VideoClipEntity>,
        overlays: List<TextOverlayEntity>
    ) {
        updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        deleteAllClipsForProject(project.id)
        deleteAllOverlaysForProject(project.id)
        if (clips.isNotEmpty()) {
            insertClips(clips.mapIndexed { idx, c -> c.copy(projectId = project.id, orderIndex = idx) })
        }
        overlays.forEach { overlay ->
            insertOverlay(overlay.copy(projectId = project.id))
        }
    }
}

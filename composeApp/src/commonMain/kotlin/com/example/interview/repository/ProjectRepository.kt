package com.example.interview.repository

import com.example.interview.analytics.Analytics
import com.example.interview.api.ApiClient
import com.example.interview.model.Comment
import com.example.interview.model.Pano
import com.example.interview.model.Project
import com.example.interview.model.Room
import com.example.interview.model.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing projects. Uses in-memory storage with reactive StateFlow.
 *
 * This is the single source of truth for project data. Project is the aggregate root,
 * so all mutations to rooms, panos, and comments must be done through Project's
 * copyByXyz methods and then saved via this repository.
 */
class ProjectRepository(
    private val apiClient: ApiClient,
    private val analytics: Analytics
) {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())

    /**
     * Reactive flow of all projects. Collect from this to react to changes.
     */
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    init {
        populateSampleData()
    }

    /**
     * Saves or updates a project. If a project with the same ID exists, it is replaced.
     * If it's a new project, it is added to the list.
     *
     * @param project The project (aggregate root) to save
     */
    fun save(project: Project) {
        val currentProjects = _projects.value
        val existingIndex = currentProjects.indexOfFirst { it.id == project.id }
        val isNew = existingIndex < 0

        _projects.value = if (existingIndex >= 0) {
            currentProjects.toMutableList().apply {
                this[existingIndex] = project
            }
        } else {
            currentProjects + project
        }

        // Track analytics
        if (isNew) {
            analytics.trackEvent("entity_created", mapOf(
                "entity_type" to "project",
                "id" to project.id
            ))
        } else {
            analytics.trackEvent("entity_updated", mapOf(
                "entity_type" to "project",
                "id" to project.id
            ))
        }

        // Track nested entity changes
        project.rooms.forEach { room ->
            when (room.syncState) {
                SyncState.NEW -> analytics.trackEvent("entity_created", mapOf(
                    "entity_type" to "room",
                    "id" to room.id,
                    "project_id" to project.id
                ))
                SyncState.MODIFIED -> analytics.trackEvent("entity_updated", mapOf(
                    "entity_type" to "room",
                    "id" to room.id,
                    "project_id" to project.id
                ))
                SyncState.SYNCED -> { /* No tracking needed for synced entities */ }
            }

            room.pano?.let { pano ->
                when (pano.syncState) {
                    SyncState.NEW -> analytics.trackEvent("entity_created", mapOf(
                        "entity_type" to "pano",
                        "id" to pano.id,
                        "project_id" to project.id,
                        "room_id" to room.id
                    ))
                    SyncState.MODIFIED -> analytics.trackEvent("entity_updated", mapOf(
                        "entity_type" to "pano",
                        "id" to pano.id,
                        "project_id" to project.id,
                        "room_id" to room.id
                    ))
                    SyncState.DELETED -> analytics.trackEvent("entity_deleted", mapOf(
                        "entity_type" to "pano",
                        "id" to pano.id,
                        "project_id" to project.id,
                        "room_id" to room.id
                    ))
                    SyncState.SYNCED -> { /* No tracking needed for synced entities */ }
                }
            }

            room.comments.forEach { comment ->
                when (comment.syncState) {
                    SyncState.NEW -> analytics.trackEvent("entity_created", mapOf(
                        "entity_type" to "comment",
                        "id" to comment.id,
                        "project_id" to project.id,
                        "room_id" to room.id
                    ))
                    SyncState.MODIFIED -> analytics.trackEvent("entity_updated", mapOf(
                        "entity_type" to "comment",
                        "id" to comment.id,
                        "project_id" to project.id,
                        "room_id" to room.id
                    ))
                    SyncState.SYNCED -> { /* No tracking needed for synced entities */ }
                }
            }
        }

        // Track deletions (legacy support for deletedPanos/deletedPanoIds)
        project.deletedPanos.forEach { deletedPano ->
            analytics.trackEvent("entity_deleted", mapOf(
                "entity_type" to "pano",
                "id" to deletedPano.panoId,
                "project_id" to project.id,
                "room_id" to deletedPano.roomId
            ))
        }
        // Also track legacy deletedPanoIds
        project.deletedPanoIds.forEach { panoId ->
            if (!project.deletedPanos.any { it.panoId == panoId }) {
                analytics.trackEvent("entity_deleted", mapOf(
                    "entity_type" to "pano",
                    "id" to panoId,
                    "project_id" to project.id
                ))
            }
        }
    }

    /**
     * Uploads a project to the server. Only uploads entities that have changed (NEW or MODIFIED).
     */
    suspend fun uploadProject(projectId: String) {
        val project = _projects.value.find { it.id == projectId } ?: return

        analytics.trackEvent("upload_started", mapOf("project_id" to projectId))

        try {
            var itemsSyncedCount = 0

            // Upload project if new or modified
            when (project.syncState) {
                SyncState.NEW -> {
                    apiClient.createProject(project)
                    itemsSyncedCount++
                }
                SyncState.MODIFIED -> {
                    apiClient.updateProject(project)
                    itemsSyncedCount++
                }
                SyncState.SYNCED -> { /* Skip synced projects */ }
            }

            // Upload rooms
            project.rooms.forEach { room ->
                when (room.syncState) {
                    SyncState.NEW -> {
                        apiClient.createRoom(projectId, room)
                        itemsSyncedCount++
                    }
                    SyncState.MODIFIED -> {
                        apiClient.updateRoom(projectId, room)
                        itemsSyncedCount++
                    }
                    SyncState.SYNCED -> { /* Skip synced rooms */ }
                }

                // Upload pano if present and new
                room.pano?.let { pano ->
                    when (pano.syncState) {
                        SyncState.NEW -> {
                            apiClient.createPano(projectId, room.id, pano)
                            itemsSyncedCount++
                        }
                        SyncState.DELETED -> {
                            apiClient.deletePano(projectId, room.id, pano.id)
                            itemsSyncedCount++
                        }
                        SyncState.MODIFIED, SyncState.SYNCED -> { /* Skip */ }
                    }
                }

                // Upload comments
                room.comments.forEach { comment ->
                    when (comment.syncState) {
                        SyncState.NEW -> {
                            apiClient.createComment(projectId, room.id, comment)
                            itemsSyncedCount++
                        }
                        SyncState.MODIFIED -> {
                            apiClient.updateComment(projectId, room.id, comment)
                            itemsSyncedCount++
                        }
                        SyncState.SYNCED -> { /* Skip synced comments */ }
                    }
                }
            }

            // Delete panos (legacy support for deletedPanos/deletedPanoIds)
            project.deletedPanos.forEach { deletedPano ->
                apiClient.deletePano(projectId, deletedPano.roomId, deletedPano.panoId)
                itemsSyncedCount++
            }
            // Also handle legacy deletedPanoIds (for backward compatibility)
            project.deletedPanoIds.forEach { panoId ->
                // Try to find roomId from deletedPanos first
                val deletedPano = project.deletedPanos.find { it.panoId == panoId }
                if (deletedPano != null) {
                    // Already handled above
                } else {
                    // Fallback: try to find which room had this pano before deletion
                    // This is a best-effort approach for legacy data
                    // In practice, deletedPanos should be used going forward
                }
            }

            // After successful upload, mark all uploaded entities as SYNCED
            val updatedProject = markProjectAsSynced(project)
            updateProjectWithoutTracking(updatedProject)

            analytics.trackEvent("upload_completed", mapOf(
                "project_id" to projectId,
                "success" to true,
                "items_synced_count" to itemsSyncedCount
            ))
        } catch (e: Exception) {
            analytics.trackEvent("upload_failed", mapOf(
                "project_id" to projectId,
                "reason" to e.message ?: "Unknown error",
                "error_details" to e.toString()
            ))
            throw e
        }
    }

    private fun markProjectAsSynced(project: Project): Project {
        val syncedRooms = project.rooms.map { room ->
            // Remove panos that were successfully deleted (DELETED state)
            val syncedPano = when {
                room.pano?.syncState == SyncState.DELETED -> null
                room.pano != null -> room.pano.copy(syncState = SyncState.SYNCED)
                else -> null
            }
            val syncedComments = room.comments.map { it.copy(syncState = SyncState.SYNCED) }
            room.copy(
                syncState = SyncState.SYNCED,
                pano = syncedPano,
                comments = syncedComments
            )
        }
        return project.copy(
            syncState = SyncState.SYNCED,
            rooms = syncedRooms,
            deletedPanoIds = emptyList(),
            deletedPanos = emptyList()
        )
    }

    /**
     * Updates project state without triggering analytics tracking.
     * Used internally when syncing state after upload.
     */
    private fun updateProjectWithoutTracking(project: Project) {
        val currentProjects = _projects.value
        val existingIndex = currentProjects.indexOfFirst { it.id == project.id }

        _projects.value = if (existingIndex >= 0) {
            currentProjects.toMutableList().apply {
                this[existingIndex] = project
            }
        } else {
            currentProjects + project
        }
    }

    private fun populateSampleData() {
        // Sample Project 1: Water Damage Assessment
        // Create with SYNCED state since this is sample data from "server"
        val room1 = Room.make(name = "Living Room", id = "room-1").copy(syncState = SyncState.SYNCED)
        val room2 = Room.make(name = "Kitchen", id = "room-2").copy(syncState = SyncState.SYNCED)
        val room3 = Room.make(name = "Master Bedroom", id = "room-3").copy(syncState = SyncState.SYNCED)
        val pano1 = Pano(id = "pano-1", imageData = byteArrayOf(1, 2, 3), syncState = SyncState.SYNCED)
        val comment1 = Comment(id = "comment-1", text = "Water stain visible on ceiling", syncState = SyncState.SYNCED)
        val comment2 = Comment(id = "comment-2", text = "Carpet is damp near window", syncState = SyncState.SYNCED)
        val comment3 = Comment(id = "comment-3", text = "Under sink damage observed", syncState = SyncState.SYNCED)

        val sampleProject1 = Project(
            id = "sample-project-1",
            name = "123 Main St - Water Damage",
            syncState = SyncState.SYNCED
        )
            .copyByAddingRoom(room1)
            .copyByAddingRoom(room2)
            .copyByAddingRoom(room3)
            .copyBySettingPanoToRoom("room-1", pano1)
            .copyByAddingCommentToRoom("room-1", comment1)
            .copyByAddingCommentToRoom("room-1", comment2)
            .copyByAddingCommentToRoom("room-2", comment3)
            // Mark all as SYNCED since they were added with SYNCED state but copyBy* methods set them to NEW
            .let { project ->
                val syncedRooms = project.rooms.map { room ->
                    val syncedPano = room.pano?.copy(syncState = SyncState.SYNCED)
                    val syncedComments = room.comments.map { it.copy(syncState = SyncState.SYNCED) }
                    room.copy(syncState = SyncState.SYNCED, pano = syncedPano, comments = syncedComments)
                }
                project.copy(rooms = syncedRooms, syncState = SyncState.SYNCED)
            }

        // Sample Project 2: Empty project for testing
        val sampleProject2 = Project(
            id = "sample-project-2",
            name = "456 Oak Ave - Inspection",
            syncState = SyncState.SYNCED
        )

        _projects.value = listOf(sampleProject1, sampleProject2)
    }
}

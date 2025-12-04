package com.example.interview.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class DeletedPano(
    val panoId: String,
    val roomId: String
)

data class Project(
    val id: String,
    val name: String,
    val rooms: List<Room> = emptyList(),
    val syncState: SyncState = SyncState.SYNCED,
    val deletedPanoIds: List<String> = emptyList(),
    val deletedPanos: List<DeletedPano> = emptyList()
) {
    fun room(id: String): Room? = rooms.find { it.id == id }

    fun copyByAddingRoom(room: Room): Project {
        val newRoom = if (room.syncState == SyncState.SYNCED) room.copy(syncState = SyncState.NEW) else room
        return copy(rooms = rooms + newRoom)
    }

    fun copyByAddingCommentToRoom(roomId: String, comment: Comment): Project {
        val newComment = if (comment.syncState == SyncState.SYNCED) comment.copy(syncState = SyncState.NEW) else comment
        return copy(rooms = rooms.map { room ->
            if (room.id == roomId) room.copy(comments = room.comments + newComment)
            else room
        })
    }

    fun copyBySettingPanoToRoom(roomId: String, pano: Pano): Project {
        val newPano = if (pano.syncState == SyncState.SYNCED) pano.copy(syncState = SyncState.NEW) else pano
        return copy(rooms = rooms.map { room ->
            if (room.id == roomId) room.copy(pano = newPano)
            else room
        })
    }

    fun copyByUpdatingRoomName(roomId: String, name: String): Project {
        return copy(rooms = rooms.map { room ->
            if (room.id == roomId) {
                val newSyncState = if (room.syncState == SyncState.NEW) SyncState.NEW else SyncState.MODIFIED
                room.copy(name = name, syncState = newSyncState)
            } else room
        })
    }

    fun copyByRemovingPanoFromRoom(roomId: String): Project {
        val updatedRooms = rooms.map { r ->
            if (r.id == roomId && r.pano != null) {
                r.copy(pano = r.pano.copy(syncState = SyncState.DELETED))
            } else {
                r
            }
        }
        return copy(rooms = updatedRooms)
    }

    fun copyByUpdatingCommentInRoom(roomId: String, commentId: String, text: String): Project {
        return copy(rooms = rooms.map { room ->
            if (room.id == roomId) {
                room.copy(comments = room.comments.map { comment ->
                    if (comment.id == commentId) {
                        val newSyncState = if (comment.syncState == SyncState.NEW) SyncState.NEW else SyncState.MODIFIED
                        comment.copy(text = text, syncState = newSyncState)
                    } else comment
                })
            } else room
        })
    }

    fun copyByUpdatingName(name: String): Project {
        val newSyncState = if (syncState == SyncState.NEW) SyncState.NEW else SyncState.MODIFIED
        return copy(name = name, syncState = newSyncState)
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun make(name: String, id: String = Uuid.random().toString()): Project {
            return Project(id = id, name = name, syncState = SyncState.NEW)
        }
    }
}

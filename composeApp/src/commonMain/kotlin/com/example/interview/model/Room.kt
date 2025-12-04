package com.example.interview.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Room(
    val id: String,
    val name: String,
    val pano: Pano? = null,
    val comments: List<Comment> = emptyList(),
    val syncState: SyncState = SyncState.SYNCED
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun make(name: String, id: String = Uuid.random().toString()): Room {
            return Room(id = id, name = name, syncState = SyncState.NEW)
        }
    }
}

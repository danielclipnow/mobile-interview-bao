package com.example.interview.model

import kotlinx.datetime.Clock

data class Comment(
    val id: String,
    val text: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val syncState: SyncState = SyncState.SYNCED
)

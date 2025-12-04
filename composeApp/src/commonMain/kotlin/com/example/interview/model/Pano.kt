package com.example.interview.model

data class Pano(
    val id: String,
    val imageData: ByteArray,
    val syncState: SyncState = SyncState.SYNCED
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Pano
        return id == other.id && imageData.contentEquals(other.imageData)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + imageData.contentHashCode()
        return result
    }
}

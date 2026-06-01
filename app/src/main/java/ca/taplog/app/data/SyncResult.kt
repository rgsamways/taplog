package ca.taplog.app.data

sealed class SyncResult {
    object Success : SyncResult()
    data class Conflict(val message: String) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}
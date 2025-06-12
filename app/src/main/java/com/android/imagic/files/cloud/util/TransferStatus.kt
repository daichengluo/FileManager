package com.android.imagic.files.cloud.util

sealed interface TransferStatus {
    data class Transferring(val transferred: Long, val total: Long) : TransferStatus
    data class Failed(val exception: Exception) : TransferStatus
    data class Finished(val data: String? = null) : TransferStatus
}
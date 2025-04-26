package com.android.imagic.files.provider.cloud.sdrive.client

sealed interface TransferStatus {
    data class Transferring(val progress: Int) : TransferStatus
    data class Failed(val exception: Exception) : TransferStatus
    data object Finished : TransferStatus
}

package com.android.imagic.files.filelist

data class FileTransfer(
    val id: Int,
    val title: String?,
    val text: String?,
    val max: Int,
    val progress: Int,
    val indeterminate: Boolean,
    val status: Status = Status.TRANSFERRING
) {

    enum class Status {
        TRANSFERRING, CANCELLING, FAILED, FINISHED
    }
}

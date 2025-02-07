package com.android.imagic.files.provider.archive

import android.os.Parcelable
import com.android.imagic.files.provider.archive.archiver.ReadArchive
import com.android.imagic.files.provider.common.AbstractPosixFileAttributes
import com.android.imagic.files.provider.common.ByteString
import com.android.imagic.files.provider.common.EncryptedFileAttributes
import com.android.imagic.files.provider.common.FileTimeParceler
import com.android.imagic.files.provider.common.PosixFileModeBit
import com.android.imagic.files.provider.common.PosixFileType
import com.android.imagic.files.provider.common.PosixGroup
import com.android.imagic.files.provider.common.PosixUser
import java8.nio.file.Path
import java8.nio.file.attribute.FileTime
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
internal class ArchiveFileAttributes(
    override val lastModifiedTime: @WriteWith<FileTimeParceler> FileTime,
    override val lastAccessTime: @WriteWith<FileTimeParceler> FileTime,
    override val creationTime: @WriteWith<FileTimeParceler> FileTime,
    override val type: PosixFileType,
    override val size: Long,
    override val fileKey: Parcelable,
    override val owner: PosixUser?,
    override val group: PosixGroup?,
    override val mode: Set<PosixFileModeBit>?,
    override val seLinuxContext: ByteString?,
    private val isEncrypted: Boolean,
    private val entryName: String
) : AbstractPosixFileAttributes(), EncryptedFileAttributes {
    override fun isEncrypted(): Boolean = isEncrypted

    fun entryName(): String = entryName

    companion object {
        fun from(archiveFile: Path, entry: ReadArchive.Entry): ArchiveFileAttributes {
            val lastModifiedTime = entry.lastModifiedTime ?: FileTime.fromMillis(0)
            val lastAccessTime = entry.lastAccessTime ?: lastModifiedTime
            val creationTime = entry.creationTime ?: lastModifiedTime
            val type = entry.type
            val size = entry.size
            val fileKey = ArchiveFileKey(archiveFile, entry.name)
            val owner = entry.owner
            val group = entry.group
            val mode = entry.mode
            val seLinuxContext = null
            val isEncrypted = entry.isEncrypted
            val entryName = entry.name
            return ArchiveFileAttributes(
                lastModifiedTime, lastAccessTime, creationTime, type, size, fileKey, owner, group,
                mode, seLinuxContext, isEncrypted, entryName
            )
        }
    }
}

package com.android.imagic.files.cloud.gdrive.sdk

import android.content.Context
import com.android.imagic.files.R
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.util.ApiOperation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java8.nio.file.Path
import kotlinx.coroutines.flow.Flow
import java.io.File

object GoogleDriveServiceClient {

    private val services = mutableMapOf<ServerAuthority, GoogleDriveService>()

    fun getService(
        authority: ServerAuthority,
        account: GoogleSignInAccount,
        context: Context
    ): GoogleDriveService = synchronized(services) {
        var service = services[authority]
        if (service == null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            ).apply { selectedAccount = account.account }

            val drive = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(context.getString(R.string.app_name)).build()
            service = GoogleDriveService(drive)
            services[authority] = service
        }
        return service
    }

    fun getService(authority: ServerAuthority, context: Context): GoogleDriveService? =
        synchronized(services) {
            var service = services[authority]
            if (service == null) {
                GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                    val credential = GoogleAccountCredential.usingOAuth2(
                        context, listOf(DriveScopes.DRIVE_FILE)
                    ).apply { selectedAccount = account.account }

                    val drive = Drive.Builder(
                        NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                    ).setApplicationName(context.getString(R.string.app_name)).build()
                    service = GoogleDriveService(drive)
                    services[authority] = service
                }
            }
            return service
        }

    fun getService(authority: ServerAuthority): GoogleDriveService? =
        synchronized(services) { services[authority] }

    suspend fun getFileList(
        directoryId: String,
        authority: ServerAuthority,
        trashed: Boolean = false
    ): ApiOperation<List<CloudFileItem>> = getService(authority)?.getFileList(directoryId, trashed)
        ?: ApiOperation.Failure(Exception("Authorization has expired"))

    suspend fun createDirectory(
        name: String,
        directoryId: String,
        authority: ServerAuthority
    ): Boolean = getService(authority)?.createDirectory(name, directoryId) ?: false

    suspend fun createFile(
        name: String,
        directoryId: String,
        authority: ServerAuthority
    ): Boolean = getService(authority)?.createEmptyFile(name, directoryId) ?: false

    suspend fun uploadFile(
        file: File,
        directoryId: String,
        authority: ServerAuthority
    ): Boolean = getService(authority)?.uploadFile(file, directoryId) ?: false

    fun uploadFile(
        file: Path,
        directoryId: String,
        authority: ServerAuthority
    ): Flow<TransferStatus>? = getService(authority)?.uploadPathWithProgressFlow(file, directoryId)

    fun downloadFile(
        fileId: String,
        targetPath: Path,
        fileSize: Long,
        authority: ServerAuthority
    ): Flow<TransferStatus>? = getService(authority)?.downloadFile(fileId, targetPath, fileSize)

    fun downloadFileWithResume(
        fileId: String,
        targetPath: Path,
        fileSize: Long,
        authority: ServerAuthority
    ): Flow<TransferStatus>? =
        getService(authority)?.downloadFileWithResume(fileId, targetPath, fileSize)

    suspend fun deleteFile(fileId: String, authority: ServerAuthority): Boolean =
        getService(authority)?.deleteFile(fileId) ?: false

    suspend fun renameFile(fileId: String, newName: String, authority: ServerAuthority): Boolean =
        getService(authority)?.renameFile(fileId, newName) ?: false

    suspend fun copyFile(
        fileId: String,
        targetDirectoryId: String,
        isDirectory: Boolean,
        authority: ServerAuthority
    ): Boolean {
        val service = getService(authority) ?: return false
        return if (isDirectory) {
            service.copyFileRecursively(fileId, targetDirectoryId)
        } else {
            service.copyFile(fileId, targetDirectoryId)
        }
    }

    suspend fun moveFile(
        fileId: String,
        targetDirectoryId: String,
        authority: ServerAuthority
    ): Boolean = getService(authority)?.moveFile(fileId, targetDirectoryId) ?: false

    suspend fun getSpaceData(authority: ServerAuthority): SpaceData? =
        getService(authority)?.getSpaceData()
}
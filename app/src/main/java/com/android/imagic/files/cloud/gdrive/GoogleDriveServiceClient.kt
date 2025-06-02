package com.android.imagic.files.cloud.gdrive

import android.util.Log
import com.android.imagic.files.app.networkJson
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class GoogleDriveServiceClient(val client: OkHttpClient) {

    private val json: Json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/drive/v3/")
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(GoogleDriveService::class.java)

    suspend fun getRootDirectoryFiles() {
        val query = "'me' in owners and trashed=false"
        val files = network.listFiles(query).files
        Log.d("chengluo", "getRootDirectoryFiles: $files")
    }

    suspend fun uploadFile() {
        val query = "'me' in owners and trashed=false"
        val files = network.listFiles(query).files
        Log.d("chengluo", "getRootDirectoryFiles: $files")
    }

    suspend fun createFolder(folderName: String) {
        val folderBody = CreateFolderRequest(
            name = folderName,
            mimeType = "application/vnd.google-apps.folder",
            parents = listOf("root")
        )
        val folder = network.createFolder(folderBody)
        Log.d("chengluo", "createFolder: $folder")
    }
}
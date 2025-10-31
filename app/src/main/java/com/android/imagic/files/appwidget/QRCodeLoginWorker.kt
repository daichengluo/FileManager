package com.android.imagic.files.appwidget

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.cloud.gdrive.api.GoogleDriveClient
import com.android.imagic.files.cloud.onedrive.OneDriveClient
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.sdrive.client.StarBoardDriveClient.OkHttpAuthenticatorInterceptor
import com.android.imagic.files.storage.GoogleDriveServer
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.Constants
import com.android.imagic.files.util.NetworkUtil
import com.android.imagic.files.util.QrcodeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileOutputStream
import java.time.Clock

class QRCodeLoginWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var state: String? = null
    private var loginStatus: QrcodeLoginStatus = QrcodeLoginStatus.UNSTARTED
    private var accessTokenData: AccessTokenData? = null
    private var getStateJob: Job? = null
    private var getAccessTokenJob: Job? = null

    override suspend fun doWork(): Result {
        Log.d("chengluo", "QRCodeLoginWorker doWork: ")
        generateQrcode()
        qrcodeExpired()
        return Result.success()
    }

    fun qrcodeExpired() = scope.launch {
        delay(1000 * 60 * 9)
        getStateJob?.cancel()
        getStateJob = null
        LoginWidgetUpdater.needRefresh = true
        LoginWidgetUpdater.updateWidgets(applicationContext)
    }

    suspend fun generateQrcode() = withContext(Dispatchers.IO) {
        val deviceId = NetworkUtil.getMacAddress() ?: getAndroidId(applicationContext)
        state = generateRandomString()
        val timestamp = Clock.systemUTC().instant().epochSecond.toString()
        val url =
            "https://wboard.starboard-online.com/Long.html?Mac=$deviceId&state=$state&timeStamp=$timestamp"
        Log.d("chengluo", "QRCodeLoginWorker generateQrcode url: $url")
        LoginWidgetUpdater.bitmap = QrcodeUtil.generateQRCodeBitmap(url, 176)
        LoginWidgetUpdater.updateWidgets(applicationContext)
        getState()
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context) = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    fun getState() {
        Log.d("chengluo", "QRCodeLoginWorker getState: ")
        if (getStateJob?.isActive == true) return
        if (state == null) return
        getStateJob = scope.launch {
            while (isActive && LoginWidgetUpdater.bitmap != null) {
                AuthClientImpl.getState(state!!)
                    .onSuccess {
                        Log.d("chengluo", "getState Success: $it")
                        loginStatus = it
                        if (QrcodeLoginStatus.LOGGING == it) {
                            getStateJob?.cancel()
                            getStateJob = null
                            getAccessToken()
                        }
                    }
                    .onFailure {
                        Log.d("chengluo", "getState Failure: ${it.message}")
                    }
                delay(1000L)
            }
        }
    }

    fun getAccessToken() {
        Log.d("chengluo", "QRCodeLoginWorker getAccessToken: ")
        if (getAccessTokenJob?.isActive == true) return
        if (state == null) return
        LoginWidgetUpdater.needRefresh = true
        LoginWidgetUpdater.updateWidgets(applicationContext)
        getAccessTokenJob = scope.launch {
            while (isActive && loginStatus == QrcodeLoginStatus.LOGGING) {
                AuthClientImpl.getAccessToken(state!!)
                    .onSuccess {
                        Log.d("chengluo", "getAccessToken Success: $it")
                        accessTokenData = it
                        if (it != null) {
                            getAccessTokenJob?.cancel()
                            getAccessTokenJob = null
                            updateUserInfo(it)
                        }
                    }
                    .onFailure {
                        Log.d("chengluo", "getAccessToken Failure: ${it.message}")
                        getAccessTokenJob?.cancel()
                        getAccessTokenJob = null
                    }
                delay(1000L)
            }
        }
    }

    fun updateUserInfo(data: AccessTokenData) = scope.launch {
        Log.d("chengluo", "QRCodeLoginWorker updateUserInfo: ")
        val headerImageFile = getUserHeaderImage(data.access_token)

        val authority = data.toAuthority()
        val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
        val okHttpClient = okHttpClient.newBuilder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .addNetworkInterceptor(authenticatorInterceptor)
            .authenticator(authenticatorInterceptor)
            .build()
        val client = CloudClient(okHttpClient)

        val headerImagerUrl = if (headerImageFile != null) {
            when (val result = client.uploadFileService(headerImageFile)) {
                is ApiOperation.Success -> result.data
                is ApiOperation.Failure -> null
            }
        } else null
        Log.d("chengluo", "QRCodeLoginWorker updateUserInfo headerImagerUrl: $headerImagerUrl")

        client.getUserInfo(data.toAuthInfoBody(headerImagerUrl)).onSuccess {
            Log.d("chengluo", "QRCodeLoginWorker updateUserInfo Success: $it")
            val newAuthority = getSpaceUsageInfo(it.toAuthority())
            Storages.addOrReplace(StarBoardDriveServer(null, null, newAuthority))

            val authority = data.toServerAuthority()
            if (it.isGoogle) {
                Log.d("chengluo", "QRCodeLoginWorker getUserInfo isGoogle")
                val response = GoogleDriveClient.getSpaceData(authority)
                val updateAuthority = if (response is ApiOperation.Success) {
                    authority.copy(
                        totalSpace = response.data.total,
                        usedSpace = response.data.used
                    )
                } else authority
                val googleDriveServer = GoogleDriveServer(
                    null,
                    null,
                    updateAuthority
                )
                Storages.addOrReplace(googleDriveServer)
            }
            if (it.isMicrosoft) {
                Log.d("chengluo", "QRCodeLoginWorker getUserInfo isGoogle")
                val response = OneDriveClient.getSpaceData(authority)
                val updateAuthority = if (response is ApiOperation.Success) {
                    authority.copy(
                        totalSpace = response.data.total,
                        usedSpace = response.data.used
                    )
                } else authority
                val oneDriveServer = OneDriveServer(
                    null,
                    null,
                    updateAuthority
                )
                Storages.addOrReplace(oneDriveServer)
            }
            LoginWidgetUpdater.needRefresh = false
            LoginWidgetUpdater.updateWidgets(applicationContext)
        }
    }

    private suspend fun getSpaceUsageInfo(authority: Authority): Authority {
        Log.d("chengluo", "QRCodeLoginWorker getSpaceUsageInfo: ")
        val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
        val okHttpClient = okHttpClient.newBuilder()
            .addNetworkInterceptor(authenticatorInterceptor)
            .authenticator(authenticatorInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
        val client = CloudClient(okHttpClient)
        return when (val response = client.getSpaceUsageInfo()) {
            is ApiOperation.Success -> {
                val data = response.data
                Log.d("chengluo", "QRCodeLoginWorker getSpaceUsageInfo Success: $data")
                authority.copy(
                    freeSpace = data.remainingSpace,
                    totalSpace = data.totalSpace
                )
            }

            is ApiOperation.Failure -> authority
        }
    }

    private suspend fun getUserHeaderImage(accessToken: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(Constants.MS_GRAPH_PHOTO)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext null
                }

                val bytes = response.body?.bytes()
                response.close()

                if (bytes != null) {
                    val file = File.createTempFile("userHeaderImage", ".jpg")
                    FileOutputStream(file).use { it.write(bytes) }
                    return@withContext file
                }
            } catch (e: Exception) {
                Log.e("chengluo", "QRCodeLoginWorker getUserHeaderImage: ", e)
            }
            return@withContext null
        }

    private fun generateRandomString(length: Int = 12): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
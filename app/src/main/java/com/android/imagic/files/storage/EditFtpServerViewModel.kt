
package com.android.imagic.files.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.provider.common.newDirectoryStream
import com.android.imagic.files.util.ActionState
import com.android.imagic.files.util.isFinished
import com.android.imagic.files.util.isReady
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.nio.charset.Charset

class EditFtpServerViewModel : ViewModel() {
    val charsets = Charset.availableCharsets().values.toList()

    private val _connectState = MutableStateFlow<ActionState<FtpServer, Unit>>(ActionState.Ready())
    val connectState = _connectState.asStateFlow()

    fun connect(server: FtpServer) {
        viewModelScope.launch {
            check(_connectState.value.isReady)
            _connectState.value = ActionState.Running(server)
            _connectState.value = try {
                runInterruptible(Dispatchers.IO) {
                    FtpServerAuthenticator.addTransientServer(server)
                    try {
                        val path = server.path
                        path.fileSystem.use {
                            path.newDirectoryStream().toList()
                        }
                    } finally {
                        FtpServerAuthenticator.removeTransientServer(server)
                    }
                }
                ActionState.Success(server, Unit)
            } catch (e: Exception) {
                ActionState.Error(server, e)
            }
        }
    }

    fun finishConnecting() {
        viewModelScope.launch {
            check(_connectState.value.isFinished)
            _connectState.value = ActionState.Ready()
        }
    }
}

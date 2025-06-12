package com.android.imagic.files.cloud.util

import android.util.Log
import kotlinx.coroutines.Job

object TransferJobManager {
    private val jobMap = mutableMapOf<Int, Job>()
    private val lock = Any()

    fun register(uploadId: Int, job: Job) {
        Log.d("chengluo", "TransferJobManager register: $uploadId")
        synchronized(lock) {
            jobMap[uploadId]?.cancel() // 如果已有同 id 上传，先取消
            jobMap[uploadId] = job
        }
    }

    fun cancel(uploadId: Int) {
        Log.d("chengluo", "TransferJobManager cancel: $uploadId")
        synchronized(lock) {
            jobMap[uploadId]?.cancel()
            jobMap.remove(uploadId)
        }
    }

    fun cancelAll() {
        synchronized(lock) {
            jobMap.values.forEach { it.cancel() }
            jobMap.clear()
        }
    }

    fun remove(uploadId: Int) {
        synchronized(lock) {
            jobMap.remove(uploadId)
        }
    }

    fun isActive(uploadId: Int): Boolean {
        return synchronized(lock) {
            jobMap[uploadId]?.isActive == true
        }
    }
}
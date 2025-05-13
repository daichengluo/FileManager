package com.android.imagic.files.util

import java.io.File
import java.io.IOException
import java.util.Locale

object NetworkUtil {

    fun getEthMacAddress(): String? {
        return try {
            val mac = File("/sys/class/net/eth0/address").readText().trim()
            mac.uppercase(Locale.ENGLISH).takeIf { it.length >= 17 }?.substring(0, 17)
        } catch (_: IOException) {
            null
        }
    }

    fun getMacAddress(): String? {
        val interfaces = listOf("eth0", "wlan0")
        for (iface in interfaces) {
            val mac = readMacFromInterface(iface)
            if (!mac.isNullOrBlank()) return mac
        }
        return null
    }

    private fun readMacFromInterface(interfaceName: String): String? {
        return try {
            val file = File("/sys/class/net/$interfaceName/address")
            if (file.exists()) {
                val mac = file.readText().trim()
                mac.uppercase(Locale.ENGLISH).takeIf { it.length >= 17 }?.substring(0, 17)
            } else {
                null
            }
        } catch (_: IOException) {
            null
        }
    }
}
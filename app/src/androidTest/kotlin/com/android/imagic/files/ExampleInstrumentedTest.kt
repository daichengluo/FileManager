package com.android.imagic.files

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val proSha1 = "27:19:6E:38:6B:87:5E:76:AD:F7:00:E7:EA:84:E4:C6:EE:E3:3D:FA"
        val edlaSha1 = "CB:06:1D:3A:2D:46:EC:4F:83:29:5D:DC:B3:D3:B6:BA:0B:3E:75:B9"
        sha1ToBase64("proSha1", proSha1)
        sha1ToBase64("edlaSha1", edlaSha1)
    }

    fun sha1ToBase64(flavorName: String, sha1: String) {
        val cleanedHexString = sha1.replace(":", "")
        val len = cleanedHexString.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] =
                ((cleanedHexString[i].digitToInt(16) shl 4) + cleanedHexString[i + 1].digitToInt(16)).toByte()
        }
        val base64 = Base64.encodeToString(data, Base64.NO_WRAP)
        println("$flavorName base64: $base64")
    }
}
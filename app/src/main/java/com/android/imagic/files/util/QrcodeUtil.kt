package com.android.imagic.files.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.Hashtable

object QrcodeUtil {

    fun generateQRCodeBitmap(content: String, size: Int = 320): Bitmap {
        val hints = Hashtable<EncodeHintType, String>().apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val rawMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 1, 1, hints)
        val croppedMatrix = removeWhiteBorder(rawMatrix)

        val croppedWidth = croppedMatrix.width
        val croppedHeight = croppedMatrix.height

        // 缩放比率
        val scaleX = size.toFloat() / croppedWidth
        val scaleY = size.toFloat() / croppedHeight

        // 最终生成 Bitmap
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                // 找到原图中的位置
                val origX = (x / scaleX).toInt()
                val origY = (y / scaleY).toInt()
                if (origX in 0 until croppedWidth && origY in 0 until croppedHeight) {
                    if (croppedMatrix[origX, origY]) {
                        pixels[y * size + x] = 0xFF000000.toInt() // 黑色像素
                    }
                }
            }
        }

        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    // 移除白边函数
    private fun removeWhiteBorder(matrix: BitMatrix): BitMatrix {
        var left = matrix.width
        var top = matrix.height
        var right = 0
        var bottom = 0

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        val width = right - left + 1
        val height = bottom - top + 1
        val croppedMatrix = BitMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matrix[x + left, y + top]) {
                    croppedMatrix.set(x, y)
                }
            }
        }
        return croppedMatrix
    }
}
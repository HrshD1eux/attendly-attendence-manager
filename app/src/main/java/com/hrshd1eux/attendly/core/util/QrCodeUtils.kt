package com.hrshd1eux.attendly.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object QrCodeUtils {

    private const val PREFIX = "ATTENDLY:v1:"

    /**
     * Compresses plain timetable text using GZIP + Base64 to ensure
     * low data density and rapid scanning even on budget cameras.
     */
    fun compressTimetable(plainText: String): String {
        return try {
            val byteOut = ByteArrayOutputStream()
            GZIPOutputStream(byteOut).use { gzip ->
                gzip.write(plainText.toByteArray(StandardCharsets.UTF_8))
            }
            val base64 = Base64.getEncoder().encodeToString(byteOut.toByteArray())
            "$PREFIX$base64"
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Decompresses timetable payload if prefixed with ATTENDLY:v1:,
     * or returns plainText as-is if already uncompressed text.
     */
    fun decompressTimetable(input: String): String {
        val trimmed = input.trim()
        if (!trimmed.startsWith(PREFIX)) {
            return trimmed
        }
        return try {
            val base64 = trimmed.substring(PREFIX.length)
            val bytes = Base64.getDecoder().decode(base64)
            val byteIn = ByteArrayInputStream(bytes)
            GZIPInputStream(byteIn).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            trimmed
        }
    }

    /**
     * Generates a high-contrast black-and-white QR code Bitmap.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Decodes a QR code from a provided Bitmap (e.g. chosen from Gallery or captured).
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            decompressTimetable(result.text)
        } catch (e: Exception) {
            null
        }
    }
}

package soko.ekibun.qqnotifyplus.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object FileUtils {
    private fun md5(string: String): String {
        if (TextUtils.isEmpty(string)) {
            return ""
        }
        try {
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = md5!!.digest(string.toByteArray())
            var result = ""
            for (b in bytes) {
                var temp = Integer.toHexString(b.toInt() and 0xff)
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result += temp
            }
            return result
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return string
    }

    fun saveBitmapToCache(context: Context, bmp: Bitmap, fileName: String, uniqueName: String, delete: Boolean): File? {
        var file: File? = null
        try {
            val path = getDiskCacheDir(context, uniqueName).absolutePath
            val fileFolder = File(path)
            if (fileFolder.exists() && delete)
                fileFolder.delete()
            if (!fileFolder.exists())
                fileFolder.mkdirs()

            file = File(path, md5(fileName))
            if (!file.exists())
                file.createNewFile()
            val outStream = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file
    }

    fun getBitmapFromCache(context: Context, fileName: String, uniqueName: String): Bitmap? {
        try {
            val file = File(getDiskCacheDir(context, uniqueName).absolutePath, md5(fileName))
            if (file.exists())
                return BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun getDiskCacheDir(context: Context, uniqueName: String): File {
        val cachePath: String = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            context.externalCacheDir!!.path
        } else {
            context.cacheDir.path
        }

        return File(cachePath + File.separator + uniqueName)
    }
}
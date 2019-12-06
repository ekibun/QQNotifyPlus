@file:Suppress("DEPRECATION")

package soko.ekibun.qqnotifyplus.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.content.Intent
import androidx.annotation.RequiresApi

object NotificationUtil{
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(channelId: String, title: String): NotificationChannel{
        val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH)
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        channel.enableVibration(true)
        return channel
    }

    /**
     * 向小米手机发送未读消息数广播
     * @param count
     */
    fun sendToXiaoMi(notification: Notification, count: Int) {
        try {
            val field = notification.javaClass.getDeclaredField("extraNotification")
            val extraNotification = field.get(notification)
            val method = extraNotification.javaClass.getDeclaredMethod("setMessageCount", Int::class.javaPrimitiveType)
            method.invoke(extraNotification, count)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
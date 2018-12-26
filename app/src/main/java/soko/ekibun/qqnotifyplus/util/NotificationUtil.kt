@file:Suppress("DEPRECATION")

package soko.ekibun.qqnotifyplus.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.app.NotificationCompat

object NotificationUtil{
    //创建渠道并发布通知
    fun builder(context: Context, channelId: String, title: String): NotificationCompat.Builder{
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >=26) {
            val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(context, channelId)
    }

    fun getLargeIcon(context: Context, notification: Notification): Bitmap?{
        return if(Build.VERSION.SDK_INT >=23)
            (notification.getLargeIcon()?.loadDrawable(context) as? BitmapDrawable)?.bitmap
        else
            notification.extras.get(Notification.EXTRA_LARGE_ICON) as? Bitmap
    }
}
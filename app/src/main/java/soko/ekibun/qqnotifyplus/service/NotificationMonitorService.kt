package soko.ekibun.qqnotifyplus.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.MessagingStyle.Message
import android.support.v4.app.Person
import android.support.v4.graphics.drawable.IconCompat
import android.util.Log
import soko.ekibun.qqnotifyplus.R
import soko.ekibun.qqnotifyplus.util.FileUtils
import soko.ekibun.qqnotifyplus.util.NotificationUtil
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.content.res.ResourcesCompat

class NotificationMonitorService : NotificationListenerService() {
    enum class Tag{
        QQ,
        TIM,
        QQ_LITE,
        QZONE_QQ,
        QZONE_TIM,
        QZONE_LITE
    }
    companion object {
        val tags = mapOf(
                "com.tencent.mobileqq" to Tag.QQ,
                "com.tencent.tim" to Tag.TIM,
                "com.tencent.qqlite" to Tag.QQ_LITE)
        val qzoneTag = mapOf(
                Tag.QQ to Tag.QZONE_QQ,
                Tag.TIM to Tag.QZONE_TIM,
                Tag.QQ_LITE to Tag.QZONE_LITE)
        fun isQzoneTag(tag: Tag): Boolean{
            return qzoneTag.containsValue(tag)
        }
    }

    private val notificationManager: NotificationManager by lazy{ getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val msgList = HashMap<Tag, HashMap<String, Notifies>>()
    private val maxCount = 20
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        var tag = NotificationMonitorService.tags[sbn.packageName]?:return

        val notification = sbn.notification?:return
        val notifyTitle = notification.extras.getString(Notification.EXTRA_TITLE)?:""
        val notifyText = notification.extras.getString(Notification.EXTRA_TEXT)?.replace("\n", " ")?:""
        val notifyTicker = notification.tickerText?.toString()?.replace("\n", " ")?:""
        Log.v("title", notifyTitle)
        Log.v("text", notifyText)
        Log.v("ticker", notifyTicker)

        val mul = !notifyText.contains(":") && !notifyTicker.endsWith(notifyText)
        val title = if (mul) notifyText else notifyTitle
        if(notifyTicker.isEmpty() || title == notifyTicker) return

        //单独处理QQ空间
        val count = if (notifyTicker == notifyText) {
            val count = Regex("QQ空间动态\\(共(\\d+)条未读\\)$").find(title)?.groupValues?.get(1)?.toIntOrNull()?:0
            if(count > 0 || "QQ空间动态" == title)
                tag = qzoneTag[tag]?:tag
            Math.max(1, count)
        }else
            Regex("(\\d+)\\S{1,3}新消息\\)?$").find(title)?.groupValues?.get(1)?.toIntOrNull()?:1

        val notify = if(isQzoneTag(tag))
            Notify("QQ空间动态",
                    notifyTicker)
        else
            Regex("(.*?)\\((.+?)\\):(.+)").find(notifyTicker)?.groupValues?.let{
                Notify(it.getOrNull(1)?:"",
                        it.getOrNull(3)?:"",
                        it.getOrNull(2)?:"")
            }?:Regex("([^:]+): (.+)").find(notifyTicker)?.groupValues?.let{
                Notify(it.getOrNull(1)?:"",
                        it.getOrNull(2)?:"")
            }?:return
        val key = if(isQzoneTag(tag)) "qzone" else if(notify.group.isEmpty()) "u_" + notify.name else "g_" + notify.group
        val tagMsgList = msgList.getOrPut(tag) { HashMap()}

        //删除旧消息
        while(true) {
            var sum = 0
            tagMsgList.forEach { sum += it.value.size }
            Log.v("sum", "$sum, $count")
            if (sum < count) break
            var oldestTime: Pair<String, Notifies>? = null
            tagMsgList.forEach {
                if (oldestTime == null || (it.value.size > 0 && oldestTime?.second?.size?:0 > 0 && oldestTime?.second?.get(0)?.timestamp ?: 0 > it.value[0].timestamp))
                    oldestTime = Pair(it.key, it.value)
            }
            (oldestTime?: break).second.removeAt(0)
            if(oldestTime?.second?.size?:break == 0) {
                tagMsgList.remove(oldestTime?.first ?: break)
                notificationManager.cancel(oldestTime?.first ?: break, tag.ordinal)
            }
        }

        val notifies = tagMsgList.getOrPut(key){ Notifies()}

        val icon = NotificationUtil.getLargeIcon(this, notification)
        if(!mul && icon != null) {
            FileUtils.saveBitmapToCache(this, icon, key, "profile", false)
            notifies.profile = icon
        }
        val profile = notifies.profile?:FileUtils.getBitmapFromCache(this, key, "profile")?:icon

        val person = Person.Builder().setName(notify.name)
                .setIcon(if(notify.group.isEmpty()) IconCompat.createWithBitmap(profile) else null).build()

        val time = System.currentTimeMillis()
        notifies.add(Message(notify.content, time, person))
        while(notifies.size > maxCount) notifies.removeAt(0)

        val style = NotificationCompat.MessagingStyle(person)
        style.conversationTitle = notify.group
        style.isGroupConversation = !notify.group.isEmpty()
        notifies.forEach { style.addMessage(it) }

        Log.v("ord", tag.name + tag.ordinal)
        lastIntent[tag.ordinal] = notification.contentIntent
        val notificationIntent = Intent(this, NotificationMonitorService::class.java)
        notificationIntent.putExtra("NotifyClick", true)
        notificationIntent.putExtra("tag", tag.ordinal)
        val pendingIntent = PendingIntent.getService(this.applicationContext, tag.ordinal, notificationIntent, 0)

        val builder = NotificationUtil.builder(this,
                if(isQzoneTag(tag)) "qzone" else if(notify.group.isEmpty()) "friend" else "group",
                if(isQzoneTag(tag)) "QQ空间消息" else if(notify.group.isEmpty()) "私聊消息" else "群组消息")
                .setLargeIcon(profile)
                .setStyle(style)
                .setColor(ResourcesCompat.getColor(resources, if(isQzoneTag(tag)) R.color.colorQzone else R.color.colorPrimary, theme))
                .setSubText("${notifies.size}条${if(isQzoneTag(tag))"未读" else "新消息"}")
                .setContentTitle(notify.group)
                .setSmallIcon(if(isQzoneTag(tag)) R.drawable.ic_qzone else if(tag == Tag.TIM) R.drawable.ic_tim else  R.drawable.ic_qq)
                .setTicker(notifyTicker)
                .setWhen(time)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(tag.name)
        notificationManager.notify(key, tag.ordinal, builder.build())
        if(!isQzoneTag(tag)){
            builder.setGroupSummary(true)
            builder.setSubText("${count}条新消息")
            notificationManager.notify(tag.name, tag.ordinal, builder.build())
        }



        if(Build.VERSION.SDK_INT >= 23)
            setNotificationsShown(arrayOf(sbn.key))
        cancelNotification(sbn.key)
    }

    private val lastIntent: HashMap<Int, PendingIntent> = HashMap()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.hasExtra("tag")) {
                val tag = intent.getIntExtra("tag", 0)
                notificationManager.cancel(tag)
                val sbns = activeNotifications
                if (sbns != null && sbns.isNotEmpty()) {
                    for (sbn in sbns) {
                        if (packageName != sbn.packageName || sbn.id != tag)
                            continue
                        notificationManager.cancel(sbn.tag, sbn.id)
                    }
                }
                if (intent.hasExtra("NotifyClick")) {
                    try {
                        lastIntent[tag]?.send()
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    class Notifies: ArrayList<Message>(){
        var profile: Bitmap? = null
    }

    data class Notify(
            val name: String,
            val content: String,
            val group: String = ""
    )
}

package soko.ekibun.qqnotifyplus.service

import android.app.Notification
import android.graphics.Bitmap
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
import android.preference.PreferenceManager
import android.support.v4.content.res.ResourcesCompat
import eu.chainfire.librootjava.RootIPCReceiver
import soko.ekibun.qqnotifyplus.root.IIPC
import eu.chainfire.libsuperuser.Shell
import soko.ekibun.qqnotifyplus.root.RootMain

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
        const val EXTRA_NOTIFICATION_MODIFIED = "QQNotificationModified"

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

    override fun onCreate() {
        super.onCreate()
        ipcReceiver.setContext(this)
    }

    override fun onDestroy() {
        ipcReceiver.release()
        super.onDestroy()
    }

    private val ipcReceiver by lazy{
        object: RootIPCReceiver<IIPC>(this, 0){
            override fun onConnect(ipc: IIPC?) {
                while(statusBarNotifications.isNotEmpty()){
                    val sbn = statusBarNotifications.removeAt(0)
                    modifyNotification(sbn, ipc)
                }
                disconnect()
            }
            override fun onDisconnect(ipc: IIPC?) {}
        }
    }


    private val msgList = HashMap<Tag, HashMap<String, Notifies>>()
    private var statusBarNotifications = ArrayList<StatusBarNotification>()
    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    fun modifyNotification(sbn: StatusBarNotification, ipc: IIPC?){
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

        val intent = ipc?.getIntent(notification.contentIntent)
        val uinname = intent?.extras?.getString("uinname")

        if(intent?.extras?.getString("uin", "")?.toIntOrNull() != null)
            sp.edit().putString("${tag.name}_$uinname", intent.toUri(0)?.toString()).apply()

        val notify = if(isQzoneTag(tag))
            Notify("QQ空间动态",
                    notifyTicker)
        else Regex("(.*?)\\((${if(uinname.isNullOrEmpty()) ".+?" else uinname})\\):(.+)").find(notifyTicker)?.groupValues?.let{
            Notify(it.getOrNull(1)?:"",
                    it.getOrNull(3)?:"",
                    it.getOrNull(2)?:"")
        }?:Regex("(${if(uinname.isNullOrEmpty()) "[^:]+" else uinname}): (.+)").find(notifyTicker)?.groupValues?.let{
            Notify(it.getOrNull(1)?:"",
                    it.getOrNull(2)?:"")
        }?:return
        val key = if(isQzoneTag(tag)) "qzone" else  "${tag.name}_" + if(notify.group.isEmpty()) notify.name else notify.group
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

        val style = NotificationCompat.MessagingStyle(person)
        style.conversationTitle = notify.group
        style.isGroupConversation = !notify.group.isEmpty()
        notifies.forEach { style.addMessage(it) }

        val pendingIntent = try{
            val it = Intent.parseUri(sp.getString(key, "")?:"",0)
            val uin = it.extras?.getString("uin", "")?.toIntOrNull()?:throw Exception("no uin")
            PendingIntent.getActivity(this.applicationContext, uin, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }catch (e: Exception){
            notification.contentIntent }

        val channelId =  "${tag.name}+" + if(isQzoneTag(tag)) "qzone" else if(notify.group.isEmpty()) "friend" else "group"
        val channelName = if(isQzoneTag(tag)) "QQ空间消息" else if(notify.group.isEmpty()) "私聊消息" else "群组消息"
        val channelGroupTag = NotificationMonitorService.tags[sbn.packageName]?:tag

        val builder = NotificationCompat.Builder(this, channelId)
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
        val newNotification = builder.build()
        newNotification.extras.putBoolean(EXTRA_NOTIFICATION_MODIFIED, true)
        NotificationUtil.sendToXiaoMi(newNotification, notifies.size)
        if(ipc == null){
            val manager = NotificationUtil.getNotificationManager(this)
            NotificationUtil.registerChannel(manager, channelId, channelName, channelGroupTag.name, channelGroupTag.name)
            manager.notify(key, tag.ordinal, newNotification)
        } else ipc.sendNotification(sbn.packageName, key, tag.ordinal, newNotification, channelId, channelName)

        cancelNotification(sbn.key)
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if(sbn.notification.extras.containsKey(EXTRA_NOTIFICATION_MODIFIED) || !NotificationMonitorService.tags.containsKey(sbn.packageName)) return

        statusBarNotifications.add(sbn) //cache

        //Root
        if(root) {
            shell.addCommand(RootMain.getLaunchScript(this), 0) { commandCode, exitCode, output ->
                Log.v("su_intent", "$commandCode, $exitCode, $output")
            }
        }else modifyNotification(sbn, null)
    }
    private var root = false
    private val shell= Shell.Builder().useSU().open { commandCode, exitCode, output ->
        root = exitCode == 0
        Log.v("su", "$commandCode, $exitCode, $output")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.hasExtra("tag")) {
                val tag = intent.getIntExtra("tag", 0)
                val pkg = tags.toList().firstOrNull { it.second.ordinal == tag || qzoneTag[it.second]?.ordinal == tag }?.first?:""
                val sbns = activeNotifications
                if (sbns != null && sbns.isNotEmpty()) {
                    for (sbn in sbns) {
                        if (pkg != sbn.packageName || sbn.id != tag) continue
                        cancelNotification(sbn.key)
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

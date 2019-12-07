package soko.ekibun.qqnotifyplus.service

import android.app.Notification
import android.graphics.drawable.Icon
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.oasisfeng.nevo.sdk.MutableStatusBarNotification
import com.oasisfeng.nevo.sdk.NevoDecoratorService
import soko.ekibun.qqnotifyplus.R
import soko.ekibun.qqnotifyplus.util.MessagingBuilder
import soko.ekibun.qqnotifyplus.util.NotificationUtil
import kotlin.math.max


class NevoDecorator : NevoDecoratorService() {
    enum class Tag{
        QQ,
        TIM,
        QQ_LITE,
        QZONE_QQ,
        QZONE_TIM,
        QZONE_LITE
    }
    class Notifies: ArrayList<NotificationCompat.MessagingStyle.Message>(){
        var profile: Icon? = null
    }

    data class Notify(
            val name: String,
            val content: String,
            val group: String = ""
    )

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
    private val msgList = HashMap<Tag, HashMap<String, Notifies>>()
    override fun apply(evolving: MutableStatusBarNotification): Boolean {
        var tag = tags[evolving.packageName]?: return false
        val notification = evolving.notification
        val notifyTitle = notification.extras.getString(Notification.EXTRA_TITLE)?:""
        val notifyText = notification.extras.getString(Notification.EXTRA_TEXT)?.replace("\n", " ")?:""
        val notifyTicker = notification.tickerText?.toString()?.replace("\n", " ")?:""
        Log.v("title", notifyTitle)
        Log.v("text", notifyText)
        Log.v("ticker", notifyTicker)

        val mul = !notifyText.contains(":") && !notifyTicker.endsWith(notifyText)
        val title = if (mul) notifyText else notifyTitle
        if(notifyTicker.isEmpty() || title == notifyTicker) return false

        //单独处理QQ空间
        val count = if (notifyTicker == notifyText) {
            val count = Regex("QQ空间动态\\(共(\\d+)条未读\\)$").find(title)?.groupValues?.get(1)?.toIntOrNull()?:0
            if(count > 0 || "QQ空间动态" == title)
                tag = qzoneTag[tag]?:tag
            max(1, count)
        }else
            Regex("(\\d+)\\S{1,3}新消息\\)?$").find(title)?.groupValues?.get(1)?.toIntOrNull()?:1

        val notify = if(isQzoneTag(tag))
            Notify("QQ空间动态",
                    notifyTicker)
        else Regex("(.*?)\\((.+?)\\):(.+)").find(notifyTicker)?.groupValues?.let{
            Notify(it.getOrNull(1)?:"",
                    it.getOrNull(3)?:"",
                    it.getOrNull(2)?:"")
        }?:Regex("([^:]+): (.+)").find(notifyTicker)?.groupValues?.let{
            Notify(it.getOrNull(1)?:"",
                    it.getOrNull(2)?:"")
        }?: throw Exception("not matched Parser")
        val key = "${tag.name}_" + if(isQzoneTag(tag)) "qzone" else if(notify.group.isEmpty()) notify.name else notify.group
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
        val icon = notification.getLargeIcon()
        notifies.profile = icon ?: notifies.profile
        val person = Person.Builder().setName(notify.name)
                .setIcon(if(notify.group.isEmpty() && notifies.profile != null) IconCompat.createFromIcon(notifies.profile!!) else null).build()
        val time = System.currentTimeMillis()
        notifies.add(NotificationCompat.MessagingStyle.Message(notify.content, time, person))

        val style = NotificationCompat.MessagingStyle(person)
        style.conversationTitle = notify.group
        style.isGroupConversation = notify.group.isNotEmpty()
        notifies.forEach { style.addMessage(it) }

        MessagingBuilder.flatIntoExtras(style, notification.extras)
        notification.extras.putString(Notification.EXTRA_TEMPLATE, TEMPLATE_MESSAGING)
        notification.extras.putBoolean(Notification.EXTRA_SHOW_WHEN, true)
        notification.extras.putString(Notification.EXTRA_SUB_TEXT, "${notifies.size}条${if(isQzoneTag(tag))"未读" else "新消息"}")

        notification.`when` = time
        notification.smallIcon = Icon.createWithResource(this, if(isQzoneTag(tag)) R.drawable.ic_qzone else if(tag == Tag.TIM) R.drawable.ic_tim else  R.drawable.ic_qq)
        notification.color = getColor(if(isQzoneTag(tag)) R.color.colorQzone else R.color.colorPrimary)

        val channelId =  "${tag.name}+" + if(isQzoneTag(tag)) "qzone" else if(notify.group.isEmpty()) "friend" else "group"
        val channelName = if(isQzoneTag(tag)) "QQ空间消息" else if(notify.group.isEmpty()) "私聊消息" else "群组消息"
        if (SDK_INT >= O) {
            createNotificationChannels(evolving.packageName, Process.myUserHandle(), listOf(
                    NotificationUtil.createChannel(channelId, channelName)))
            notification.channelId = channelId
        }
        return true
    }
}
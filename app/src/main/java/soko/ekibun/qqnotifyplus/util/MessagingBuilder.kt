package soko.ekibun.qqnotifyplus.util

import android.app.Notification
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat


object MessagingBuilder {
    private const val KEY_TEXT = "text"
    private const val KEY_TIMESTAMP = "time"
    private const val KEY_SENDER = "sender"
    private const val KEY_SENDER_PERSON = "sender_person"
    private const val KEY_DATA_MIME_TYPE = "type"
    private const val KEY_DATA_URI = "uri"
    private const val KEY_EXTRAS_BUNDLE = "extras"

    private fun toBundle(message: NotificationCompat.MessagingStyle.Message): Bundle? {
        val bundle = Bundle()
        bundle.putCharSequence(KEY_TEXT, message.text)
        bundle.putLong(KEY_TIMESTAMP, message.timestamp) // Must be included even for 0
        val sender = message.person
        if (sender != null) {
            bundle.putCharSequence(KEY_SENDER, sender.name) // Legacy listeners need this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) bundle.putParcelable(KEY_SENDER_PERSON, sender.toAndroidPerson())
        }
        if (message.dataMimeType != null) bundle.putString(KEY_DATA_MIME_TYPE, message.dataMimeType)
        if (message.dataUri != null) bundle.putParcelable(KEY_DATA_URI, message.dataUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !message.extras.isEmpty) bundle.putBundle(KEY_EXTRAS_BUNDLE, message.extras)
        //if (message.isRemoteInputHistory()) bundle.putBoolean(KEY_REMOTE_INPUT_HISTORY, message.isRemoteInputHistory());
        return bundle
    }

    private fun getBundleArrayForMessages(messages: List<NotificationCompat.MessagingStyle.Message>): Array<Bundle?>? {
        val N = messages.size
        val bundles = arrayOfNulls<Bundle>(N)
        for (i in 0 until N) bundles[i] = toBundle(messages[i])
        return bundles
    }


    fun flatIntoExtras(messaging: NotificationCompat.MessagingStyle, extras: Bundle) {
        val user = messaging.user
        if (user != null) {
            extras.putCharSequence(NotificationCompat.EXTRA_SELF_DISPLAY_NAME, user.name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) extras.putParcelable(Notification.EXTRA_MESSAGING_PERSON, user.toAndroidPerson()) // Not included in NotificationCompat
        }
        if (messaging.conversationTitle != null) extras.putCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE, messaging.conversationTitle)
        val messages = messaging.messages
        if (messages.isNotEmpty()) extras.putParcelableArray(NotificationCompat.EXTRA_MESSAGES, getBundleArrayForMessages(messages))
        //if (! mHistoricMessages.isEmpty()) extras.putParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES, MessagingBuilder.getBundleArrayForMessages(mHistoricMessages));
        extras.putBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION, messaging.isGroupConversation)
    }
}
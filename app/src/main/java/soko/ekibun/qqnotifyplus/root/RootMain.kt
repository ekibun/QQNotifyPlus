package soko.ekibun.qqnotifyplus.root

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava
import soko.ekibun.qqnotifyplus.BuildConfig
import soko.ekibun.qqnotifyplus.util.NotificationUtil
import java.util.*
import android.content.pm.ApplicationInfo

object RootMain {

    fun getLaunchScript(context: Context): List<String> {
        val paramList = ArrayList<String>()
        paramList.add(context.packageCodePath)
        return RootJava.getLaunchScript(context, RootMain::class.java, null, null, paramList.toTypedArray(), context.packageName + ":root")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        RootJava.restoreOriginalLdLibraryPath()
        // implement interface
        val ipc = object : IIPC.Stub() {
            override fun getPid(): Int {
                return android.os.Process.myPid()
            }
            override fun getIntent(pendingIntent: PendingIntent): Intent {
                return getIntentFromPendingIntent(pendingIntent)
            }
            @SuppressLint("WrongConstant")
            override fun sendNotification(pkg: String, tag: String, id: Int, notification: Notification, channelId: String, channelName: String) {
                try {
                    val context = RootJava.getPackageContext(pkg)
                    val appInfo = (notification.extras.getParcelable("android.appInfo") as? ApplicationInfo)?: context.packageManager.getApplicationInfo(pkg, 8192)
                    val manager = NotificationUtil.getNotificationManager(context)
                    val channel = if (Build.VERSION.SDK_INT >= 26)
                        listOf(NotificationUtil.createChannel(channelId, "$channelName+", null)) else ArrayList()
                    createNotificationChannelsForPackage(manager, pkg, appInfo.uid, channel)
                    enqueueNotificationWithTag(manager, pkg, pkg, tag, id, notification, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

        // send it to the non-root end
        try {
            RootIPC(BuildConfig.APPLICATION_ID, ipc, 0, 30 * 1000, true)
        } catch (e: RootIPC.TimeoutException) {
            e.printStackTrace()
        }

    }

    @SuppressLint("PrivateApi")
    fun getIntentFromPendingIntent(pendingIntent: PendingIntent): Intent {
        try {
            val getIntent = PendingIntent::class.java.getDeclaredMethod("getIntent")
            return getIntent.invoke(pendingIntent) as Intent
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    @SuppressLint("PrivateApi")
    fun createNotificationChannelsForPackage(manager: NotificationManager, pkg: String, uid: Int, channels: List<NotificationChannel>) {
        try {
            val method = NotificationManager::class.java.getMethod("getService")
            val obj = method.invoke(manager)
            val classINotificationManager = Class.forName("android.app.INotificationManager\$Stub\$Proxy")
            if (Build.VERSION.SDK_INT >= 26) {
                val classParceledListSlice = Class.forName("android.content.pm.ParceledListSlice")
                val createNotificationChannelsForPackage = classINotificationManager.getDeclaredMethod("createNotificationChannelsForPackage", String::class.java, Integer.TYPE, classParceledListSlice)
                val parceledListSlice = classParceledListSlice.getConstructor(List::class.java).newInstance(channels)
                createNotificationChannelsForPackage.invoke(obj, pkg, uid, parceledListSlice)
            }
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    @SuppressLint("PrivateApi")
    fun enqueueNotificationWithTag(manager: NotificationManager, pkg: String, opPkg: String, tag: String, id: Int, notification: Notification, userId: Int){
        try {
            val method = NotificationManager::class.java.getMethod("getService")
            val obj = method.invoke(manager)
            val classINotificationManager = Class.forName("android.app.INotificationManager\$Stub\$Proxy")
            if (Build.VERSION.SDK_INT >= 26) {
                val enqueueNotificationWithTag = classINotificationManager.getDeclaredMethod("enqueueNotificationWithTag", String::class.java, String::class.java, String::class.java, Integer.TYPE, Notification::class.java, Integer.TYPE)
                enqueueNotificationWithTag.invoke(obj, pkg, opPkg, tag, id, notification, userId)
            }
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}

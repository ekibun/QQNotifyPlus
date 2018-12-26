package soko.ekibun.qqnotifyplus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent



class AccessibilityMonitorService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.packageName == null || event.className == null)
                    return
                val tag =NotificationMonitorService.tags[ event.packageName.toString()]?:return
                val className = event.className.toString()
                Log.v("class", className)
                if ("com.tencent.mobileqq.activity.SplashActivity" == event.className || "com.dataline.activities.LiteActivity" == event.className)
                    startService(Intent(this, NotificationMonitorService::class.java).putExtra("tag", tag.ordinal))
                else if (className.startsWith("cooperation.qzone."))
                    startService(Intent(this, NotificationMonitorService::class.java).putExtra("tag", (NotificationMonitorService.qzoneTag[tag]?:tag).ordinal))
            }
        }
    }

    override fun onInterrupt() {
        //辅助服务被关闭 执行此方法
    }
}

package soko.ekibun.qqnotifyplus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityMonitorService : AccessibilityService() {

    var currentActivity = ""
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val tag = NotificationMonitorService.tags[ event.packageName.toString()]?:return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->{
                if ("com.tencent.mobileqq.activity.SplashActivity" == currentActivity || "com.dataline.activities.LiteActivity" == currentActivity)
                    enumNode(rootInActiveWindow, "root|"){subNode->
                        if(subNode.contentDescription in listOf("群资料卡", "返回消息")) {
                            enumNode(subNode.parent, "toolbar|") {node->
                                if(node.className == "android.widget.TextView" && !node.text.isNullOrEmpty()) {
                                    startService(Intent(this, NotificationMonitorService::class.java).putExtra("key", "${tag.name}_${node.text}"))
                                    true
                                } else false
                            }
                            true
                        } else false
                    }
                else if (currentActivity.startsWith("cooperation.qzone."))
                    enumNode(rootInActiveWindow, "root|"){node->
                        if(node.contentDescription == "消息 标题") {
                            startService(Intent(this, NotificationMonitorService::class.java).putExtra("key", "${NotificationMonitorService.qzoneTag[tag]?:tag}_qzone"))
                            true
                        } else false
                    }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.packageName == null || event.className == null)
                    return
                currentActivity = event.className.toString()
                Log.v("activity", currentActivity)
            }
        }
    }

    private fun enumNode(accessibilityNodeInfo: AccessibilityNodeInfo, subCount: String, callback:(AccessibilityNodeInfo)->Boolean){
        try{
            for(i in 0 until accessibilityNodeInfo.childCount){
                val subNode = accessibilityNodeInfo.getChild(i)?:continue
                if(callback(subNode)) return
                enumNode(subNode, "$subCount|", callback)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        //辅助服务被关闭 执行此方法
    }
}

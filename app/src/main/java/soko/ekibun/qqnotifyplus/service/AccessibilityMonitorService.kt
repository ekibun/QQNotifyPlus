package soko.ekibun.qqnotifyplus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityMonitorService : AccessibilityService() {

    var currentActivity = ""
    var currentPackage = ""
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->{
                val tag = NotificationMonitorService.tags[currentPackage]?:return
                if (currentActivity.startsWith("cooperation.qzone."))
                    enumNode(rootInActiveWindow?:return, "root|"){node->
                        if(node.contentDescription == "消息 标题") {
                            startService(Intent(this, NotificationMonitorService::class.java).putExtra("key", "${NotificationMonitorService.qzoneTag[tag]?:tag}_qzone"))
                            true
                        } else false
                    }
                else when(currentPackage){
                    "com.tencent.qqlite", "com.tencent.mobileqq" -> {
                        if(currentActivity in listOf("com.tencent.mobileqq.activity.SplashActivity", "com.dataline.activities.LiteActivity"))
                            enumNode(rootInActiveWindow?:return, "root|"){subNode->
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
                    }
                    "com.tencent.tim" -> {
                        if(currentActivity in listOf("com.tencent.mobileqq.activity.SplashActivity", "com.tencent.mobileqq.activity.ChatActivity"))
                            enumNode(rootInActiveWindow?:return, "root|"){subNode->
                                if(subNode.contentDescription in listOf("返回消息界面", "群资料卡")) {
                                    enumNode(subNode.parent.parent?:subNode.parent, "toolbar|") {node->
                                        if(node.className == "android.widget.TextView" && !node.text.isNullOrEmpty() && node.contentDescription == null) {
                                            startService(Intent(this, NotificationMonitorService::class.java).putExtra("key", "${tag.name}_${node.text}"))
                                            true
                                        } else false
                                    }
                                    true
                                } else false
                            }
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.packageName == null || event.className == null) return
                currentActivity = event.className.toString()
                currentPackage = event.packageName.toString()
                Log.v("activity", "$currentPackage $currentActivity")
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

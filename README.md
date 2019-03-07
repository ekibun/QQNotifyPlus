![icon](./app/src/main/ic_launcher-web.png)
# QQNotifyPlus
[![GitHub release](	https://img.shields.io/github/release-pre/ekibun/QQNotifyPlus.svg)](https://github.com/ekibun/QQNotifyPlus)
[![GitHub license](	https://img.shields.io/github/license/ekibun/QQNotifyPlus.svg)](https://github.com/ekibun/QQNotifyPlus)
![](https://img.shields.io/github/downloads/ekibun/QQNotifyPlus/total.svg) 
## 主要功能
### 替换QQ消息通知
支持QQ正式版、轻聊版、TIM
- 使用SVG图标，可随导航栏变色
- 使用Message Style显示每条消息的内容
### root模式
自alpha20190207增加root模式

在第一次截获通知时请求root，授权后增加如下功能
- 通知在原app上显示
- 保存intent直接打开会话窗口 
## 开始使用
手动打开如下权限
- 通知使用权（required）
- 无障碍(关闭则无法在QQ打开时清理通知）
- 自启动（MIUI等）

注，由于Android8使用通知渠道管理通知，app不提供设置界面，请通过设置调整通知样式 
## 感谢以下的开源项目及作者
- [Android Open Source Project](http://source.android.com/)
- [libRootJava](https://github.com/Chainfire/librootjava)

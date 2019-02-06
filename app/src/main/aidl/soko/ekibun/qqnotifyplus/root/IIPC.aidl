// IIPC.aidl
package soko.ekibun.qqnotifyplus.root;

interface IIPC {
    int getPid();
    Intent getIntent(in PendingIntent pendingIntent);
    void sendNotification(String pkg, String tag, int id, in Notification notification, String channelId, String channelName);
}
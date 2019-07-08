// IIPC.aidl
package soko.ekibun.qqnotifyplus.root;

interface IIPC {
    Intent getIntent(in PendingIntent pendingIntent);
    int getUid(String pkg, in Notification notification);
    PendingIntent getPendingIntent(in Intent intent, String pkg, int uid, int requestCode, int flags);
    void sendNotification(String pkg, String tag, int id, in Notification notification, String channelId, String channelName, int uid);
}
package soko.ekibun.qqnotifyplus.root;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import eu.chainfire.librootjava.RootIPC;
import eu.chainfire.librootjava.RootJava;
import soko.ekibun.qqnotifyplus.BuildConfig;
import soko.ekibun.qqnotifyplus.util.NotificationUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PendingIntentGetter {
    /**
     * Call this from non-root code to generate the script to launch the root code
     *
     * @param context Application or activity context
     * @param params Parameters to pass
     * @param libs Native libraries to pass (no extension), for example libmynativecode
     * @return Script
     */
    public static List<String> getLaunchScript(Context context, String[] params, String[] libs) {
        // Add some of our parameters to whatever has been passed in
        // Doing it this way is an example of separating parameters you need every time from
        // parameters that may differ based on what the app is doing.
        // If we didn't do this, we'd use params directly in the getLaunchScript call below
        List<String> paramList = new ArrayList<String>();

        // Path to our APK - this is just an example of parameter passing, there are several ways
        // to get the path of the APK from the code running as root without this.
        paramList.add(context.getPackageCodePath());

        // Add paths to our native libraries
        if (libs != null) {
            for (String lib : libs) {
                paramList.add(RootJava.getLibraryPath(context, lib));
            }
        }

        // Originally passed parameters
        if (params != null) {
            Collections.addAll(paramList, params);
        }

        // Create actual script
        return RootJava.getLaunchScript(context, PendingIntentGetter.class, null, null, paramList.toArray(new String[0]), context.getPackageName() + ":root");
    }

    public static void main(String[] args) {
        // implement interface
        IBinder ipc = new IIPC.Stub() {
            @Override
            public int getPid() {
                return android.os.Process.myPid();
            }

            @Override
            public Intent getIntent(PendingIntent pendingIntent) {
                return getIntentFromPendingIntent(pendingIntent);
            }

            @Override
            public void sendNotification(String pkg, String tag, int id, Notification notification, String channelId, String channelName) {
                try {
                    Context context = RootJava.getPackageContext(pkg);
                    NotificationManager manager = NotificationUtil.INSTANCE.buildChannel(context, channelId, channelName);
                    manager.notify(tag, id, notification);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // send it to the non-root end
        try {
            new RootIPC(BuildConfig.APPLICATION_ID, ipc, 0, 30 * 1000, true);
        } catch (RootIPC.TimeoutException e) {
            e.printStackTrace();
        }
    }


    /**
     * Return the Intent for PendingIntent.
     * Return null in case of some (impossible) errors: see Android source.
     * @throws IllegalStateException in case of something goes wrong.
     * See {@link Throwable#getCause()} for more details.
     */
    public static Intent getIntentFromPendingIntent(PendingIntent pendingIntent) throws IllegalStateException {
        try {
            @SuppressLint("PrivateApi")
            Method getIntent = PendingIntent.class.getDeclaredMethod("getIntent");
            return (Intent) getIntent.invoke(pendingIntent);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

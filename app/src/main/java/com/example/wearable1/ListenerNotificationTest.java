package com.example.wearable1;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

public class ListenerNotificationTest extends NotificationListenerService {

    //    BroadcastReceiver
    private NotificationListenerRecetor notificationListenerRecetor;
    public String message = "";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        message = sbn.getNotification().extras.getString("android.title")
                + " com " + sbn.getNotification().extras.getString("android.text");
        if(MainActivity.connected)
            MainActivity.bt.send("noti," + message + ",\n", false);
        /*Toast.makeText(getApplicationContext(), "Receive notification: "
                        + sbn.getNotification().extras.getString("android.title")
                        + " com " + sbn.getNotification().extras.getString("android.text"),
                Toast.LENGTH_LONG).show();*/
        sendBroadcast(new Intent("Register").putExtra("Register", sbn.getNotification()
                .extras.getString("android.text")));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Toast.makeText(getApplicationContext(), "Remove notification: "
                        + sbn.getNotification().extras.getString("android.title")
                        + " com " + sbn.getNotification().extras.getString("android.text"),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Toast.makeText(getApplicationContext(), "Connected! The service is connected!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Toast.makeText(getApplicationContext(), "Disconnected! The service is disconnected!", Toast.LENGTH_LONG).show();String notificationListenerString = Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners");
        //Check notifications access permission
        if (!(notificationListenerString == null || !notificationListenerString.contains(getPackageName())))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(ComponentName.createRelative(this.getApplicationContext().getPackageName(), "ListenerNotificationTest"));
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationListenerRecetor = new NotificationListenerRecetor();
        IntentFilter intentFilter = new IntentFilter("Register");
        registerReceiver(notificationListenerRecetor, intentFilter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(notificationListenerRecetor);
    }

    public static class NotificationListenerRecetor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

}

package com.plugin.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.app.NotificationManager;
import android.os.Bundle;

public class BackgroundNotificationReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("NOTIFS", "BROADCAST RECEIVER");

    Bundle extras = intent.getExtras();
    boolean isPushPluginActive = PushPlugin.isActive();


    if (extras != null)	{
        Bundle originalExtras = extras.getBundle("pushBundle");
        
        originalExtras.putBoolean("foreground", false);
        originalExtras.putBoolean("coldstart", !isPushPluginActive);

        String actionSelected = extras.getString(GCMIntentService.ACTION_KEY);
        originalExtras.putString("actionSelected", actionSelected);

        PushPlugin.sendExtras(originalExtras);

        // cancel the notification when an action button is clicked
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(GCMIntentService.getAppName(context), extras.getInt("notId"));
    }
  }

}

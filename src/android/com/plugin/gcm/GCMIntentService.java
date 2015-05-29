package com.plugin.gcm;

import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

@SuppressLint("NewApi")
public class GCMIntentService extends GcmListenerService {

	private static final String TAG = "GCMIntentService";
    public static final String ACTION_KEY = "PUSH_NOTIFICATION_ACTION_IDENTIFIER";
	
	public GCMIntentService() {
		super();
	}

	@Override
	public int onStartCommand(Intent intent, int i1, int i2) {
		String regId = intent.getStringExtra("regId");
		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

//	@Override
//	public void onUnregistered(Context context, String regId) {
//		Log.d(TAG, "onUnregistered - regId: " + regId);
//	}

	@Override
	public void onMessageReceived(String from, Bundle extras) {

		Log.d(TAG, "onMessage - context: " + this);

		// Extract the payload from the message
        Log.v(TAG, "extras: " + extras.toString());

		if (extras != null)
		{
			// if we are in the foreground, just surface the payload, else post it to the statusbar
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
                PushPlugin.sendExtras(extras);
			}
			else {
				extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                if (extras.getString("message") != null && extras.getString("message").length() != 0) {
					createNotification(this, extras);
                }
            }
        }
	}

	public void createNotification(Context context, Bundle extras)
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		int defaults = Notification.DEFAULT_ALL;

		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {}
		}

		Notification.Builder mBuilder = new Notification.Builder(context)
				.setDefaults(defaults)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(extras.getString("title"))
				.setTicker(extras.getString("title"))
				.setContentIntent(contentIntent)
				.setAutoCancel(true);

		String message = extras.getString("message");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		
		int notId = 0;
		
		try {
			notId = Integer.parseInt(extras.getString("notId"));
		}
		catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		}
		catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}

        String cat_id = extras.getString("categoryName");
        if(cat_id != null) {
          JSONObject category = PushPlugin.categories.optJSONObject(cat_id);
          JSONArray actions   = category.optJSONArray("actions"); 
          int requestCode;

          // add each action
          for(int i = 0; i < actions.length(); i++) {
            JSONObject action = actions.optJSONObject(i);
            requestCode = new Random().nextInt();

            // set up correct receiver for foreground or background handler
            //if(extras.getString("activationMode") == "background") {
            // notificationIntent = new Intent(this, BackgroundNotificationReceiver.class);
            //}
            //else {
              notificationIntent = new Intent(this, PushHandlerActivity.class);
            //}
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            notificationIntent.putExtra("pushBundle", extras);
            notificationIntent.putExtra("notId", notId);
            notificationIntent.putExtra(ACTION_KEY, action.optString("identifier"));

            contentIntent = PendingIntent.getBroadcast(this, requestCode, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder  = mBuilder.addAction(action.optInt("icon"), action.optString("title"), contentIntent);
          }
        }

		mNotificationManager.notify((String) appName, notId, mBuilder.build());
	}
	
	public static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
//	@Override
//	public void onError(Context context, String errorId) {
//		Log.e(TAG, "onError - errorId: " + errorId);
//	}

}

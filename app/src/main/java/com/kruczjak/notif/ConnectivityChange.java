package com.kruczjak.notif;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ConnectivityChange extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("log", false)) return;

        if (FunctionsMain.isInternetAccess(context)) {
            Log.i("NET", "connected");
            sendBroadcast(context, 3);
            if (!NotService.service_state && preferences.getBoolean("service", true)) {
                Intent service = new Intent(context, NotService.class);
                context.startService(service);
                Log.i("SERVICE", "START");
            }

            if (!ChatService.service_state) {
                Log.i("SERVICECHAT", "START");
                Intent service = new Intent(context, ChatService.class);
                context.startService(service);
            }

        } else {
            Log.i("NET", "Not connected");
            shutdownAllServices(context);
            sendBroadcast(context, 4);
        }
    }

    /**
     * Stops all services.
     *
     * @param context
     */
    private void shutdownAllServices(Context context) {
        Intent service = new Intent(context, NotService.class);
        context.stopService(service);
        service = new Intent(context, ChatService.class);
        context.stopService(service);
    }

    /**
     * Send broadcast to Starter.java
     *
     * @param context
     * @param number
     */
    private void sendBroadcast(Context context, int number) {
        Intent brad = new Intent("com.kruczjak.notif");
        brad.putExtra("t", number);
        context.sendBroadcast(brad);
    }
}
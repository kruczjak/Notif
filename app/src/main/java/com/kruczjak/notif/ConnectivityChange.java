package com.kruczjak.notif;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class ConnectivityChange extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.getBoolean("log", false)) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

            if (activeNetInfo != null && activeNetInfo.isConnectedOrConnecting()) {
                Log.i("NET", "connected");

                if (preferences.getBoolean("service", true)) {
                    Intent brad = new Intent("com.kruczjak.notif");
                    brad.putExtra("t", 3);
                    context.sendBroadcast(brad);

                    if (!NotService.service_state) {
                        Intent service = new Intent(context, NotService.class);
                        context.startService(service);
                        //TODO
                        Log.i("SERVICE", "START");
                    } else
                        Log.i("SERVICE", "IS_RUNNING OR DISABLED");
                }

                if (!ChatService.service_state) {
                    Log.i("SERVICECHAT", "START");
                    Intent service = new Intent(context, ChatService.class);
                    context.startService(service);
                }

            } else {
                Log.i("NET", "not connected");

                Intent brad = new Intent("com.kruczjak.notif");
                brad.putExtra("t", 4);
                context.sendBroadcast(brad);

                Intent service = new Intent(context, NotService.class);
                context.stopService(service);
                service = new Intent(context, ChatService.class);
                context.stopService(service);
            }
        }
    }
}
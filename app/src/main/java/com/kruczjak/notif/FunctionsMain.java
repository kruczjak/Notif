/**
 *
 */
package com.kruczjak.notif;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Main functions used > 1 in code. Content:
 * date process, check_rate
 *
 * @author Jakub
 */
public class FunctionsMain {

    /**
     * Shows data in notifications and in Starter.java listview.
     *
     * @param date of notification being send
     * @return customized date in string
     */
    public String date_process(Date date) {    //TODO fix deprecation warning
        SimpleDateFormat df;
        if (date.getDate() == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
            df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return df.format(date);
        } else if (date.getDate() == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1) { //TODO fix this (30&31)
            df = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "wczoraj " + df.format(date); //TODO change this constant
            //view_holder.updated.setText("wczoraj "+df.format(date));
        } else {
            df = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
            return df.format(date);
        }
    }

    /**
     * Checks refresh rate set by user, depends on network type.
     *
     * @param ctx Context of app
     * @return RATE in sec
     */
    public int check_rate(Context ctx) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);

        ConnectivityManager connectivityManager = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnectedOrConnecting()) {
            if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                Log.i("CHANGE", "MOBILE :D");
                return Integer.parseInt(preferences.getString("mobileRATE", "120"));
            } else {
                Log.i("CHANGE", "WiFi or sth else");
                return Integer.parseInt(preferences.getString("standardRATE", "90"));
            }
        } else {
            return Integer.parseInt(preferences.getString("standardRATE", "90"));
        }
    }

    /**
     * Translates object_type into user's language.
     *
     * @param object_type type of notification
     * @return response in right language
     */
    public String resolveObject_type(String object_type, Context ctx) {
        int id = ctx.getResources().getIdentifier(object_type, "string", "com.kruczjak.notif");
        if (id == 0)
            return object_type;
        else
            return ctx.getString(id);
    }

    /**
     * Checks if Internet is available.
     *
     * @return true if connected, false if disconnected
     */
    public static boolean isInternetAccess(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetInfo != null && activeNetInfo.isConnectedOrConnecting());
    }

}

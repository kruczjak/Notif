package com.kruczjak.notif;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Service to update and show notification in android from fb.
 *
 * @author Jakub Kruczek
 */
public class NotService extends Service {

    private final IBinder mBinder = new LocalBinder();
    static boolean service_state;
    private Timer updatingTimer = new Timer();
    private static final String TAG = "NOTSERVICE";
    private Request request;
    Session session; //= Session.getActiveSession(); 
    private final static String MY_APP_ID = "159414930918189";
    private FunctionsMain fuck = new FunctionsMain();
    private SharedPreferences preferences;
    PowerManager.WakeLock wl;

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        service_state = true;

//	    thanks to this - no FC
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (preferences.getBoolean("log", false)) {
            session = new Session.Builder(this).setApplicationId(MY_APP_ID).build(); //create session with this id
            session.openForRead(null);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Log.d(TAG, "START_SERVICE");

        firstRequest(getLastUpdated_time());

        TimerTask mainTask = new TimerTask() {
            public void run() {
                wl.acquire();
                if (session != null && session.isOpened()) {
                    Log.i(TAG, "CHECKNOW 1Stage ");
                    Request.executeAndWait(request);
                    Log.i(TAG, "CHECKENDED 1Stage ");
                }
                wl.release();
            }
        };

        updatingTimer.scheduleAtFixedRate(mainTask, 0, fuck.check_rate(getApplicationContext()) * 1000);

        return START_STICKY;
    }

    /**
     * Connect to database and get newest time.
     *
     * @return last updated_time
     */
    private String getLastUpdated_time() {
        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        return db.getLastUpdated();
    } //FIXME memory can be leaked

    /**
     * Starting request.
     *
     * @param updated_time
     */
    private void firstRequest(String updated_time) {

        String query = "SELECT notification_id,updated_time,href,title_text,object_type FROM notification WHERE recipient_id=me() AND is_unread=1 AND is_hidden=0";

        if (updated_time != null) {
            query += " AND updated_time>" + updated_time;
        }

        Bundle params = new Bundle();
        params.putString("q", query);
        params.putString("locale", Locale.getDefault().toString());
        Log.d("locale", Locale.getDefault().toString());

        request = new Request(session, "/fql", params, HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        jsonArrayDecodeAndTestForNew(response);
                    }
                }
        );

    }

    /**
     * Create array and pass it to test();
     *
     * @param response from request
     */
    protected void jsonArrayDecodeAndTestForNew(Response response) {
        GraphObject graphObject = response.getGraphObject();
        if (graphObject != null) {
            JSONObject jsonObject = graphObject.getInnerJSONObject();
            try {
                JSONArray array = jsonObject.getJSONArray("data");
                if (array.length() > 0)
                    saveAndShowNotification(array);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Save to database and show notifications.
     *
     * @param array with data from fb
     */
    private void saveAndShowNotification(JSONArray array) {
        JSONObject object;

        try {
            object = (JSONObject) array.get(0);
            firstRequest(object.getString("updated_time"));
            Log.i("lastTime", object.getString("updated_time"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (array.length() <= 3) {
            for (int i = 0; i < array.length(); i++) {                                    //sprawdzanie powiadomienia czy si� nadaje
                try {
                    object = (JSONObject) array.get(i);
                    Log.i("OVER", "przekazuje dane do powiadomienia ;)");
                    showNotification(object.getInt("notification_id"), object.getString("href"),            //poka� powiadomienie
                            object.getString("title_text"), object.getString("object_type"), object.getInt("updated_time"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                Log.i("OVER", "4 or more notifs");
                String[] event;
                if (array.length() < 6)
                    event = new String[array.length()];
                else
                    event = new String[6];

                DatabaseHandler db = new DatabaseHandler(getApplicationContext());
                for (int i = 0; i < array.length(); i++) {
                    object = (JSONObject) array.get(i);
                    db.checkandgo(object.getInt("notification_id"), object.getInt("updated_time"), object.getString("title_text"),
                            object.getString("href"), 1, object.getString("object_type"));
                    if (i < 6)
                        event[i] = object.getString("title_text");
                }
                db.close();
                showNumberNotification(array.length() - 1, event);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Shows the big one or just number of incoming notifications from fb
     *
     * @param i     - number of notifications
     * @param event - max 6 strings title_text
     */
    protected void showNumberNotification(int i, String[] event) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Nowe powiadomienia: " + i)
                        .setContentText("Kliknij, by odczytać");

        Intent resultIntent = new Intent(this, Starter.class);
        //resultIntent.putExtra("not readed", "");	//TODO
        resultIntent.putExtra("not", true);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        mBuilder.setContentIntent(resultPendingIntent);
        //ring-ring :P
        //Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Uri alarmSound = Uri.parse(preferences.getString("notifications_ringtone", "content://settings/system/notification_sound"));
        mBuilder.setSound(alarmSound);
        //vibration
        if (preferences.getBoolean("notifications_vibrate", true)) {
            long[] vibrate = {0, 100, 200, 300};            //TODO customized vibration PRO
            mBuilder.setVibrate(vibrate);
        }
        //auto-close
        mBuilder.setAutoCancel(true);
        //call manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //big one
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Nowe powiadomienia: " + i);
        inboxStyle.setSummaryText("Kliknij, by odczytać");
        for (int k = 0; k < event.length; k++) {
            inboxStyle.addLine(event[k]);
        }
        mBuilder.setStyle(inboxStyle);
        mNotificationManager.notify(1, mBuilder.build());
    }

    /**
     * Shows notification in ANDROID and save record to db
     *
     * @param ID          notification_id
     * @param href        notification href target
     * @param title_text  notification main text
     * @param object_type notification type
     */
    protected void showNotification(int ID, String href, String title_text, String object_type, int updated_time) {
        DatabaseHandler db = new DatabaseHandler(this);
        db.checkandgo(ID, updated_time, title_text, href, 1, object_type);
        db.close();

        object_type = fuck.resolveObject_type(object_type, this);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(object_type)
                        .setContentText(title_text);
        //browser
        Intent intent = new Intent(Intent.ACTION_VIEW);    //intent to redirect to web browser
        intent.setData(Uri.parse(href));//use this link
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK); //open on this screen
        mBuilder.setContentIntent(pending); //set content view open
        //ring-ring :P
        Uri alarmSound = Uri.parse(preferences.getString("notifications_ringtone", "content://settings/system/notification_sound"));
        mBuilder.setSound(alarmSound);
        //vibration
        if (preferences.getBoolean("notifications_vibrate", true)) {
            long[] vibrate = {0, 100, 200, 300};            //TODO customized vibration PRO
            mBuilder.setVibrate(vibrate);
        }
        //auto-close
        mBuilder.setAutoCancel(true);
        //call manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //big one
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(title_text));

        mNotificationManager.notify(ID, mBuilder.build());
    }

    /**
     * Cancel the timer, set state of service to false.
     */
    @Override
    public void onDestroy() {
        if (!wl.isHeld())
            wl.acquire();

        updatingTimer.cancel();
        service_state = false;
        wl.release();
    }

    public class LocalBinder extends Binder {
        NotService getService() {
            return NotService.this;
        }
    }
}

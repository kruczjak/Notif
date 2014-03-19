/**
 *
 */
package com.kruczjak.notif;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author Jakub Kruczek
 */
public class FBRequestMessages {

    private static final String TAG = "FBRequestMessages";
    private static final String LINK = "me";
    private Context ctx;

    FBRequestMessages(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Design for first run, after logged in.
     * Uses big amount of data.
     * Fucking JSON
     * ...
     * Download 10 last conversations, each with 13 last messages.
     * Cannot receive pictures :/
     */
    public void firstTimeRun(final ProgressDialog dialog) {
        final Starter ctxS = (Starter) ctx;
        String fields = "inbox.limit(10).fields(comments.limit(20).fields(message,from),to,unread)";

        Bundle params = new Bundle();
        params.putString("fields", fields);

        Session session = Session.getActiveSession();

        Request req = new Request(session, LINK, params, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if (response != null) {
                    try {
                        firstTimeProcess(response, 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "redictering to FBFriends");
                if (FunctionsMain.isInternetAccess(ctx))
                    ctxS.loggedInActions2(dialog);
                else {
                    Log.e(TAG, "LOGIN:failure, no net");
                    ctxS.logOut();
                    dialog.cancel();
                }
            }
        });

        Request.executeBatchAsync(req);
    }

    /**
     * Processing reply from server.
     *
     * @param params
     * @param lastTime
     */
    private void requestProcess(Bundle params, final int lastTime) {
        Session session = Session.getActiveSession();

        Request req = new Request(session, LINK, params, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if (response != null) {
                    try {
                        firstTimeProcess(response, lastTime);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        Request.executeBatchAsync(req);
    }

    /**
     * For first time run
     *
     * @param params
     */
    private void requestProccess(Bundle params) {
        requestProcess(params, 0);
    }

    /**
     * Parse response after facebook request. Maybe not only for firstRun, will see..
     *
     * @param response
     * @throws JSONException
     */
    private void firstTimeProcess(Response response, int lastTime) throws JSONException {
        Log.i(TAG, "Proccessing...");

        GraphObject graphObject = response.getGraphObject();
        if (graphObject != null) {
            JSONObject main = graphObject.getInnerJSONObject();
            String myFBID = main.getString("id");
            Log.i(TAG, myFBID);

            JSONArray data = main.getJSONObject("inbox").getJSONArray("data");
            JSONObject dataPart;
            JSONArray dataIN;
            for (int i = 0; i < data.length(); i++) {
                dataPart = (JSONObject) data.get(i);
                //dostep do updated_time and to!!!
                JSONArray toData = dataPart.getJSONObject("to").getJSONArray("data");
                JSONObject to = (JSONObject) toData.get(0);

                if (to.getString("id").equals(myFBID) && !toData.isNull(1)) {
                    to = (JSONObject) toData.get(1);
                }
                String fbid = to.getString("id");
                String name = to.getString("name");

                Log.i(TAG, fbid);
                Log.i(TAG, name);

                int unreadCount = dataPart.getInt("unread");
                Log.i(TAG, Integer.toString(unreadCount));

                dataIN = dataPart.getJSONObject("comments").getJSONArray("data");    //these are now comments
                firstTimeMessageThread(dataIN, fbid, unreadCount, lastTime, name);
            }
        }

        if (lastTime != 0) {
            Intent intent = new Intent("com.kruczjak.notif");
            intent.putExtra("t", 2);
            ctx.sendBroadcast(intent);
        }
    }

    /**
     * Download messages in specified thread. PS. databases are very bad in use...
     *
     * @param dataIN
     * @throws JSONException
     */
    private void firstTimeMessageThread(JSONArray dataIN, String fbid, int unrCount, int lastTime, String name) throws JSONException {
        if (dataIN != null) {
            JSONObject data;
            ChatDB db = ChatDB.getInstance(ctx);
            for (int k = 0; k < dataIN.length(); k++) {
                data = dataIN.getJSONObject(k);
                if (!data.isNull("message")) {
                    String text = data.getString("message");

                    Log.i(TAG, data.getString("created_time"));    //motherfucker, why isn't it in ms?
                    String dateOld = data.getString("created_time");

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", Locale.getDefault());

                    int converted = 0;
                    try {
                        converted = (int) (dateFormat.parse(dateOld).getTime() / 1000);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, Integer.toString(converted));


                    int direction = 0;
                    if (!data.getJSONObject("from").getString("id").equals(fbid))
                        direction = 1;

                    int unreaded = 0;

                    if (unrCount > 0) {
                        unreaded = 1;
                        unrCount--;
                    }

                    if (lastTime < converted)
                        db.MessageAdder(fbid, text, converted, unreaded, direction, name);

                }
            }
            db.close();
        }
    }

    /**
     * Started when ChatService starts.
     */
    public void startAppRun() {
        ChatDB db = ChatDB.getInstance(ctx);
        int lastTime = db.getLastUpdated();
        db.close();

        String fields = "inbox.since(" + Integer.toString(lastTime) + ").limit(99)";

        Bundle params = new Bundle();
        params.putString("fields", fields);

        requestProcess(params, lastTime);
    }
}

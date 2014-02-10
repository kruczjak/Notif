package com.kruczjak.notif;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Download avatar and friends info.
 * Created by krucz_000 on 31.12.13.
 */
public class FBFriends {
    protected static final String TAG = "FBFriends";
    protected Starter ctx;

    public FBFriends() {
    }

    public FBFriends(Starter ctx) {
        this.ctx = ctx;
    }

    /**
     * Download information about friends
     *
     * @param dialog dialog to disable if error
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void getFriends(final ProgressDialog dialog) throws ExecutionException, InterruptedException {
        Session session = Session.getActiveSession();
        String graphPath = "me/friends";

        Request request = new Request(session, graphPath, null, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                List<String[]> list = null;
                try {
                    processFriends(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "redictering to FBRequestMessages");
                if (ctx.isInternetAccess())
                    new FBRequestMessages(ctx).firstTimeRun(dialog);
                else {
                    Log.e(TAG, "LOGIN:failure, no net");
                    ctx.logOut();
                    dialog.cancel();
                }
            }
        });
        Request.executeBatchAsync(request);
    }

    /**
     * Processing response from facebook about friends
     *
     * @param response response from facebook
     * @throws JSONException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void processFriends(Response response) throws JSONException, ExecutionException, InterruptedException {
        JSONArray data = response.getGraphObject().getInnerJSONObject().getJSONArray("data");
        JSONObject dataPart;
        List<String[]> list = new ArrayList<String[]>();
        for (int i = 0; i < data.length(); i++) {
            dataPart = (JSONObject) data.get(i);
            String name = dataPart.getString("name");
            String fbid = dataPart.getString("id");
            list.add(new String[]{name, fbid});
        }
        Log.i(TAG, String.valueOf(list.size()));

        ChatDB chatDB = new ChatDB(ctx);
        chatDB.addFirstTimeContacts(list);
        chatDB.close();

    }

//    private String getRedictURL(String fbID) {
//        final String url = "http://graph.facebook.com/" + fbID + "/picture?width=100&height=100";
//        String photo = null;
//        try {
//            HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
//            con.setInstanceFollowRedirects( false );
//            con.connect();
//            Log.i(TAG, Integer.toString(con.getResponseCode()));
//            Log.i(TAG, con.getHeaderField("Location"));
//            photo = con.getHeaderField("Location");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return photo;
//    }

//    private void getRedictURL(final Starter ctx, final ProgressDialog dialog, final List<String[]> list) {
//        new AsyncTask<Void, Void, List<String[]>>() {
//            @Override
//            protected List<String[]> doInBackground(Void... voids) {
//                List<String[]> readyList = new ArrayList<String[]>();
//                for (String[] data : list) {
//                    String url = "http://graph.facebook.com/" + data[1] + "/picture?width=100&height=100";
//                    String photo = null;
////                    try {
////                        HttpURLConnection con = (HttpURLConnection) (new URL(url).openConnection());
////                        con.setInstanceFollowRedirects(false);
////                        con.connect();
////                        Log.i(TAG, Integer.toString(con.getResponseCode()));
////                        Log.i(TAG, con.getHeaderField("Location"));
////                        photo = con.getHeaderField("Location");
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
//                    readyList.add(new String[]{data[0], data[1], photo});
//                }
//                return readyList;
//            }
//
//            @Override
//            protected void onPostExecute(List<String[]> list) {
//                ChatDB chatDB = new ChatDB(ctx);
//                chatDB.addFirstTimeContacts(list);
//                chatDB.close();
//
//                Log.i(TAG, "redictering to FBRequestMessages");
//                if (ctx.isInternetAccess())
//                    new FBRequestMessages(ctx).firstTimeRun(dialog);
//                else {
//                    Log.e(TAG, "LOGIN:failure, no net");
//                    ctx.logOut();
//                    dialog.cancel();
//                }
//            }
//        }.execute();
//    }

    /**
     * Get new link and then fire load updated photo
     *
     * @param avatar where to load photo
     * @param fbID   facebook id
     * @param from   0 if message overview, 1 if contacts
     */
    public void getNewPhotoLinkAndUpdatePhoto(final Avatar avatar, final String fbID, final int from) {
        final String url = "http://graph.facebook.com/" + fbID + "/picture?width=100&height=100";
        AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String photo = null;
                try {
                    HttpURLConnection con = (HttpURLConnection) (new URL(url).openConnection());
                    con.setInstanceFollowRedirects(false);
                    con.connect();
                    Log.i(TAG, Integer.toString(con.getResponseCode()));
                    photo = con.getHeaderField("Location");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return photo;
            }

            @Override
            protected void onPostExecute(String photo) {
                if (photo != null) {
                    ChatDB chatDB = new ChatDB(avatar.getContext());
                    chatDB.updatePhoto(fbID, photo);
                    chatDB.close();

                    avatar.setTested(true);

                    Intent intent = new Intent("com.kruczjak.notif");
                    if (from == 0) {
                        intent.putExtra("t", 2);
                        avatar.getContext().sendBroadcast(intent);
                    } else if (from == 1) {
                        intent.putExtra("t", 1);
                        avatar.getContext().sendBroadcast(intent);
                    }
                    avatar.setTesting(false);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            asyncTask.execute();
        }
    }
}


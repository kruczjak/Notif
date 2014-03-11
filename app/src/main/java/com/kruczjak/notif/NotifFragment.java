/**
 *
 */
package com.kruczjak.notif;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.kruczjak.notif.Starter.NotifCommunicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * @author Jakub
 */
public class NotifFragment extends SherlockFragment implements OnRefreshListener, NotifCommunicator {
    private static final String TAG = "NotifFragment";
    private ListView lv;
    private Cursor c;
    private DatabaseHandler db;
    private CursorNotifAdapter cA;
    private Runnable runnable;
    private Handler handler = new Handler();
    private FunctionsMain fuck = new FunctionsMain();
    private PullToRefreshLayout mPullToRefreshLayout;
    private boolean handler_running = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
// Inflate the layout
        View view = inflater.inflate(R.layout.notif, container, false);
        if (view != null) {
            lv = (ListView) view.findViewById(R.id.listView1);
        }

        initPullToRefresh(view);

        runnable = new Runnable() {

            @Override
            public void run() {
                mPullToRefreshLayout.setRefreshing(true);
                c.moveToFirst();
//                checkFacebookNotification(" AND updated_time>" + c.getString(1));
//                handler.postDelayed(this, fuck.check_rate(getActivity()) * 1000);
            }

        };

        initListView();
        firstInitRequest();
        return view;
    }

    private void initPullToRefresh(View view) {
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(getActivity())
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
    }

    private void initListView() {

        lv.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                openBrowser(position);
            }

            private void openBrowser(int position) {
                Intent intent = new Intent(Intent.ACTION_VIEW);    //intent to redirect to web browser
                int i = c.getPosition();    //revert to last position or errors in listview -_-
                c.moveToPosition(position);
                String href = c.getString(3);
                c.moveToPosition(i);
                intent.setData(Uri.parse(href));
                startActivity(intent);
            }
        });

        new Handler().post(new Runnable() {    //thread for loading data from db
            @Override
            public void run() {
                db = new DatabaseHandler(getActivity());
                c = db.getMainall();
                cA = new CursorNotifAdapter(getActivity(), c, false);
                lv.setAdapter(cA);
                db.close();
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        stopHandler();
    }

    @Override
    public void onResume() {
        super.onResume();
        timerStart();
    }

    @Override
    public void onRefreshStarted(View view) {

    }

    private void timerStart() {
    }

    private boolean isInternetAccess() {
        return ((Starter) getActivity()).isInternetAccess();
    }

    /**
     * Init request query for the first time.
     */
    private void firstInitRequest() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (preferences.getBoolean("log", false)) {
            if (isInternetAccess()) {
                Log.i("FIrst", "First requestGO");
                checkFacebookNotification("");

            } else {
                Toast myToast = Toast.makeText(getActivity(),
                        "Not connected (you though that I would have my data from air?) ;)",
                        Toast.LENGTH_SHORT);
                myToast.show();
            }
            timerStart();
        }
    }

    /**
     * First time request execute.
     */
    private void checkFacebookNotification(final String requestAddition) {
        Session session = Session.getActiveSession();

        String notification_numberQuery = "SELECT notification_id,href,title_text,is_unread,updated_time,object_type FROM notification WHERE recipient_id=me() AND is_hidden=0";
        notification_numberQuery += requestAddition;
        Bundle params = new Bundle();
        params.putString("q", notification_numberQuery);
        params.putString("locale", Locale.getDefault().toString());
        Log.d("locale", Locale.getDefault().toString());

        Request request = new Request(session,
                "/fql",
                params,
                HttpMethod.GET,
                new Request.Callback() {
                    public void onCompleted(Response response) {
                        GraphObject graphObject = response.getGraphObject();
                        if (graphObject != null) {
                            JSONObject jsonObject = graphObject.getInnerJSONObject();
                            try {
                                JSONArray array = jsonObject.getJSONArray("data");
                                DatabaseHandler db = new DatabaseHandler(getActivity());
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object = (JSONObject) array.get(i);
                                    db.checkandgo(object.getInt("notification_id"), object.getInt("updated_time"),
                                            object.getString("title_text"), object.getString("href"),
                                            object.getInt("is_unread"), object.getString("object_type"));
                                }
                                Log.i("TRY", "try to update");
                                db.close();
                                change_cursor();
                                if (mPullToRefreshLayout.isRefreshing())
                                    mPullToRefreshLayout.setRefreshComplete();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                });
        Request.executeBatchAsync(request);
    }


    /**
     * Refresh listview after update.
     */
    protected void change_cursor() {
        if (lv != null) {
            Log.i("TRY", "try to update2");
            new Handler().post(new Runnable() {    //thread for loading data from db
                @Override
                public void run() {
                    db = new DatabaseHandler(getActivity());
                    c = db.getMainall();
                    cA.changeCursor(c);
                    db.close();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        c.close();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Starter) getActivity()).notifCommunicator = this;
    }

    @Override
    public void stopHandler() {
        Log.i(TAG, "Stopping handler");
        Log.i(TAG, "called handlercancel");
        if (handler_running) {
            handler.removeCallbacks(runnable);
            handler_running = false;
        }
        Log.i(TAG, "handlercancel done");
    }

    @Override
    public void startHandler() {
        Log.i(TAG, "Starting handler and req");
        firstInitRequest();
    }
}

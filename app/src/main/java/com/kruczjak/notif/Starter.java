package com.kruczjak.notif;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * MainActivity with splash screen and listview.
 *
 * @author Jakub Kruczek
 */

public class Starter extends SherlockFragmentActivity {

    private boolean closingAfterChoose = false;

    public interface MessageOverviewCommunicator {
        public void refreshListView();

        public boolean onMyContextItemSelected(android.view.MenuItem item);
    }

    public interface MessageThreadCommunicator {
        public void refreshListView();

        public void sendStatus(int i);
    }

    public interface NotifCommunicator {
        public void stopHandler();

        public void startHandler();
    }

    public MessageOverviewCommunicator messageOverviewCommunicator;
    public NotifCommunicator notifCommunicator;
    public MessageThreadCommunicator messageThreadCommunicator;

    private static final int FRAGMENT_GROUP = 0;
    ChatService mService;
    protected static final String TAG = "Starter";
    private boolean isResumed = false;
    private boolean isServiceBind = false;
    private UiLifecycleHelper uiHelper; // from facebookSDK classes
    private boolean service_start = true;
    private Menu menu;
    private ContactsAdapter cca;
    private SplashFragment splash;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        startChatService();
/*facebook*/
        uiHelper = new UiLifecycleHelper(this, callback); // start facebookSDK's															// work
        uiHelper.onCreate(savedInstanceState);
/*Tabs*/
        StarterViewPager vP = (StarterViewPager) findViewById(R.id.viewpager);
        new FragmentTabPagerControl(this, vP);
        if (!preferences.getBoolean("service", true))
            vP.setDisableScrolling(true);
/*ActionBar*/
        navigationMode();
/*Binding Service*/
        if (preferences.getBoolean("log", false) && isInternetAccess())
            doBindService();
/*DrawerLayout (Contacts)*/
        initDrawerLayout();
        stopNotService();
    }

    private void startChatService() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!ChatService.service_state && preferences.getBoolean("log", false))
            if (isInternetAccess()) {
                Intent service = new Intent(this, ChatService.class);
                startService(service);
            }
    }

    /**
     * Checks if Internet is available.
     *
     * @return true if connected, false if disconnected
     */
    public boolean isInternetAccess() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetInfo != null && activeNetInfo.isConnectedOrConnecting());
    }

    /**
     * Update left friends list ASYNC.
     */
    public void updateMDrawerLayout() {
        Log.i(TAG, "drawerUpdateAfterRoster");
        new Handler().post(new Runnable() { // thread for loading data from db
            @Override
            public void run() {
                ChatDB dbcha = new ChatDB(getApplicationContext());
                cca.setFavourites(dbcha.getFav());
                cca.setOnline(getOnline());
                cca.setRest(dbcha.getOnlineContacts(getOnline()));
                cca.notifyDataSetChanged();
                dbcha.close();
            }
        });
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((ChatService.LocalBinder) service).getService();
            mService.rosterFirstStart();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    /**
     * Connects service to activity.
     */
    private void doBindService() {
        if (!isServiceBind) {
            isServiceBind = true;
            bindService(new Intent(Starter.this, ChatService.class), mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Disconnects service from activity.
     */
    private void doUnbindService() {
        if (isServiceBind) {
            isServiceBind = false;
            unbindService(mConnection);
        }
    }

    /**
     * Jakies cos dziwnego do obslugi tego czegos. No more questions :P
     * <p/>
     * Why fucking eclipse is underlining my Polished words?
     * <p/>
     * Init navigation drawer and connect with SherlockActionBar (sorry google,
     * it's better).
     */
    private void initDrawerLayout() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        final CharSequence mDrawerTitle = "Friends"; //TODO strings
        final CharSequence mTitle = getTitle();

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.icon, R.string.friends, R.string.friends) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                if (closingAfterChoose)
                    closingAfterChoose = false;
                else
                    getSupportActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        final Activity activity = this;
        new Handler().post(new Runnable() { // thread for loading data from db
            @Override
            public void run() {
                ChatDB dbcha = new ChatDB(getApplicationContext());
                cca = new ContactsAdapter(activity, dbcha.getFav(), dbcha.getOnlineContacts(getOnline()), getOnline());
                mDrawerList.setAdapter(cca);
                dbcha.close();
            }
        });

        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long arg3) {
                LinearLayout sc = (LinearLayout) findViewById(R.id.left_drawer_layout);
                closingAfterChoose = true;
                mDrawerLayout.closeDrawer(sc);
                Bundle args = cca.getData(position);

                if (args.getBoolean("notable")) {
                    ChatDB db = new ChatDB(getApplicationContext());
                    db.createMessageTable(String.valueOf(args.getInt("id")));
                    db.close();
                }
                addMessageThreadFragment(args);
            }

        });

        mDrawerList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                Bundle args = cca.getData(info.position);
                menu.setHeaderTitle(args.getString("name"));
                String[] menuItems = getResources().getStringArray(R.array.menu);

                ChatDB db = new ChatDB(getApplicationContext());
                boolean is = db.isFav(String.valueOf(args.getInt("id")));
                db.close();

                if (is)
                    menu.add(FRAGMENT_GROUP, 0, 0, menuItems[0]);
                else
                    menu.add(FRAGMENT_GROUP, 0, 0, menuItems[1]);
            }
        });

        EditText editText = (EditText) findViewById(R.id.finder);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.toString().equals("")) {
                    ChatDB dbcha = new ChatDB(getApplicationContext());
                    cca = new ContactsAdapter(activity, dbcha.getFav(), dbcha.getOnlineContacts(getOnline()), getOnline());
                    mDrawerList.setAdapter(cca);
                    dbcha.close();
                } else {
                    String search = charSequence.toString();
                    if (mDrawerList.getAdapter() == cca) {
                        ChatDB chatDB = new ChatDB(getApplicationContext());
                        ContactsSearchAdapter searchAdapter = new ContactsSearchAdapter(activity, chatDB.getSearched(search, getOnline()), getOnline());
                        mDrawerList.setAdapter(searchAdapter);
                        chatDB.close();
                    } else {
                        ChatDB chatDB = new ChatDB(getApplicationContext());
                        ContactsSearchAdapter searchAdapter = (ContactsSearchAdapter) mDrawerList.getAdapter();
                        searchAdapter.changeCursor(chatDB.getSearched(search, getOnline()));

                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * Stops service.
     */
    private void stopNotService() {
        Intent service = new Intent(this, NotService.class);
        stopService(service);
        NotService.service_state = true;
    }

    /**
     * Checks if service is running and turns it on, when not.
     */
    private void startNotService() {
        NotService.service_state = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (preferences.getBoolean("service", true) && preferences.getBoolean("log", false))
            if (isInternetAccess()) {
                Intent service = new Intent(this, NotService.class);
                startService(service);
            }
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle bundle = getIntent().getBundleExtra("go");

        if (bundle != null)
            addMessageThreadFragment(bundle);
/*Open notification tab, if needed*/
        if (getIntent().getBooleanExtra("not", false)) {
            ActionBar ab = getSupportActionBar();
            ab.setSelectedNavigationItem(1);
        }
    }

    private void addMessageThreadFragment(Bundle bundle) {
        MessageThread fragment = new MessageThread();
        fragment.setArguments(bundle);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        transaction.add(R.id.layout_main, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerMyReceiver();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("log", false) && isInternetAccess() && !prefs.getBoolean("first", true)) {
            doBindService();
        }

        uiHelper.onResume();
        isResumed = true;
        stopNotService();
        if (messageOverviewCommunicator != null)
            messageOverviewCommunicator.refreshListView();
    }

    private void registerMyReceiver() {
        registerReceiver(receiver, new IntentFilter("com.kruczjak.notif"));
    }

    private void unregisterMyReceiver() {
        unregisterReceiver(receiver);
    }

    @Override
    public void onPause() {
        super.onPause();

        doUnbindService();
        unregisterMyReceiver();

        uiHelper.onPause();
        isResumed = false;
        Session session = Session.getActiveSession();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (session != null && service_start && !preferences.getBoolean("firstRun", true))
            startNotService();

        service_start = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    private void addSplashFragment() {
        splash = new SplashFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.add(R.id.layout_main, splash);
        transaction.commit();
    }

    private void removeSplashFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.remove(splash);
        transaction.commit();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        Session session = Session.getActiveSession();

        if (session != null && session.isOpened()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (!preferences.getBoolean("log", false)) {
//				preferences.edit().putBoolean("log", true).commit();
//              addFacebookPermission();
            }
        } else {
            ActionBar ab = getSupportActionBar();
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            showSplash();
            Log.d(TAG, "OnResumeFragments showSplash()");
        }
    }

    /**
     * Called when session is changed.
     *
     * @param session
     * @param state
     * @param exception
     */
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Only make changes if the activity is visible
        if (isResumed) {
            FragmentManager manager = getSupportFragmentManager();
            // Get the number of entries in the back stack
            int backStackSize = manager.getBackStackEntryCount();
            // Clear the back stack
            for (int i = 0; i < backStackSize; i++) {
                manager.popBackStack();
            }

            if (state.isOpened()) {
                if (!preferences.getBoolean("log", false)) { // when called
                    // first time
                    // (after first
                    // ok)
                    preferences.edit().putBoolean("log", true).commit();

                    addFacebookPermission(); // TODO ADD VIEW TO AUTHENTICATE
                    // AFTER THIS WAS CANCELLED

                } else { // when called second time, after 2 ok

                    if (preferences.getBoolean("firstRun", true)) {
                        loggedInActions();
                    }
                }

            } else if (state.isClosed()) {
                showSplash();
            }
        }
    }

    /**
     * Starting loging actions
     */
    private void loggedInActions() {
        Log.i(TAG, "Do Login");
        ProgressDialog dialog = ProgressDialog.show(Starter.this, "",
                "Loading. Please wait...", true);

        try {
            new FBFriends(this).getFriends(dialog);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "LOGIN:1st gone");
    }

    /**
     * Ending login actions, last called
     *
     * @param dialog dialog to disable
     */
    public void loggedInActions2(final ProgressDialog dialog) {
        if (isInternetAccess()) {
            Log.i(TAG, "Splash removing");
            removeSplashFragment();
            notifCommunicator.startHandler();
            startChatService();
            doBindService();
            enableMenu(true);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            navigationMode();
            Log.i(TAG, "LOGIN:2nd gone");
        } else {
            //shit we have very big problem! no internet (ChatService should be shutdown)
            Log.e(TAG, "LOGIN:failure, no net");
            logOut();
            dialog.cancel();
            return;
        }

        updateMDrawerLayout();
        messageOverviewCommunicator.refreshListView();

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dialog.cancel();
            }
        }.execute();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("firstRun", false).commit();
    }

    /**
     * Set navigation layout to TABS if service enabled
     */
    public void navigationMode() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("service", true)) {
            ActionBar ab = getSupportActionBar();
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }
    }

    /**
     * Fucking Async adding permissions -_- it's TODO, ok but add check
     */
    private void addFacebookPermission() {

        NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, new String[]{"manage_notifications"})
                .setDefaultAudience(SessionDefaultAudience.ONLY_ME);

        Session session = Session.getActiveSession();
        session.requestNewPublishPermissions(newPermissionsRequest); // callback
        // is
        // called
        // after
        // it!

    }

    /**
     * Show Splash screen and do some damage! xD (cancel handler runnable)
     */
    public void showSplash() {
        doUnbindService();
        Intent service = new Intent(this, ChatService.class);
        stopService(service);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("log", false).commit();
        enableMenu(false);
        lockCloseDrawer();
        ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        addSplashFragment();
        if (notifCommunicator != null)
            notifCommunicator.stopHandler(); // TODO check if it's online first
        // -_-
        Log.i(TAG, "Showed");
    }

    /**
     * Some kind of facebook shit. Something like listener.
     */
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    /**
     * Create menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.starter, menu);
        // for first run!
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("log", false))
            enableMenu(false);

        return true;
    }

    /**
     * Click on item in menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            this.service_start = false; // to don't start service when looking
            // at settigsactivity
            return startSettingsActivity();
        } else if (itemId == R.id.logout) {
            return logOut();
        } else if (itemId == android.R.id.home) {
            getSupportFragmentManager().popBackStack();
            return true;
        }
        return false;
    }

    private boolean startSettingsActivity() {
        Intent settings = new Intent(this, SettingsActivity.class);
        settings.addCategory(Intent.CATEGORY_PREFERENCE);
        startActivity(settings);
        return true;
    }

    /**
     * Performs logOut from FB and clears all DB.
     */
    public boolean logOut() {
        Session session = Session.getActiveSession();
        session.closeAndClearTokenInformation();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("log", false).commit();
        preferences.edit().putBoolean("firstRun", true).commit();

        DatabaseHandler db = new DatabaseHandler(this);
        db.deleteAndCreateDB();
        db.close();

        ChatDB chatDB = new ChatDB(this);
        chatDB.deleteAndCreateDB();
        chatDB.close();
        return true;
        // TODO add delete of chat db
    }

    /**
     * Enables or disables menu buttons.
     *
     * @param enable if true
     */
    private void enableMenu(boolean enable) {
        if (menu != null) {
            MenuItem item2 = menu.findItem(R.id.logout);
            item2.setEnabled(enable);
        }
    }

    /**
     * Lock drawer in close position.
     */
    public void lockCloseDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    /**
     * Unlock drawer.
     */
    public void unlockDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    /**
     * Broadcasts: 0 - chat connected 1 - roster saved to db 2 - got roster,
     * update listview 3 - internet connected 4 - internet disconnected
     */
    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("t", -1)) {
                case 0:
                    Log.i(TAG, "Broadcast 0 received");
//                    mService.rosterFirstStart();
                    break;
                case 1:
                    Log.i(TAG, "Broadcast 1 received");
                    updateMDrawerLayout();
                    break;
                case 2:
                    messageOverviewCommunicator.refreshListView();
                    break;
                case 3:
                    Log.i(TAG, "Broadcast 3 received");
                    notifCommunicator.startHandler();
                    doBindService();
                    break;
                case 4:
                    Log.i(TAG, "Broadcast 4 received");
                    notifCommunicator.stopHandler();
                    doUnbindService();
                    break;
                case 5:
                    Log.i(TAG, "Broadcast 5 received");
                    messageThreadCommunicator.refreshListView();
                    break;
                case 6:
                    sendStatus(6);
                    break;
                case 7:
                    sendStatus(7);
                    break;
                case 8:
                    Log.i(TAG, "Broadcast 8 received");
                    break;
            }
        }

    };

    /**
     * Sends status to MessageThread
     *
     * @param i 6 for active, 7 for writing (I think)
     */
    private void sendStatus(int i) {
        if (messageThreadCommunicator != null)
            messageThreadCommunicator.sendStatus(i);
    }

    /**
     * Called after screen rotates or configuration change (like in title)(...)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Starting queue in ChatService to send messages.
     *
     * @param id   in app id
     * @param text message text
     * @param time time when image was sended
     */
    public void startProcessQueue(int id, String text, String time) {
        Log.i(TAG, "Starting proccesing queue, Starter->ChatService");
        if (mService != null)
            mService.startProcessQueue();
        else {
            Log.w(TAG, "Service ChatService is not running!");
            ChatDB db = new ChatDB(this);
            db.markError(id, text, time, 1, 0);
            messageThreadCommunicator.refreshListView();
        }
    }

    public void setChattingNow(String chattingNow) {
        if (mService != null)
            mService.setChattingNow(chattingNow);
    }

    public List<String> getOnline() {
        if (mService != null)
            return mService.getOnline();
        else
            return null;
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getGroupId() == FRAGMENT_GROUP) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

            String id = String.valueOf(cca.getData(info.position).getInt("id"));

            ChatDB db = new ChatDB(getApplicationContext());
            db.addOrDeleteFav(id);
            db.close();
            updateMDrawerLayout();
            Toast.makeText(getApplicationContext(), "Friend added or deleted", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getGroupId() == 1) {
            return messageOverviewCommunicator.onMyContextItemSelected(item);
        }
        return false;
    }
}

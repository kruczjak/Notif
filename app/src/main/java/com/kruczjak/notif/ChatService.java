package com.kruczjak.notif;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.facebook.Session;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Czacior xmpp :D
 *
 * @author Jakub
 */
public class ChatService extends Service {

    private final IBinder mBinder = new LocalBinder(); // receives callbacks
    // from app
    static boolean service_state;
    private static final String TAG = "CHAT";
    private XMPPConnection xmpp;
    private PowerManager.WakeLock wl;
    private Timer updatingTimer = new Timer();
    private Chat chatThread;
    AndroidConnectionConfiguration config;
    String chattingNow = "";
    private List<String> online;
    private Waiter waiter;

    public List<String> getOnline() {
        return online;
    }

    public void setChattingNow(String chattingNow) {
        this.chattingNow = chattingNow;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        service_state = true;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!preferences.getBoolean("firstRun", true))
            new FBRequestMessages(this).startAppRun();

        processOldNotifications();

        Log.i(TAG, "service created");
    }

    private void processOldNotifications() {
        ChatDB db = new ChatDB(this);
        Cursor c = db.getNotNotified();

        for (int i = 0; i < c.getCount(); i++) {
            c.moveToPosition(i);
            Bundle datas = new Bundle();
            datas.putInt("id", c.getInt(0));
            datas.putString("name", c.getString(2));
            datas.putString("fbid", c.getString(1));
            datas.putInt("nr", c.getInt(6));
            datas.putBoolean("start", true);
            downloadPhotoAndShowNotification(datas, c.getString(4));
        }

        c.close();
        db.close();
    }

    private void chatConfigAndStart() throws XMPPException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SmackConfiguration.setDefaultPingInterval(60);
        Log.i(TAG, Integer.toString(SmackConfiguration.getDefaultPingInterval()));
        Log.i(TAG, Integer.toString(SmackConfiguration.getPacketCollectorSize()));
        Log.i(TAG, Integer.toString(SmackConfiguration.getPacketReplyTimeout()));
        // SmackConfiguration.setPacketReplyTimeout(10000);

        config = new AndroidConnectionConfiguration("chat.facebook.com", 5222, "facebook.com");
        config.setSASLAuthenticationEnabled(true);
        // config.setSecurityMode(SecurityMode.required);
        config.setRosterLoadedAtLogin(true);
        config.setSendPresence(preferences.getBoolean("avalibility_chat", true));
        config.setReconnectionAllowed(true);

//        config.setDebuggerEnabled(true);
//        XMPPConnection.DEBUG_ENABLED = true; // TODO delete debugger

        connectAsync(config);
    }

    /**
     * Create AsyncTask.
     *
     * @param config
     */
    private void connectAsync(ConnectionConfiguration config) {
        xmpp = new XMPPConnection(config);
//        XMPPConnection.DEBUG_ENABLED = true; // TODO delete debug
        SASLAuthentication.registerSASLMechanism("X-FACEBOOK-PLATFORM", SASLXFacebookPlatformMechanism.class);
        SASLAuthentication.supportSASLMechanism("X-FACEBOOK-PLATFORM", 0);

        if (!xmpp.isConnected()) {
            online = new ArrayList<String>();
            setupListeners();
            new ConnectionEstablishing().execute();
        }
    }

    private void setupListeners() {
        Log.i(TAG, "Setting up listeners");
        xmpp.getChatManager().addChatListener(new ChatManagerListener() {
            public void chatCreated(Chat chat, boolean createdLocally) {
                chat.addMessageListener(new MessageListener() {
                    public void processMessage(Chat arg0, Message message) {
                        processIncomingMessage(message);
                    }
                });
            }
        });

        xmpp.getRoster().addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<String> addresses) {

            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {

            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {

            }

            @Override
            public void presenceChanged(Presence presence) {
                if (presence.isAvailable())
                    online.add(presence.getFrom().split("[@-]")[1]);
                else
                    online.remove(presence.getFrom().split("[@-]")[1]);

                if (waiter == null || waiter.getStatus() != AsyncTask.Status.RUNNING) {
                    waiter = new Waiter();
                    waiter.execute();
                }
            }
        });
    }

    /**
     * Process incoming messages.
     *
     * @param message
     */
    private void processIncomingMessage(Message message) {
        // wl.acquire(1000);
        if (message.getBody() != null) {    //TODO someone is writng...
            String user = message.getFrom().split("[@-]")[1];

            ChatDB db = new ChatDB(this);
            Date date = new Date();
            int time = (int) (date.getTime() / 1000);
            Bundle _idAndName = db.MessageAdder(user, message.getBody(), time);
            db.close();

            if (!user.equals(chattingNow))
                downloadPhotoAndShowNotification(_idAndName, message.getBody());
            else {
                Log.i(TAG, "refresh after message!");
                sendTypeBroadcast(6);
                sendTypeBroadcast(5);   //refresh MessageThread
            }

            sendTypeBroadcast(2);
        } else {
            String user = message.getFrom().split("[@-]")[1];
            String xml = message.toXML();
            Document doc;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            try {
                InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.parse(is);
                NodeList nl = doc.getElementsByTagName("message");
                Element e = (Element) nl.item(0);
                NodeList comp = e.getElementsByTagName("composing");
                if (user.equals(chattingNow))
                    if (comp.getLength() == 0) {
                        //nie istnieje - przeslal "active"
                        sendTypeBroadcast(6);
                    } else {
                        //istnieje - przeslal composing
                        sendTypeBroadcast(7);
                    }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadPhotoAndShowNotification(final Bundle _idAndName, final String body) {
        Picasso.with(getApplicationContext())
                .load(_idAndName.getString("photo"))
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                        Log.i(TAG, "Bitmap loaded " + loadedFrom.toString());
                        showNotification(_idAndName, body, bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Drawable drawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable drawable) {

                    }
                });
    }

    private void showNotification(Bundle _idAndName, String body, Bitmap bitmap) {
        Log.i(TAG, "Notification start");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.icon)
                .setContentTitle(_idAndName.getString("name")).setContentText(body).setContentInfo(Integer.toString(_idAndName.getInt("nr")));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (bitmap != null)
            mBuilder.setLargeIcon(bitmap);

        if (_idAndName.getInt("nr") == 1 || _idAndName.getBoolean("start", false)) {
            Uri alarmSound = Uri.parse(preferences.getString("notifications_ringtone", "content://settings/system/notification_sound"));
            mBuilder.setSound(alarmSound);
            // vibration
            if (preferences.getBoolean("notifications_vibrate", true)) {
                long[] vibrate = {0, 100, 200, 300}; // TODO customized
                // vibration PRO
                mBuilder.setVibrate(vibrate);
            }
        }

        mBuilder.setAutoCancel(true);

        Intent open = new Intent(this, Starter.class);
        open.putExtra("go", _idAndName);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, open, 0);
        mBuilder.setContentIntent(pendingOpen);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body).setSummaryText("Messages"));

        mNotificationManager.notify(_idAndName.getInt("id"), mBuilder.build());
        // :D

        sendTypeBroadcast(2);
    }

    private class ConnectionEstablishing extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (!xmpp.isConnected())
                    xmpp.connect();

                Session s = Session.getActiveSession();
                if (s == null) {
                    s = new Session.Builder(getBaseContext()).setApplicationId("159414930918189").build();
                    s.openForRead(null);
                }

                String aT = s.getAccessToken();

                if (xmpp.isConnected() && !xmpp.isAuthenticated())
                    xmpp.login("159414930918189", aT);
                else {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (xmpp.isConnected() && !xmpp.isAuthenticated())
                        xmpp.login("159414930918189", aT);

                    try {
                        Thread.sleep(200); // for stability
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    sendTypeBroadcast(0);

                }

            } catch (XMPPException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            // check for not sent messages last time
            startProcessQueue();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Chat service starty");

        SmackAndroid.init(this);
        try {
            chatConfigAndStart();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        checkForNotReaded();

        TimerTask mainTask = new TimerTask() {
            public void run() {
                wl.acquire(100);
            }
        };

        updatingTimer.scheduleAtFixedRate(mainTask, 1000, 60 * 1000);
        // TODO wakelock or not wakelock? That's THE question.
        return START_STICKY; // restart after kill
    }

    private void checkForNotReaded() {
//todo
    }

    @Override
    public void onDestroy() {
        updatingTimer.cancel();

        xmppDisconnect();// works only when connection is still on
        if (config != null)
            config.setReconnectionAllowed(false);
        Log.i(TAG, "Chat service stop");

        service_state = false;

        if (wl.isHeld())
            wl.release();
    }

    private void xmppDisconnect() {
        if (xmpp != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    xmpp.disconnect();
                    return null;
                }
            }.execute();
        }
    }

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ChatService getService() {
            return ChatService.this;
        }
    }

    /**
     * Update Roster, it should be faster in AsyncTask.
     */
    public void rosterFirstStart() {
        final Context ctx = this;

//        new AsyncTask<Void, Void, Void>() {
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                if (xmpp != null) {
//                    Roster roster = xmpp.getRoster();
//                    Collection<RosterEntry> entries = roster.getEntries();
//
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    System.out.println(entries.size() + " buddy(ies):");
//                    ChatDB db = new ChatDB(ctx);
//
//                    for (RosterEntry entry : entries) {
//                        String user = entry.getUser().split("[@-]")[1];
//                        String name = entry.getName();
//
//                        if (db.checkToAdd(user, name)) {
//                            File avatar = new File(getFilesDir(), user);
//                            if (avatar.exists()) continue;
//                            VCard vCard = new VCard();
//
//                            try {
//                                vCard.load(xmpp, entry.getUser());
//                                byte[] avatarBytes = vCard.getAvatar();
//                                if (avatarBytes == null) continue;
//
//                                FileOutputStream fos = new FileOutputStream(avatar.getPath());
//                                fos.write(avatarBytes);
//                                fos.close();
//                                Log.i(TAG, "Successfully saved avatar");
//                            } catch (XMPPException e) {
//                                Log.e(TAG, "Error in vCard");
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//
//
//                        }
//                    }
//                    db.close();
//
//                    sendTypeBroadcast(1);
//                    sendTypeBroadcast(2);
//                }
//                return null;
//            }
//        }.execute();
    }

    /**
     * Send broadcast to STARTER.class Types: 0 - connected to FB, 1 - after
     * roster, 2 - new notification, 5 - check thread messages
     *
     * @param type
     */
    private void sendTypeBroadcast(int type) {
        Intent intent = new Intent("com.kruczjak.notif");
        intent.putExtra("t", type);
        sendBroadcast(intent);
    }

    private int createChat(String fbid) {
        if (xmpp == null || fbid == null || !xmpp.isAuthenticated() || !xmpp.isConnected()) {
            Log.w(TAG, "ERROR not auth");
            return 2;
        }

        ChatManager chatmanager = xmpp.getChatManager();

        chatThread = chatmanager.createChat("-" + fbid + "@chat.facebook.com", mess);
        Log.i(TAG, "chat created");
        return 0;
    }

    /**
     * @param message
     * @return
     */
    private int sendMessageChat(Message message) {
        if (chatThread == null || !xmpp.isAuthenticated() || !xmpp.isConnected() || message == null) {
            Log.w(TAG, "ERROR not auth");
            return 2;
        }

        try {
            chatThread.sendMessage(message);
        } catch (XMPPException e) {
            Log.w(TAG, "WTF ERROR");
            return 2;
        }
        Log.i(TAG, "message send");
        return 0;
    }

    /**
     * Removes listeners and clear chatThread
     */
    private void deleteChat() {
        if (chatThread == null)
            return;

        chatThread.removeMessageListener(mess);
        chatThread = null;
    }

    MessageListener mess = new MessageListener() {

        @Override
        public void processMessage(Chat chat, Message message) {
        }

    };

    /**
     * ekhm, need something to prevent from spamming TODO need to update time of
     * message after delivery
     */
    public void startProcessQueue() {
        Log.i(TAG, "Starting checking queue");
        ChatDB db = new ChatDB(this);
        Cursor c = db.getMessagesQueue();
        if (c.moveToFirst()) {
            processMessagesAsync(c, db);
        }
    }

    /**
     * Message queue processing core AsyncTask
     *
     * @param c  cursor with messages
     * @param db database to save errors
     */
    private void processMessagesAsync(final Cursor c, final ChatDB db) {
        new AsyncTask<Void, Cursor, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int i;
                for (i = 0; i < c.getCount(); i++) {
                    Log.i(TAG, "Creating chat with " + c.getString(1));
                    if (createChat(c.getString(1)) != 0) {
                        // create new thread and attempt to reconnect based on
                        // reconnectionmanager from SMACK
                        // check as with error
                        publishProgress(c, 1);
                        break;
                    }
                    Date date = new Date();
                    int time = (int) (date.getTime() / 1000 + 1);
                    publishProgress(c, 0, time);
                    // TODO error handling!
                    Message message = new Message();
                    message.setBody(c.getString(2));

                    sendMessageChat(message);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // nothing
                    }
                    deleteChat();
                    c.moveToNext();
                }
                return i;
            }

            private void publishProgress(Cursor c, int i) {
                publishProgress(c, i, 0);
            }

            private void publishProgress(Cursor c, int i, int time) {
                db.markError(c.getInt(0), c.getString(2), c.getString(3), i, time);
                sendTypeBroadcast(5);
            }

            protected void onPostExecute(Integer i) {
                // check for not sent messages last time
                c.moveToFirst();
                while (i > 0) {
                    db.deleteSendedMessages(c.getString(1), c.getString(3));
                    i--;
                    c.moveToNext();
                }

                db.close();
            }

        }.execute();
    }


    private class Waiter extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            sendTypeBroadcast(2);
            sendTypeBroadcast(1);
            Log.i(TAG, "User change");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void sendTypeBroadcast(int type) {
            Intent intent = new Intent("com.kruczjak.notif");
            intent.putExtra("t", type);
            sendBroadcast(intent);
        }
    }
}

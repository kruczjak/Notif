/**
 *
 */
package com.kruczjak.notif.frags;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.kruczjak.notif.ChatDB;
import com.kruczjak.notif.R;
import com.kruczjak.notif.Starter;
import com.kruczjak.notif.Starter.MessageThreadCommunicator;
import com.kruczjak.notif.views.MessagesAdapter;

import java.util.Date;

/**
 * Opens view for see the chat thread.
 *
 * @author Jakub Kruczek
 */
public class MessageThread extends SherlockFragment implements MessageThreadCommunicator {
    protected static final String TAG = "ChatThread";
    private ListView lv;
    private Bundle args;
    private EditText messageText;
    private MessagesAdapter mA;
    private Button send;
    private TextView composing;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chat_thread, container, false);
        if (view != null) {
            lv = (ListView) view.findViewById(R.id.listView1);
            send = (Button) view.findViewById(R.id.button1);
            messageText = (EditText) view.findViewById(R.id.editText1);
            composing = (TextView) view.findViewById(R.id.textView);
        }

        args = this.getArguments();

        NotificationManager mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(args.getInt("id"));

        ChatDB db = new ChatDB(getActivity());
        mA = new MessagesAdapter(getActivity(), db.getMessages(Integer.toString(args.getInt("id"))), false);
        lv.setAdapter(mA);
        db.close();

        send.setOnClickListener(new OnClickListener() {
            // send a message
            @Override
            public void onClick(View arg0) {
                send();
            }
        });

        messageText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    if (!event.isShiftPressed()) {
                        if (actionId == EditorInfo.IME_ACTION_SEND || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            send();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        return view;
    }

    /**
     * Send messages async! Add them to queue and then send I think that creating and closing chat gonna be fast.
     */
    protected void send() {
        String messageTextString = messageText.getText().toString();
        if (!messageTextString.trim().equals("")) {
            messageText.setText("");
            scrollMyListViewToBottom();

            Date date = new Date();
            long time = date.getTime() + 1000;
            int timeInt = (int) (date.getTime() / 1000 + 1);
            //first save to message view
            ChatDB db = new ChatDB(getActivity());
            db.addMessageOnThread(messageTextString, Integer.toString(args.getInt("id")), 0, 1, timeInt);
            //now save to queue
            db.addMessageToQueue(args.getInt("id"), args.getString("fbid"), messageTextString, time);
            db.close();
            //now fire the queue
            ((Starter) getActivity()).startProcessQueue(args.getInt("id"), messageTextString, Long.toString(time));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeActionBar();
        updateDatabase();
        ((Starter) getActivity()).setChattingNow("");
    }

    private void closeActionBar() {
        ActionBar aB = getSherlockActivity().getSupportActionBar();
        ((Starter) getActivity()).navigationMode();
        aB.setTitle("Notif");
        aB.setHomeButtonEnabled(false);
        aB.setDisplayHomeAsUpEnabled(false);
        aB.setIcon(R.drawable.icon);
    }

    @Override
    public void onResume() {
        super.onResume();
        initActionBar();
        refresh();
        ((Starter) getActivity()).setChattingNow(args.getString("fbid"));
    }

    private void updateDatabase() {
        ChatDB db = new ChatDB(getActivity());
        Cursor c = mA.getCursor();
        if (c != null) {
            if (c.moveToLast())
                db.updateAfterChat(args.getInt("id"), c.getString(1), c.getInt(3), c.getInt(4));
        }
        db.close();
        ((Starter) getActivity()).messageOverviewCommunicator.refreshListView();
    }

    private void initActionBar() {
        final ActionBar aB = getSherlockActivity().getSupportActionBar();
        aB.setTitle(args.getString("name"));

//        String avatarPath = getActivity().getFilesDir() + "/" + args.getString("fbid");
//        File file = new File(getActivity().getFilesDir(), args.getString("fbid"));
//        if (file.exists()) {
//            Bitmap bmp = BitmapFactory.decodeFile(avatarPath);
//            Resources res = getResources();
//            BitmapDrawable icon = new BitmapDrawable(res, bmp);
//            aB.setIcon(icon);
//        } else
//            aB.setIcon(R.drawable.avatar);
//        Picasso.with(getActivity())
//                .load("http://graph.facebook.com/" + args.getString("fbid") + "/picture?width=100&height=100")
//                .into(new Target() {
//                    @Override
//                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
//                        Log.i(TAG,"Bitmap loaded " + loadedFrom.toString());
//                        Resources res = getResources();
//                        BitmapDrawable icon = new BitmapDrawable(res, bitmap);
//                        aB.setIcon(icon);
//                    }
//
//                    @Override
//                    public void onBitmapFailed(Drawable drawable) {
//
//                    }
//
//                    @Override
//                    public void onPrepareLoad(Drawable drawable) {
//
//                    }
//                });

        if (aB.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS)
            aB.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
    }

    private void scrollMyListViewToBottom() {
        lv.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                lv.setSelection(mA.getCount() - 1);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Starter) getActivity()).messageThreadCommunicator = this;
    }

    @Override
    public void refreshListView() {
        refresh();
    }

    @Override
    public void sendStatus(int i) {
        if (i == 6) composing.setVisibility(View.INVISIBLE);
        else if (i == 7) composing.setVisibility(View.VISIBLE);
    }

    public void refresh() {
        ChatDB db = new ChatDB(getActivity());
        mA.changeCursor(db.getMessages(Integer.toString(args.getInt("id"))));
        db.close();
        scrollMyListViewToBottom();
    }
}

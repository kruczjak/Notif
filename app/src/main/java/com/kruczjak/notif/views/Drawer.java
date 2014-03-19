package com.kruczjak.notif.views;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.kruczjak.notif.ChatDB;
import com.kruczjak.notif.R;
import com.kruczjak.notif.Starter;

/**
 * Created by kruczjak on 19.03.14.
 */
public class Drawer extends DrawerLayout {
    public static final String TAG = "Drawer";
    private ListView mDrawerList;
    private boolean closingAfterChoose;
    private ContactsAdapter contactsAdapter;
    private EditText editText;
    private ChatDB chatDB;

    public Drawer(Context context) {
        super(context);
    }

    public Drawer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Drawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private ActionBarDrawerToggle getMyDrawerListener() {
        final CharSequence mDrawerTitle = "Friends"; //TODO strings
        final CharSequence mTitle = getStarted().getTitle();

        return new ActionBarDrawerToggle(getStarted(), this, R.drawable.icon, R.string.friends, R.string.friends) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                if (closingAfterChoose)
                    closingAfterChoose = false;
                else
                    getStarted().getSupportActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getStarted().getSupportActionBar().setTitle(mDrawerTitle);
            }
        };
    }

    public void init() {
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        setDrawerListener(getMyDrawerListener());

        chatDB = new ChatDB(getContext());
        new Handler().post(new Runnable() { // thread for loading data from db
            @Override
            public void run() {
                contactsAdapter = new ContactsAdapter(getStarted(), chatDB.getFav(), chatDB.getOnlineContacts(getStarted().getOnline()), getStarted().getOnline());
                mDrawerList.setAdapter(contactsAdapter);
                chatDB.close();
            }
        });

        mDrawerList.setOnItemClickListener(getMyOnItemClickListener());
        mDrawerList.setOnCreateContextMenuListener(getMyOnCreateContextMenuListener());
        setEditText();
    }

    private OnCreateContextMenuListener getMyOnCreateContextMenuListener() {
        return new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
                Bundle args = contactsAdapter.getData(info.position);
                contextMenu.setHeaderTitle(args.getString("name"));
                String[] menuItems = getResources().getStringArray(R.array.menu);

                boolean is = chatDB.isFav(String.valueOf(args.getInt("id")));
                chatDB.close();

                if (is)
                    contextMenu.add(Starter.FRAGMENT_GROUP, 0, 0, menuItems[0]);
                else
                    contextMenu.add(Starter.FRAGMENT_GROUP, 0, 0, menuItems[1]);
            }
        };
    }

    private AdapterView.OnItemClickListener getMyOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LinearLayout sc = (LinearLayout) findViewById(R.id.left_drawer_layout);
                closingAfterChoose = true;
                closeDrawer(sc);
                Bundle args = contactsAdapter.getData(i);

                if (args.getBoolean("notable")) {
                    chatDB.createMessageTable(String.valueOf(args.getInt("id")));
                    chatDB.close();
                }

                getStarted().addMessageThreadFragment(args);
            }
        };
    }

    private void setEditText() {
        editText = (EditText) findViewById(R.id.finder);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                searchInBox(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void searchInBox(CharSequence charSequence) {
        if (charSequence.toString().equals("")) {
            mDrawerList.setAdapter(contactsAdapter);
            normalUpdate();
        } else {
            String search = charSequence.toString();
            if (mDrawerList.getAdapter() == contactsAdapter) {
                ContactsSearchAdapter searchAdapter = new ContactsSearchAdapter(getStarted(), chatDB.getSearched(search, getStarted().getOnline()), getStarted().getOnline());
                mDrawerList.setAdapter(searchAdapter);
                chatDB.close();
            } else {
                searchUpdate(search);
            }
        }
    }

    /**
     * Update left friends list ASYNC.
     */
    public void update() {
        if (editText.getText().toString().equals("")) normalUpdate();
        else searchUpdate(editText.getText().toString());
    }

    private void searchUpdate(String search) {
        ContactsSearchAdapter searchAdapter = (ContactsSearchAdapter) mDrawerList.getAdapter();
        searchAdapter.changeCursor(chatDB.getSearched(search, getStarted().getOnline()));
    }

    private void normalUpdate() {
        Log.i(TAG, "drawerUpdateAfterRoster");
        new Handler().post(new Runnable() { // thread for loading data from db
            @Override
            public void run() {
                contactsAdapter.setFavourites(chatDB.getFav());
                contactsAdapter.setOnline(getStarted().getOnline());
                contactsAdapter.setRest(chatDB.getOnlineContacts(getStarted().getOnline()));
                contactsAdapter.notifyDataSetChanged();
                chatDB.close();
            }
        });
    }

    private Starter getStarted() {
        return (Starter) getContext();
    }

    public ContactsAdapter getContactsAdapter() {
        return contactsAdapter;
    }
}

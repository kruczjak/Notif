package com.kruczjak.notif;

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

/**
 * Created by kruczjak on 19.03.14.
 */
public class Drawer extends DrawerLayout {
    public static final String TAG = "Drawer";
    private ListView mDrawerList;
    private Starter starterContext;
    private boolean closingAfterChoose;
    private ContactsAdapter cca;
    private EditText editText;

    public Drawer(Context context, Context ctx) {
        super(context);
    }

    public Drawer(Context context, AttributeSet attrs, Context ctx) {
        super(context, attrs);
    }

    public Drawer(Context context, AttributeSet attrs, int defStyle, Context ctx) {
        super(context, attrs, defStyle);
    }

    private ActionBarDrawerToggle getMyDrawerListener() {
        final CharSequence mDrawerTitle = "Friends"; //TODO strings
        final CharSequence mTitle = starterContext.getTitle();

        return new ActionBarDrawerToggle(starterContext, this, R.drawable.icon, R.string.friends, R.string.friends) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                if (closingAfterChoose)
                    closingAfterChoose = false;
                else
                    starterContext.getSupportActionBar().setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                starterContext.getSupportActionBar().setTitle(mDrawerTitle);
            }
        };

    }


    public void setStarterContext(Starter starterContext) {
        this.starterContext = starterContext;
    }

    public void init() {
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        setDrawerListener(getMyDrawerListener());

        new Handler().post(new Runnable() { // thread for loading data from db
            @Override
            public void run() {
                ChatDB dbcha = new ChatDB(getContext());
                cca = new ContactsAdapter(starterContext, dbcha.getFav(), dbcha.getOnlineContacts(starterContext.getOnline()), starterContext.getOnline());
                mDrawerList.setAdapter(cca);
                dbcha.close();
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
                Bundle args = cca.getData(info.position);
                contextMenu.setHeaderTitle(args.getString("name"));
                String[] menuItems = getResources().getStringArray(R.array.menu);

                ChatDB db = new ChatDB(getContext());
                boolean is = db.isFav(String.valueOf(args.getInt("id")));
                db.close();

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
                Bundle args = cca.getData(i);

                if (args.getBoolean("notable")) {
                    ChatDB db = new ChatDB(getContext());
                    db.createMessageTable(String.valueOf(args.getInt("id")));
                    db.close();
                }

                starterContext.addMessageThreadFragment(args);
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
                if (charSequence.toString().equals("")) {
                    ChatDB dbcha = new ChatDB(getContext());
                    cca = new ContactsAdapter(starterContext, dbcha.getFav(), dbcha.getOnlineContacts(starterContext.getOnline()), starterContext.getOnline());
                    mDrawerList.setAdapter(cca);
                    dbcha.close();
                } else {
                    String search = charSequence.toString();
                    if (mDrawerList.getAdapter() == cca) {
                        ChatDB chatDB = new ChatDB(getContext());
                        ContactsSearchAdapter searchAdapter = new ContactsSearchAdapter(starterContext, chatDB.getSearched(search, starterContext.getOnline()), starterContext.getOnline());
                        mDrawerList.setAdapter(searchAdapter);
                        chatDB.close();
                    } else {
                        ChatDB chatDB = new ChatDB(getContext());
                        ContactsSearchAdapter searchAdapter = (ContactsSearchAdapter) mDrawerList.getAdapter();
                        searchAdapter.changeCursor(chatDB.getSearched(search, starterContext.getOnline()));

                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * Update left friends list ASYNC.
     */
    public void update() {
        Log.i(TAG, "drawerUpdateAfterRoster");
        new Handler().post(new Runnable() { // thread for loading data from db
            @Override
            public void run() {
                ChatDB dbcha = new ChatDB(getContext());
                cca.setFavourites(dbcha.getFav());
                cca.setOnline(starterContext.getOnline());
                cca.setRest(dbcha.getOnlineContacts(starterContext.getOnline()));
                cca.notifyDataSetChanged();
                dbcha.close();
            }
        });
    }
}

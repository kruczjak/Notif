/**
 *
 */
package com.kruczjak.notif;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.kruczjak.notif.Starter.MessageOverviewCommunicator;

import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * @author Jakub
 */
public class MessageOverviewFragment extends SherlockFragment implements OnRefreshListener, MessageOverviewCommunicator, MessageOverviewListView.EndlessListener {

    private static final String TAG = "MessageOverviewFragment";
    private ChatDB Chdb;
    private MessageOverviewAdapter mOA;
    MessageOverviewListView lv;
    public static final int FRAGMENT_GROUP = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.message_overview, container, false);
        lv = (MessageOverviewListView) view.findViewById(R.id.listView1);

        Chdb = new ChatDB(getActivity());
        List<String> online = ((Starter) getActivity()).getOnline();
        mOA = new MessageOverviewAdapter(getActivity(), Chdb.getAllLastFriends(), false, online);
        lv.setAdapter(mOA);
        Chdb.close();

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                                    long id) {
                Cursor c = mOA.getCursor();
                c.moveToPosition(position);

                Bundle args = new Bundle();
                args.putInt("id", c.getInt(0));
                args.putString("fbid", c.getString(1));
                args.putString("name", c.getString(2));


                MessageThread fragment = new MessageThread();
                fragment.setArguments(args);
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                transaction.add(R.id.layout_main, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

            }
        });

        lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                Cursor c = mOA.getCursor();
                c.moveToPosition(info.position);
                menu.setHeaderTitle(c.getString(2));
                String[] menuItems = getResources().getStringArray(R.array.menu);

                ChatDB db = new ChatDB(getActivity());
                boolean is = db.isFav(c.getString(0));
                db.close();

                if (is)
                    menu.add(FRAGMENT_GROUP, 0, 0, menuItems[0]);
                else
                    menu.add(FRAGMENT_GROUP, 0, 0, menuItems[1]);
            }
        });
        lv.listener = this;
        return view;
    }

    @Override
    public void onRefreshStarted(View view) {
        refreshListView();
    }

    @Override
    public void refreshListView() {
        Log.i(TAG, "GOT IT!");
        Chdb = new ChatDB(getActivity());
        List<String> online = ((Starter) getActivity()).getOnline();
        mOA.changeCursor(Chdb.getAllLastFriends(), online);
        Chdb.close();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((Starter) getActivity()).messageOverviewCommunicator = this;
    }

    public boolean onMyContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        Log.d(TAG, "Selected " + Integer.toString(menuItemIndex));

        Cursor c = mOA.getCursor();
        c.moveToPosition(info.position);
        String id = c.getString(0);

        ChatDB db = new ChatDB(getActivity());
        db.addOrDeleteFav(id);
        db.close();
        ((Starter) getActivity()).updateMDrawerLayout();
        Toast.makeText(getActivity(), "Friend added or deleted", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void loadData() {
        Log.i("LOAD", "LOAD");
        List<String> online = ((Starter) getActivity()).getOnline();
//        mOA.changeCursor();
        lv.removeFooter();
    }
}

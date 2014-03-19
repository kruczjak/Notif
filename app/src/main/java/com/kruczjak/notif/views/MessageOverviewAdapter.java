/**
 *
 */
package com.kruczjak.notif.views;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kruczjak.notif.FunctionsMain;
import com.kruczjak.notif.R;

import java.util.Date;
import java.util.List;

/**
 * @author Jakub
 */
public class MessageOverviewAdapter extends CursorAdapter {
    private static final String TAG = "MessageOverviewAdapter";
    private FunctionsMain fuck = new FunctionsMain();
    private List<String> online;

    public MessageOverviewAdapter(Context context, Cursor c, boolean autoRequery, List<String> online) {
        super(context, c, autoRequery);
        this.online = online;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView nameText = (TextView) view.findViewById(R.id.nameText);
        nameText.setText(cursor.getString(2));

        TextView last_message = (TextView) view.findViewById(R.id.last_message);
        last_message.setText(cursor.getString(4));

        if (cursor.getInt(5) == 0) {
            last_message.setTextColor(Color.rgb(255, 102, 0));
        } else {
            last_message.setTextColor(Color.DKGRAY);
        }

        TextView last_date = (TextView) view.findViewById(R.id.last_date);
        Date date = new Date(Long.parseLong(cursor.getString(3)) * 1000);
        last_date.setText(fuck.date_process(date));

        TextView nonr = (TextView) view.findViewById(R.id.number_of_not_readed);
        if (cursor.getInt(6) == 0)
            nonr.setVisibility(View.GONE);
        else {
            nonr.setText(Integer.toString(cursor.getInt(6)));
            nonr.setVisibility(View.VISIBLE);
        }

        Avatar avatar = (Avatar) view.findViewById(R.id.avatar);
        avatar.showAvatar(cursor.getString(7), cursor.getString(1), 0);

        ImageView onlineIndicator = (ImageView) view.findViewById(R.id.online);
        if (online != null && online.contains(cursor.getString(1))) {
            onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            onlineIndicator.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);    //take inflater
        View v = inflater.inflate(R.layout.message_adapter_layout, parent, false);    //inflate view

        return v;    //return it
    }

    public void changeCursor(Cursor cursor, List<String> online) {
        super.changeCursor(cursor);
        this.online = online;
    }
}

package com.kruczjak.notif;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;

import java.util.Date;

/*
 * 0-id
 * 1-updated_time
 * 2-title_text
 * 3-href
 * 4-is_unread
 * 5-type
 */
public class CursorNotifAdapter extends CursorAdapter {
    private FunctionsMain fuck = new FunctionsMain();

    public CursorNotifAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public CursorNotifAdapter(Context context, Cursor c, boolean autoRequery, ActionBar aB) {
        super(context, c, autoRequery);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView title = (TextView) view.findViewById(R.id.title_text);
        title.setText(cursor.getString(2));

        TextView updated = (TextView) view.findViewById(R.id.updated_time);
        Date date = new Date(Long.parseLong(cursor.getString(1)) * 1000);
        updated.setText(fuck.date_process(date));

        if (cursor.getInt(4) == 1) {
            title.setTypeface(null, Typeface.BOLD);
            updated.setTypeface(null, Typeface.BOLD);
        } else {
            title.setTypeface(null, Typeface.NORMAL);
            updated.setTypeface(null, Typeface.NORMAL);
        }

        TextView type = (TextView) view.findViewById(R.id.type);
        type.setText(fuck.resolveObject_type(cursor.getString(5), context));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);    //take inflater
        View v = inflater.inflate(R.layout.adapter_list_view, parent, false);    //inflate view

        return v;    //return it
    }
}

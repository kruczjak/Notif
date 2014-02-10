package com.kruczjak.notif;

import android.app.Activity;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by kruczjak on 10.01.14.
 */
public class ContactsSearchAdapter extends BaseAdapter {
    private Cursor cursor;
    private Activity ctx;
    private List<String> online;

    public ContactsSearchAdapter(Activity ctx, Cursor cursor, List<String> online) {
        this.ctx = ctx;
        this.cursor = cursor;
        this.online = online;
    }

    public void changeCursor(Cursor searched) {
        this.cursor = searched;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        public TextView name;
        public Avatar avatar;
        public ImageView on;
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        View convertView = view;
        if (convertView == null) {
            LayoutInflater inflater = ctx.getLayoutInflater();
            ViewHolder holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.adapter_contacts, null);
            holder.avatar = (Avatar) convertView.findViewById(R.id.avatar);
            holder.name = (TextView) convertView.findViewById(R.id.nameText);
            holder.on = (ImageView) convertView.findViewById(R.id.online);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        cursor.moveToPosition(position);
        holder.avatar.showAvatar(cursor.getString(7), cursor.getString(1), 1);
        holder.name.setText(cursor.getString(2));
        if (online != null && online.contains(cursor.getString(1))) {
            holder.on.setVisibility(View.VISIBLE);
        } else {
            holder.on.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }
}

package com.kruczjak.notif;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
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
public class ContactsAdapter extends BaseAdapter {
    private Activity ctx;
    private Cursor favourites;
    private List<String> online;
    private Cursor rest;
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_MAX_NUMBER = 2;

    public void setRest(Cursor rest) {
        this.rest = rest;
    }

    public void setOnline(List<String> online) {
        this.online = online;
    }

    public void setFavourites(Cursor favourites) {
        this.favourites = favourites;
    }

    public ContactsAdapter(Activity ctx, Cursor favourites, Cursor rest, List<String> online) {
        this.ctx = ctx;
        this.favourites = favourites;
        this.online = online;
        this.rest = rest;
    }

    @Override
    public int getCount() {
        int count = 1;

        if (favourites != null)
            count += favourites.getCount();
        if (rest != null)
            count += rest.getCount();

        return count;
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
        int type = getItemViewType(position);
        if (convertView == null) {
            LayoutInflater inflater = ctx.getLayoutInflater();
            ViewHolder holder = new ViewHolder();
            if (type == 0) {
                convertView = inflater.inflate(R.layout.adapter_contacts, null);
                holder.avatar = (Avatar) convertView.findViewById(R.id.avatar);
                holder.name = (TextView) convertView.findViewById(R.id.nameText);
                holder.on = (ImageView) convertView.findViewById(R.id.online);
            } else if (type == 1) {
                convertView = inflater.inflate(R.layout.line_row, null);
            }

            convertView.setTag(holder);
        }

        if (type == 0) {
            ViewHolder holder = (ViewHolder) convertView.getTag();
            getStringsBasedOnPosition(position, holder);
        }

        return convertView;
    }

    private void getStringsBasedOnPosition(int position, ViewHolder holder) {
        String name, fbID, photoLink;
        name = fbID = photoLink = "";
        if (favourites != null && position < favourites.getCount()) {

            favourites.moveToPosition(position);
            name = favourites.getString(2);
            fbID = favourites.getString(1);
            photoLink = favourites.getString(7);
            if (online != null && online.contains(fbID)) {
                holder.on.setVisibility(View.VISIBLE);
            } else {
                holder.on.setVisibility(View.INVISIBLE);
            }

        } else if (rest != null) {
            int pos;

            if (favourites != null) {
                pos = position - favourites.getCount();
            } else {
                pos = position;
            }
            pos--;//for line_row
            rest.moveToPosition(pos);
            name = rest.getString(2);
            fbID = rest.getString(1);
            photoLink = rest.getString(7);
            holder.on.setVisibility(View.VISIBLE);
        }

        holder.name.setText(name);
        holder.avatar.showAvatar(photoLink, fbID, 1);
    }

    public Bundle getData(int position) {
        Bundle args = new Bundle();
        if (favourites != null && position < favourites.getCount()) {
            favourites.moveToPosition(position);
            args.putInt("id", favourites.getInt(0));
            args.putString("fbid", favourites.getString(1));
            args.putString("name", favourites.getString(2));
            if (favourites.isNull(3))
                args.putBoolean("notable", true);
            else
                args.putBoolean("notable", false);

        } else if (rest != null) {
            int pos;
            if (favourites != null) {
                pos = position - favourites.getCount();
            } else {
                pos = position;
            }
            pos--;//for line_row
            rest.moveToPosition(pos);
            args.putInt("id", rest.getInt(0));
            args.putString("fbid", rest.getString(1));
            args.putString("name", rest.getString(2));
            if (rest.isNull(3))
                args.putBoolean("notable", true);
            else
                args.putBoolean("notable", false);
        }

        return args;
    }

    static class ViewHolder {
        public TextView name;
        public Avatar avatar;
        public ImageView on;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_NUMBER;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == favourites.getCount()) ? TYPE_SEPARATOR : TYPE_NORMAL;
    }
}

/**
 *
 */
package com.kruczjak.notif;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

/**
 * @author Jakub
 */
public class MessagesAdapter extends CursorAdapter {

    public MessagesAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);

    }

    @Override
    public void bindView(View v, Context ctx, Cursor c) {
        TextView messageBody = (TextView) v.findViewById(R.id.textView1);
        ImageView error = (ImageView) v.findViewById(R.id.error);
        messageBody.setText(c.getString(1));

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(5, 5, 5, 5);


        RelativeLayout.LayoutParams err = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        if (c.getInt(3) == 0) {
            messageBody.setBackgroundResource(R.drawable.background_incoming);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            messageBody.setLayoutParams(lp);

            err.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            error.setLayoutParams(err);
        } else {
            messageBody.setBackgroundResource(R.drawable.background_outgoing);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            messageBody.setLayoutParams(lp);

            err.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            error.setLayoutParams(err);
        }

        if (c.getInt(5) == 0 || c.isNull(5)) {
            error.setVisibility(View.INVISIBLE);
        } else {
            error.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context ctx, Cursor c, ViewGroup vg) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE); // take
        // inflater
        View v = inflater.inflate(R.layout.message, vg, false); // inflate view

        return v;
    }
}

package com.kruczjak.notif.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class MessageListView extends ListView {
    public MessageListView(Context context) {
        super(context);
    }

    public MessageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        setSelection(getCount() - 1);
    }

}

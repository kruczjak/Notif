package com.kruczjak.notif.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.kruczjak.notif.R;

/**
 * Created by kruczjak on 12.01.14.
 */
public class MessageOverviewListView extends ListView implements OnScrollListener {
    private boolean isLoading = false;
    public EndlessListener listener;
    private View footer;

    public MessageOverviewListView(Context context) {
        super(context);
//        setOnScrollListener(this);
    }

    public MessageOverviewListView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        setOnScrollListener(this);
    }

    public MessageOverviewListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        setOnScrollListener(this);
    }

    public static interface EndlessListener {
        public void loadData();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (getAdapter() == null)
            return;
        if (getAdapter().getCount() == 0)
            return;

        int l = visibleItemCount + firstVisibleItem;
        if (l >= totalItemCount && !isLoading) {
            Log.i("deb", "end");
            LayoutInflater inflater = (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            footer = inflater.inflate(R.layout.loading_row, null);
            this.addFooterView(footer);
            isLoading = true;
            listener.loadData();
        }
    }

    /**
     * Hide loading row.
     */
    public void removeFooter() {
        removeFooterView(footer);
        isLoading = false;
    }
}

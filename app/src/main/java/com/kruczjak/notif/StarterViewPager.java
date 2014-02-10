package com.kruczjak.notif;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by kruczjak on 08.01.14.
 */
public class StarterViewPager extends ViewPager {
    boolean disableScrolling = false;

    public StarterViewPager(Context context) {
        super(context);
    }

    public StarterViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDisableScrolling(boolean disableScrolling) {
        this.disableScrolling = disableScrolling;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!this.disableScrolling) {
            return super.onInterceptTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.disableScrolling) {
            return super.onTouchEvent(ev);
        }

        return false;
    }
}

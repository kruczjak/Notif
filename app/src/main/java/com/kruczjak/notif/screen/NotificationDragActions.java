package com.kruczjak.notif.screen;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import static java.lang.Math.abs;

/**
 * Created by kruczjak on 19.03.14.
 */
public class NotificationDragActions implements View.OnTouchListener {
    private int oX;
    private int oY;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private Context ctx;

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    public NotificationDragActions(WindowManager windowManager, WindowManager.LayoutParams params, Context ctx) {
        this.windowManager = windowManager;
        this.params = params;
        this.ctx = ctx;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oX = initialX = params.x;
                oY = initialY = params.y;
                initialTouchX = motionEvent.getRawX();
                initialTouchY = motionEvent.getRawY();
                return true;
            case MotionEvent.ACTION_UP:
                checkTouch();
                return true;
            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (motionEvent.getRawX() - initialTouchX);
                params.y = initialY + (int) (motionEvent.getRawY() - initialTouchY);
                windowManager.updateViewLayout(view, params);
                return true;
        }
        return false;
    }

    private void checkTouch() {
        if (abs(params.x - oX) < 10 && abs(params.y - oY) < 10) {
            ScreenNotification.getInstance(ctx).fullMessages();
        }
    }
}

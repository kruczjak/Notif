package com.kruczjak.notif.screen;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.kruczjak.notif.views.Avatar;

/**
 * Created by kruczjak on 19.03.14.
 */
public class NotificationDragActions implements View.OnTouchListener {
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private WindowManager windowManager;
    private Avatar avatar;
    private WindowManager.LayoutParams params;

    public NotificationDragActions(WindowManager windowManager, Avatar avatar, WindowManager.LayoutParams params) {
        this.windowManager = windowManager;
        this.avatar = avatar;
        this.params = params;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = motionEvent.getRawX();
                initialTouchY = motionEvent.getRawY();
                return true;
            case MotionEvent.ACTION_UP:
                return true;
            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (motionEvent.getRawX() - initialTouchX);
                params.y = initialY + (int) (motionEvent.getRawY() - initialTouchY);
                windowManager.updateViewLayout(avatar, params);
                return true;
        }
        return false;
    }
}

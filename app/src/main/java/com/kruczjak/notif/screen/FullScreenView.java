package com.kruczjak.notif.screen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

/**
 * Created by kruczjak on 20.03.14.
 */
public class FullScreenView extends RelativeLayout {


    public FullScreenView(Context context) {
        super(context);
    }

    public FullScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullScreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            ScreenNotification.getInstance(getContext()).onBackKey();
        }
        return super.dispatchKeyEvent(event);
    }
}

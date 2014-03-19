package com.kruczjak.notif.screen;

import android.app.Service;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;

import com.kruczjak.notif.views.Avatar;

/**
 * Created by kruczjak on 19.03.14.
 */
public class ScreenNotification {
    private Context ctx;
    private WindowManager windowManager;
    private Avatar avatar;
    private static ScreenNotification screenNotification;
    private WindowManager.LayoutParams params;
    private String photoLink;

    private ScreenNotification(Context context) {
        this.ctx = context;
    }

    public static ScreenNotification getInstance(Context context) {
        if (screenNotification == null) {
            screenNotification = new ScreenNotification(context);
        }
        return screenNotification;
    }

    public void startNotification(String photoLink, String fbID) {
        if (avatar == null) {
            this.photoLink = photoLink;
            setAndShowAvatar(photoLink, fbID);
        } else {
            if (this.photoLink.equals(photoLink)) return;
            avatar.showAvatar(photoLink, fbID, 3);
            windowManager.updateViewLayout(avatar, params);
        }
    }


    public void onDestroy() {
        if (avatar != null) {
            windowManager.removeView(avatar);
            avatar = null;
        }
    }

    private void setAndShowAvatar(String photoLink, String fbID) {
        windowManager = (WindowManager) ctx.getSystemService(Service.WINDOW_SERVICE);
        avatar = new Avatar(ctx);
        avatar.showAvatar(photoLink, fbID, 3);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 20;
        params.y = 100;

        windowManager.addView(avatar, params);
        avatar.setOnTouchListener(new NotificationDragActions(windowManager, avatar, params));
    }
}

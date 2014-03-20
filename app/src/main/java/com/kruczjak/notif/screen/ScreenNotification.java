package com.kruczjak.notif.screen;

import android.app.Service;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.kruczjak.notif.R;
import com.kruczjak.notif.views.Avatar;

/**
 * Created by kruczjak on 19.03.14.
 */
public class ScreenNotification {
    private Context ctx;
    private WindowManager windowManager;
    private Avatar avatar;
    private View view;
    private static ScreenNotification screenNotification;
    private WindowManager.LayoutParams params;
    private String photoLink;
    private TextView number;
    private ImageView online;

    private ScreenNotification(Context context) {
        this.ctx = context;
    }

    public static ScreenNotification getInstance(Context context) {
        if (screenNotification == null) {
            screenNotification = new ScreenNotification(context);
        }
        return screenNotification;
    }

    public void startNotification(Bundle data) {
        if (view == null) {
            this.photoLink = data.getString("photo");
            setAndShowAvatar(data);
        } else {
            update(data);

        }
    }

    private void update(Bundle data) {
        setFromData(data);
        if (this.photoLink.equals(data.getString("photo"))) return;
        avatar.showAvatar(photoLink, data.getString("fbid"), 3);
        windowManager.updateViewLayout(view, params);
    }

    private void setFromData(Bundle data) {
        if (data.getInt("nr") == 0)
            number.setVisibility(View.GONE);
        else {
            number.setText(Integer.toString(data.getInt("nr")));
            number.setVisibility(View.VISIBLE);
        }

        if (data.getBoolean("presence"))
            online.setVisibility(View.VISIBLE);
        else
            online.setVisibility(View.INVISIBLE);
    }


    public void onDestroy() {
        if (view != null) {
            windowManager.removeView(view);
            view = null;
            avatar = null;
            online = null;
            number = null;
        }
    }

    private void setAndShowAvatar(Bundle data) {
        windowManager = (WindowManager) ctx.getSystemService(Service.WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.notification_main, null);
        avatar = (Avatar) view.findViewById(R.id.avatar);
        number = (TextView) view.findViewById(R.id.number_of_not_readed);
        online = (ImageView) view.findViewById(R.id.online);

        avatar.showAvatar(photoLink, data.getString("fbid"), 3);
        setFromData(data);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 20;
        params.y = 100;

        windowManager.addView(view, params);
        view.setOnTouchListener(new NotificationDragActions(windowManager, params, ctx));
    }

    public void changePresence(boolean present) {
        if (present)
            online.setVisibility(View.VISIBLE);
        else
            online.setVisibility(View.INVISIBLE);
    }

    public void fullMessages() {
        WindowManager.LayoutParams fullParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.chat_thread, null);

        windowManager.addView(view, fullParams);
    }
}

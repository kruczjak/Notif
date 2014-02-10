package com.kruczjak.notif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by krucz_000 on 05.01.14.
 */
public class Avatar extends ImageView implements Target {
    protected static final String TAG = "Avatar";
    boolean tested = false;
    boolean testing = false;
    private String fbID;
    private int from;

    public Avatar(Context context) {
        super(context);
    }

    public Avatar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Avatar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTested(boolean tested) {
        this.tested = tested;
    }

    public void setTesting(boolean testing) {
        this.testing = testing;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        AvatarDrawable.setBitmap(this, getContext(), bitmap, loadedFrom, false, false);
    }

    @Override
    public void onBitmapFailed(Drawable drawable) {
        //todo refresh link!!!
        Log.e(TAG, "failed");
        if (!tested && isInternetAccess() && !testing) {
            Log.i(TAG, "trying");
            testing = true;
            new FBFriends().getNewPhotoLinkAndUpdatePhoto(this, fbID, from);
        }
    }

    /**
     * Check if internet is running
     *
     * @return isInternet
     */
    private boolean isInternetAccess() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetInfo != null && activeNetInfo.isConnectedOrConnecting());
    }

    @Override
    public void onPrepareLoad(Drawable drawable) {
        setImageResource(R.drawable.avatar100x100);
    }

    /**
     * Starts showing avatar in this Avatar field.
     *
     * @param photoLink link to photo (can be null)
     * @param fbID      facebook id
     * @param from      0 from overview, 1 from contacts
     */
    public void showAvatar(String photoLink, String fbID, int from) {
        this.fbID = fbID;
        this.from = from;
        if (testing) return;
        if (photoLink == null) {
            testing = true;
            Log.i(TAG, "I see null! in " + fbID);
            setImageResource(R.drawable.avatar100x100);
            new FBFriends().getNewPhotoLinkAndUpdatePhoto(this, fbID, from);
        } else {
            Picasso.with(getContext()).load(photoLink)
                    .resize(100, 100)
                    .into((Target) this);
        }
    }
}

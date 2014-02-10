package com.kruczjak.notif;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import static com.squareup.picasso.Picasso.LoadedFrom.MEMORY;

/**
 * Created by krucz_000 on 05.01.14.
 */
class AvatarDrawable extends Drawable {
    private static final float FADE_DURATION = 200f; //ms

    /**
     * Create or update the drawable on the target {@link android.widget.ImageView} to display the supplied bitmap
     * image.
     */
    static void setBitmap(ImageView target, Context context, Bitmap bitmap,
                          Picasso.LoadedFrom loadedFrom, boolean noFade, boolean debugging) {
        Drawable placeholder = target.getDrawable();
        if (placeholder instanceof AnimationDrawable) {
            ((AnimationDrawable) placeholder).stop();
        }
        AvatarDrawable drawable =
                new AvatarDrawable(context, placeholder, bitmap, loadedFrom, noFade);
        target.setImageDrawable(drawable);
    }

    /**
     * Create or update the drawable on the target {@link ImageView} to display the supplied
     * placeholder image.
     */
    static void setPlaceholder(ImageView target, int placeholderResId, Drawable placeholderDrawable) {
        if (placeholderResId != 0) {
            target.setImageResource(placeholderResId);
        } else {
            target.setImageDrawable(placeholderDrawable);
        }
        if (target.getDrawable() instanceof AnimationDrawable) {
            ((AnimationDrawable) target.getDrawable()).start();
        }
    }

    final BitmapDrawable image;

    Drawable placeholder;

    long startTimeMillis;
    boolean animating;
    int alpha = 0xFF;

    AvatarDrawable(Context context, Drawable placeholder, Bitmap bitmap,
                   Picasso.LoadedFrom loadedFrom, boolean noFade) {
        Resources res = context.getResources();

        this.image = new BitmapDrawable(res, bitmap);

        boolean fade = loadedFrom != MEMORY && !noFade;
        if (fade) {
            this.placeholder = placeholder;
            animating = true;
            startTimeMillis = SystemClock.uptimeMillis();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (!animating) {
            image.draw(canvas);
        } else {
            float normalized = (SystemClock.uptimeMillis() - startTimeMillis) / FADE_DURATION;
            if (normalized >= 1f) {
                animating = false;
                placeholder = null;
                image.draw(canvas);
            } else {
                if (placeholder != null) {
                    placeholder.draw(canvas);
                }

                int partialAlpha = (int) (alpha * normalized);
                image.setAlpha(partialAlpha);
                image.draw(canvas);
                image.setAlpha(alpha);
                invalidateSelf();
            }
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return image.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return image.getIntrinsicHeight();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        if (placeholder != null) {
            placeholder.setAlpha(alpha);
        }
        image.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (placeholder != null) {
            placeholder.setColorFilter(cf);
        }
        image.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return image.getOpacity();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        image.setBounds(bounds);
        if (placeholder != null) {
            placeholder.setBounds(bounds);
        }
    }

}
package com.example.imdemo.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.example.imdemo.videoUtils.RecyclingBitmapDrawable;

/**
 * Created by SH on 2017/1/10.
 * Sub-class of ImageView which automatically notifies the drawable when it is
 * being displayed.
 */

public class RecyclingImageView extends ImageView {
    public RecyclingImageView(Context context) {
        super(context);
    }

    public RecyclingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclingImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDetachedFromWindow() {
        // This has been detached from Window, so clear the drawable
        setImageDrawable(null);
        super.onDetachedFromWindow();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        // Keep hold of previous Drawable
        final Drawable previousDrawable = getDrawable();
        // call super to set new drawable
        super.setImageDrawable(drawable);

        // Notify new Drawable that it is being displayed
        notifyDrawable(drawable, true);

        // Notify old Drawable so it is no longer being displayed
        notifyDrawable(previousDrawable, false);
    }

    /**
     * notifies the drawable that it's displaystate has changed
     * @param drawable
     * @param isDisplayed
     */
    private static void notifyDrawable(Drawable drawable, boolean isDisplayed) {
        if(drawable instanceof RecyclingBitmapDrawable){
            ((RecyclingBitmapDrawable)drawable).setIsDisplayed(isDisplayed);
        }else if(drawable instanceof LayerDrawable){
            LayerDrawable drawableLayer = (LayerDrawable) drawable;
            for (int i = 0,z = drawableLayer.getNumberOfLayers(); i < z; i++) {
                notifyDrawable(drawableLayer,isDisplayed);
            }
        }
    }
}

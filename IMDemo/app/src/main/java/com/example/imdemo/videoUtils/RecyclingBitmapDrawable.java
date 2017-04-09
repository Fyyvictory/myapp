package com.example.imdemo.videoUtils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by SH on 2017/1/5.
 */

public class RecyclingBitmapDrawable extends BitmapDrawable {
    static final String TAG = "CountingBitmapDrawable";

    private int mCacheRefCount = 0;
    private int mDisplayRefCount = 0;

    private boolean mHasBeenDisplayed;

    public RecyclingBitmapDrawable(Resources res, Bitmap bitmap){super(res,bitmap);}

    public void setIsDisplayed(boolean isDisplayed){
        synchronized (this){
            if(isDisplayed){
                mDisplayRefCount++;
                mHasBeenDisplayed = true;
            }else
                mDisplayRefCount--;
            checkState();
        }
    }

    public void setIsCached(boolean isCached){
        synchronized (this){
            if(isCached)
                mCacheRefCount++;
            else
                mCacheRefCount--;
        }
        checkState();
    }

    private void checkState() {
        // if the drawable cache and display ref count = 0,and
        //this drawable has been displayed,then recycle
        if(mCacheRefCount <= 0 && mDisplayRefCount <= 0 && mHasBeenDisplayed
                && hasValidBitmap()){
            getBitmap().recycle();
        }
    }

    private boolean hasValidBitmap() {
        Bitmap bitmap = getBitmap();
        return bitmap != null && bitmap.isRecycled();
    }
}

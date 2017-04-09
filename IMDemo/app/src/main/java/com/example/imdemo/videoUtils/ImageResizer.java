package com.example.imdemo.videoUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.support.multidex.BuildConfig;
import android.util.Log;

import java.io.FileDescriptor;


/**
 * Created by SH on 2017/1/5.
 * 图片尺寸处理
 */

public class ImageResizer extends ImageWorker {
    private static final String TAG = "ImageResizer";
    protected int mImageWidth;
    protected int mImageHeight;

    public ImageResizer(Context mCon, int imgSize) {
        super(mCon);
        setImageSize(imgSize);
    }

    public ImageResizer(Context mCon, int width, int height) {
        super(mCon);
        setImageSize(width, height);
    }

    private void setImageSize(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    public  void setImageSize(int imgSize) {
        setImageSize(imgSize, imgSize);
    }

    @Override
    protected Bitmap processBitmap(Object data) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap: ");
        }
        String path = String.valueOf(data);

        return ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND);
    }

    protected Bitmap processBitmap(int resId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap: " + resId);
        }
        return decodeSampledBitmapFromResource(mResources, resId, mImageWidth,
                mImageHeight, getImageCache());
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources mResources, int resId, int mImageWidth, int mImageHeight, ImageCache imageCache) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mResources, resId, options);
        options.inSampleSize = calculateInSampleSize(options, mImageWidth, mImageHeight);
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, imageCache);
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(mResources, resId, options);
    }

    public static Bitmap decodeSampledBitmapFromFile(String filename,
                                                     int reqWidth, int reqHeight, ImageCache cache) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, cache);
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename);
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight,
                                                           ImageCache cache) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options, cache);
        }
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void addInBitmapOptions(BitmapFactory.Options options, ImageCache imageCache) {
        options.inMutable = true;
        if (imageCache != null) {
            Bitmap inBitmap = imageCache.getBitmapFromReusableSet(options);
            if (inBitmap != null) {
                options.inBitmap = inBitmap;
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int width, int height) {
        final int wid = options.outWidth;
        final int hei = options.outHeight;
        int inSampleSize = 1;

        if (hei > height || wid > width) {
            final int halfHeight = hei / 2;
            final int halfWidth = wid / 2;

            while ((halfHeight / inSampleSize) > height
                    && (halfWidth / inSampleSize) > width) {
                inSampleSize *= 2;
            }

            long totalPixels = wid * hei / inSampleSize;
            final long totalReqPixelsCap = width * height * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }
}

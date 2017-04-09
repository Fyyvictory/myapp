package com.example.imdemo.videoUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.multidex.BuildConfig;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by SH on 2017/1/5.
 * <p>
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an
 * ImageView. It handles things like using a memory and disk cache, running the work in a background
 * thread and setting a placeholder image.
 */
public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 200;

    private ImageCache mImageCache;
    private Bitmap mLoadingBitmap;
    private boolean mFadeInBitmap = true;
    private boolean mExitTasksEarly = false;
    protected boolean mPauseWork = false;
    private final Object mPauseWorkLock = new Object();

    protected Resources mResources;

    private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_CLOSE = 3;


    public static final Executor DUAL_THREAD_EXECUTOR = Executors.newFixedThreadPool(2);

    protected ImageWorker(Context mCon) {
        mResources = mCon.getResources();
    }

    public void loadImage(Object data, ImageView imageView) {
        if (data == null) {
            return;
        }
        BitmapDrawable value = null;
        if (mImageCache != null) {
            value = mImageCache.getBitmapFromMemCache(String.valueOf(data));
        }
        if (value != null) {
            // 内存中有当前的bitmapdrawable对象，不用加载，直接从内存取出
            imageView.setImageDrawable(value);
        } else if (cancelPotentialWork(data, imageView)) {
            // begin do asyncTask
            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(data, imageView);
            AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, mLoadingBitmap, bitmapWorkerTask);
            imageView.setImageDrawable(asyncDrawable);
            bitmapWorkerTask.executeOnExecutor(DUAL_THREAD_EXECUTOR);
        }
    }

    /**
     * 设置加载过程中默认显示的图片
     *
     * @param bitmap
     */
    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * 同上，加载资源图片
     *
     * @param resId
     */
    public void setmLoadingBitmap(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
    }

    public void addImagCache(FragmentManager fragmentManager, ImageCache.ImageCacheParams cacheParams) {
        ImageCache.ImageCacheParams mImageCacheParams = cacheParams;
        mImageCache = ImageCache.getInstance(fragmentManager, cacheParams);
        new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
    }

    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
        setPauseWork(false);
    }

    protected ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * Cancels any pending work attached to the provided ImageView.
     *
     * @param imageView
     */
    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (BuildConfig.DEBUG) {
                final Object bitmapData = bitmapWorkerTask.data;
                Log.d(TAG, "cancelWork: canclework");
            }
        }
    }

    public static boolean cancelPotentialWork(Object obj, ImageView img) {
        final BitmapWorkerTask task = getBitmapWorkerTask(img);

        if (task != null) {
            final Object bitmapData = task.data;
            if (bitmapData == null || !bitmapData.equals(obj)) {
                task.cancel(true);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CanclePotentialWork");
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapAsyncWeak;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            bitmapAsyncWeak = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapAsyncWeak.get();
        }
    }

    /**
     * 异步的加载图片的过程
     */
    private class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapDrawable> {

        private Object data;
        private final WeakReference<ImageView> imgWeak;

        public BitmapWorkerTask(Object data, ImageView img) {
            this.data = data;
            imgWeak = new WeakReference<ImageView>(img);
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doinbackground-startasyncwork");
            }

            final String dataString = String.valueOf(data);
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (bitmap == null && !isCancelled()
                    && getAttachedImageView() != null && !mExitTasksEarly) {
                bitmap = processBitmap(data);
            }
            if (bitmap != null) {
                if (Utils.hasHoneycomb()) {
                    // Running on Honeycomb or newer, so wrap in a standard BitmapDrawable
                    drawable = new BitmapDrawable(mResources, bitmap);
                } else {
                    // Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
                    // which will recycle automagically
                    drawable = new RecyclingBitmapDrawable(mResources, bitmap);
                }

                if (mImageCache != null) {
                    mImageCache.addBitmapToCache(dataString, drawable);
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "doInBackground - finished work");
            }
            return drawable;
        }

        /**
         * 图片加载到之后，把它与imageview联系到一起（即associate）
         *
         * @param bitmapDrawable
         */
        @Override
        protected void onPostExecute(BitmapDrawable bitmapDrawable) {
            // super.onPostExecute(bitmapDrawable);
            if (isCancelled() || mExitTasksEarly) {
                bitmapDrawable = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (bitmapDrawable != null && imageView != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onpostExecute-setting bitmap");
                }
                setImagDrawable(imageView, bitmapDrawable);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        /**
         * Returns the ImageView associated with this task as long as the ImageView's task still
         * points to this task as well. Returns null otherwise.
         *
         * @return
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imgWeak.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask)
                return imageView;

            return null;
        }
    }

    protected abstract Bitmap processBitmap(Object data);

    /**
     * 当图片加载完成并显示到Imageview的时候调用此方法
     *
     * @param imageView
     * @param drawable
     */
    private void setImagDrawable(ImageView imageView, Drawable drawable) {
        if (mFadeInBitmap) {
            final TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                    new ColorDrawable(android.R.color.transparent), drawable
            });
            imageView.setBackgroundDrawable(new BitmapDrawable(mResources, mLoadingBitmap));
            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageDrawable(drawable);
        }
    }

    /**
     * 使得正在进行中background work暂停，比如当listview、scrollview正在滑动的时候，我们可以让后台线程先暂停执行，
     * 这样会令滑动效果比较顺畅，不会卡顿，用户体验较好（还算比较妙的方法）
     *
     * @param pauseWork
     */
    public void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            switch ((Integer) params[0]) {
                case MESSAGE_CLEAR:
                    clearCacheInternal();
                    break;
            }
            return null;
        }
    }

    /**
     * 清除所有的缓存
     */
    private void clearCacheInternal() {
        if (mImageCache != null) {
            //done 清除mimagecache对象
            mImageCache.clearCache();
        }
    }

    public void clearCache() {
        new CacheAsyncTask().execute(MESSAGE_CLEAR);
    }

    public void flushCache() {
        new CacheAsyncTask().execute(MESSAGE_FLUSH);
    }

    public void closeCache() {
        new CacheAsyncTask().execute(MESSAGE_CLOSE);
    }
}



package com.example.imdemo.videoUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.multidex.BuildConfig;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.File;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by SH on 2017/1/5.
 */

public class ImageCache {

    public static final String TAG = "ImageCache";
    private android.support.v4.util.LruCache<String, BitmapDrawable> mMemoryCache;
    // Default memory cache size in kilobytes
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 3; // 3MB
    private static final int DEFAULT_COMPRESS_QUALITY = 70;

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = false;
    private Set<SoftReference<Bitmap>> mReusableBitmaps;

    private ImageCache(ImageCacheParams imgCacheParams) {
        init(imgCacheParams);
    }

    public static ImageCache getInstance(FragmentManager manager,ImageCacheParams params) {

        RetainFragment retainFrag = findOrCreateRetainFragment(manager);
        ImageCache imageCache = (ImageCache) retainFrag.getmObj();
        if (imageCache == null) {
            imageCache = new ImageCache(params);
            retainFrag.setmObj(imageCache);
        }
        return imageCache;
    }

    /**
     * 清除所有的缓存，包括内存和磁盘，所以这是一个耗时的操作，不能在UI/main线程中进行
     */
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "clearCache");
            }
        }
    }

    public static class ImageCacheParams{
        public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
        public int compressQuality = DEFAULT_COMPRESS_QUALITY;
        public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
        public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;

        public void setMemCacheSizePercent(float percent){
            if(percent < 0.01f || percent > 0.8f){
                throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                        + "between 0.01 and 0.8 (inclusive)");
            }
            memCacheSize = Math.round(percent
                    * Runtime.getRuntime().maxMemory() / 1024);
        }
    }

    private void init(ImageCacheParams cacheParams) {
        ImageCacheParams mParams = cacheParams;
        // 设置内存缓存
        if(mParams.memoryCacheEnabled){
            if(BuildConfig.DEBUG){
                Log.d(TAG, "init: memorycache created,size = "+mParams.memCacheSize);
            }
            // 如果API版本在honeycomb及以上
            if(Utils.hasHoneycomb()){
                mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
            }
            mMemoryCache = new LruCache<String,BitmapDrawable>(mParams.memCacheSize){
                @Override
                protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                    if(RecyclingBitmapDrawable.class.isInstance(oldValue)){
                        ((RecyclingBitmapDrawable)oldValue).setIsCached(false);
                    }else{
                        // 这里是一个bitmap
                        if(Utils.hasHoneycomb()){
                            mReusableBitmaps.add(new SoftReference<Bitmap>(
                                    oldValue.getBitmap()));
                        }
                    }
                }

                @Override
                protected int sizeOf(String key, BitmapDrawable value) {
                    int size = getBitmapSize(value) / 1024;
                    return size==0?1:size;
                }
            };
        }
    }

    public void addBitmapToCache(String data,BitmapDrawable value){
        if(data == null || value == null){
            return;
        }
        // 添加到内存中
        if(mMemoryCache != null ){
            if(RecyclingBitmapDrawable.class.isInstance(value)){
                ((RecyclingBitmapDrawable)value).setIsCached(true);
            }
            mMemoryCache.put(data,value);
        }
    }

    @TargetApi(19)
    public static int getBitmapSize(BitmapDrawable value) {
        Bitmap bitmap = value.getBitmap();
        // API以上允许给定的大小大于bitmap的大小（byte count）
        if(Utils.hasHoneycombMR1()){
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes()*bitmap.getHeight();
    }

    /**
     * 从内存中获取到bitmap
     * @param data
     * @return
     */
    public BitmapDrawable getBitmapFromMemCache(String data) {
        BitmapDrawable mDrawable = null;
        if(mMemoryCache != null){
            mDrawable = mMemoryCache.get(data);
        }
        if(BuildConfig.DEBUG){
            Log.d(TAG, "getBitmapFromMemCache: getbitmapfrommem");
        }
        return mDrawable;
    }

    protected Bitmap getBitmapFromReusableSet(BitmapFactory.Options options){
        Bitmap bitmap = null;
        if(mReusableBitmaps != null && !mReusableBitmaps.isEmpty()){
            synchronized (mReusableBitmaps){
                final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
                Bitmap item;
                while (iterator.hasNext()){
                    item = iterator.next().get();
                    if(null != item && item.isMutable()){
                        // check to see it the item can be used for bitmap
                        if(canUseForInBitmap(item, options)){
                            bitmap = item;
                            // 获取到之后直接把这个从reuseset中去除
                            iterator.remove();
                            break;
                        }
                    }else{
                        // 这个引用已经被销毁，从reuseset中去除
                        iterator.remove();
                    }
                }
            }
        }
        return bitmap;
    }

    private boolean canUseForInBitmap(Bitmap item, BitmapFactory.Options options) {
        if(!Utils.hasKitKat()){
            return item.getWidth() == options.outWidth
                    && item.getHeight() == options.outHeight
                    && options.inSampleSize == 1;
        }
        // From Android 4.4 (KitKat) onward we can re-use if the byte size of
        // the new bitmap
        // is smaller than the reusable bitmap candidate allocation byte count.
        int width = options.outWidth/options.inSampleSize;
        int height = options.outHeight/options.inSampleSize;
        int byteCount = width*height*getBytesPerPixel(item.getConfig());
        return byteCount <= item.getByteCount();
    }

    /**
     * 获取到图片质量
     * @param config
     * @return
     */
    private int getBytesPerPixel(Bitmap.Config config) {
        if(config == Bitmap.Config.ARGB_8888){
            return 4;
        }else if(config == Bitmap.Config.ARGB_4444){
            return 2;
        }else if(config == Bitmap.Config.RGB_565){
            return 2;
        }else if(config == Bitmap.Config.ALPHA_8){
            return 1;
        }
        return 1;
    }

    /**
     * 获取到存储路径，外部存储就绪就存到sd卡上，否则就存到内存中
     * @param mCon
     * @param uniqueName
     * @return
     */
    public static File getDiskCacheDir(Context mCon,String uniqueName){
        final String filePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !isExternalStorageRemovable()?mCon.getExternalCacheDir().getAbsolutePath()
                :mCon.getCacheDir().getAbsolutePath();
        return new File(filePath+File.separator+uniqueName);
    }
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static boolean isExternalStorageRemovable() {
        if(Utils.hasGingerbread()){
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    public static String hashKeyForDisk(String key){
        String cacheKey;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            cacheKey = bytesToHexString(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] digest) {
        StringBuilder sB = new StringBuilder();
        for (byte aByte : digest) {
            String hex = Integer.toHexString(0xFF & aByte);
            if(hex.length() == 1){
                sB.append('0');
            }
            sB.append(hex);
        }
        return sB.toString();
    }

    private static RetainFragment findOrCreateRetainFragment(FragmentManager fm){
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if(fragment == null){
            fragment = new RetainFragment();
            fm.beginTransaction().add(fragment,TAG).commitAllowingStateLoss();
        }
        return fragment;
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over
     * configuration changes. It will be used to retain the ImageCache object.
     */
    public static class RetainFragment extends Fragment{
        public Object getmObj() {
            return mObj;
        }

        public void setmObj(Object mObj) {
            this.mObj = mObj;
        }

        private Object mObj;

        public RetainFragment(){}

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }
}

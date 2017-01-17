package me.li2.android.photogallery.cache;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Cache manager, include memory and disk.
 *
 * @author weiyi.li
 * li2.me
 * Created on 17/01/2017.
 */
public class CacheManager {
    private MemoryCacheManager mMemoryCacheManager;
    private DiskCacheManager mDiskCacheManager;

    public CacheManager(Context context) {
        // Initialize memory cache
        mMemoryCacheManager = new MemoryCacheManager();
        // Initialize disk cache on background thread.
        mDiskCacheManager = new DiskCacheManager(context, mMemoryCacheManager);
    }

    public Bitmap getBitmapFromMemory(String key) {
        if (mMemoryCacheManager != null) {
            return mMemoryCacheManager.getBitmap(key);
        }
        return null;
    }

    public Bitmap getBitmapFromDisk(String key) {
        if (mDiskCacheManager != null) {
            return mDiskCacheManager.getBitmap(key);
        }
        return null;
    }

    public void addBitmap(String url, Bitmap bitmap) {
        if (mDiskCacheManager != null) {
            mDiskCacheManager.addBitmap(url, bitmap);
        }
    }
}

package me.li2.android.photogallery.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.io.File;

import me.li2.android.photogallery.util.FileUtils;

/**
 * Use a disk cache:
 * JakeWharton/DiskLruCache: Java Implementation of a Disk-based LRU cache.
 * A cache that uses a bounded amount of space on a filesystem.
 *
 * Refer to:
 * http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
 * http://stackoverflow.com/a/10235381/2722270
 * https://jsonformatter.curiousconcept.com/
 *
 * @author weiyi.li
 * li2.me
 * Created on 17/01/2017.
 */
public class DiskCacheManager
{
    private Context mContext;
    private MemoryCacheManager mMemoryCacheManager;

    private DiskLruImageCache mDiskLruImageCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static int COMPRESS_QUALITY = 70;

    public DiskCacheManager(Context context, MemoryCacheManager memoryCacheManager) {
        mContext = context;
        mMemoryCacheManager = memoryCacheManager;
        new InitDiskCacheTask().execute();
    }

    private class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... files) {
            synchronized(mDiskCacheLock) {
                mDiskLruImageCache = new DiskLruImageCache(mContext,
                        DISK_CACHE_SUBDIR, DISK_CACHE_SIZE, COMPRESS_FORMAT, COMPRESS_QUALITY);
                mDiskCacheStarting = false; // Finished initialization
                mDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }

    public void addBitmap(String url, Bitmap bitmap) {
        // Add to memory cache as before
        if (mMemoryCacheManager != null) {
            mMemoryCacheManager.addBitmap(url, bitmap);
        }

        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            mDiskLruImageCache.put(FileUtils.hashKeyForDisk(url), bitmap);
        }
    }

    public Bitmap getBitmap(String url) {
        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //sleep(1); // for test purpose
            return mDiskLruImageCache.getBitmap(FileUtils.hashKeyForDisk(url));
        }
    }

    /**
     * Pauses the current thread for the specified number of seconds.
     *
     * @param seconds The number of seconds to pause.
     */
    @SuppressWarnings("unused")
    void sleep(float seconds) {
        long endTime = System.currentTimeMillis() + (long)(seconds*1000);
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
    }
}

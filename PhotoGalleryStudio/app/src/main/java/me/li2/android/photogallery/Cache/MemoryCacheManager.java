package me.li2.android.photogallery.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Memory cache manager.
 *
 * Use memory cache
 * to cache bitmaps, keeping recently referenced objects in a strong referenced LinkedHashMap
 * and evicting the least recently used memory before the cache exceeds its designed size.
 * Refer to Android Training: caching bitmaps.
 *
 * @author weiyi.li
 * li2.me
 * Created on 17/01/2017.
 */
public class MemoryCacheManager {
    private LruCache<String, Bitmap> mMemoryCache;

    public MemoryCacheManager() {
        buildMemoryCache();
    }

    private void buildMemoryCache() {
        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        // Stored in kilobytes as LruCache takes an int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in KB rather than number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void addBitmap(String key, Bitmap bitmap) {
        if (getBitmap(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmap(String key) {
        return mMemoryCache.get(key);
    }
}

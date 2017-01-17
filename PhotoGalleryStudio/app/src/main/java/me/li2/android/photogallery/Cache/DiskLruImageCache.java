package me.li2.android.photogallery.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.li2.android.photogallery.BuildConfig;
import me.li2.android.photogallery.util.FileUtils;

/**
 * Created by weiyi on 16/1/13.
 * http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
 */
public class DiskLruImageCache {
    private static final String TAG = "LI2_DiskLruImageCache";

    private DiskLruCache mDiskCache;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private int mCompressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    public DiskLruImageCache(Context context, String uniqueName, int diskCacheSize,
                             Bitmap.CompressFormat compressFormat, int quality ) {
        try {
            final File diskCacheDir = getDiskCacheDir(context, uniqueName );
            mDiskCache = DiskLruCache.open( diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize );
            mCompressFormat = compressFormat;
            mCompressQuality = quality;
        } catch (IOException ioe) {
            Log.e(TAG, "Could not open disk cache: " + ioe);

        }
    }

    private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor )
            throws IOException, FileNotFoundException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream( editor.newOutputStream( 0 ), FileUtils.IO_BUFFER_SIZE );
            return bitmap.compress( mCompressFormat, mCompressQuality, out );
        } finally {
            if ( out != null ) {
                out.close();
            }
        }
    }

    // Create a unique subdirectory of the designed app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.
    public File getDiskCacheDir(Context context, String uniqueName) {

        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !FileUtils.isExternalStorageRemovable() ?
                        FileUtils.getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public void put( String key, Bitmap data ) {

        DiskLruCache.Editor editor = null;
        try {
            if (mDiskCache.get(key) == null) {
                editor = mDiskCache.edit( key );
                if (editor != null) {
                    if( writeBitmapToFile( data, editor ) ) {
                        mDiskCache.flush();
                        editor.commit();
                        if ( BuildConfig.DEBUG ) {
                            Log.d( TAG, "image put on disk cache " + key );
                        }
                    } else {
                        editor.abort();
                        if ( BuildConfig.DEBUG ) {
                            Log.d( TAG, "ERROR on: image put on disk cache " + key );
                        }
                    }
                }
            }
        } catch (IOException e) {
            if ( BuildConfig.DEBUG ) {
                Log.d( TAG, "ERROR on: image put on disk cache " + key );
            }
            try {
                if ( editor != null ) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        } finally {
            // should not call editor.abort here, otherwise it will cause crash:
            // IllegalStateException
            // at com.jakewharton.disklrucache.DiskLruCache.completeEdit(DiskLruCache.java:511)
            // refer:
            // https://github.com/square/okhttp/issues/1211
            // http://stackoverflow.com/a/10235381/2722270
        }
    }

    public Bitmap getBitmap( String key ) {

        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get( key );
            if ( snapshot == null ) {
                return null;
            }
            final InputStream in = snapshot.getInputStream( 0 );
            if ( in != null ) {
                final BufferedInputStream buffIn =
                        new BufferedInputStream( in, FileUtils.IO_BUFFER_SIZE );
                bitmap = BitmapFactory.decodeStream( buffIn );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        } finally {
            if ( snapshot != null ) {
                snapshot.close();
            }
        }

        if ( BuildConfig.DEBUG ) {
            Log.d( TAG, bitmap == null ? "" : "image read from disk " + key);
        }

        return bitmap;

    }

    public boolean containsKey( String key ) {

        boolean contained = false;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get( key );
            contained = snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( snapshot != null ) {
                snapshot.close();
            }
        }

        return contained;

    }

    public void clearCache() {
        if ( BuildConfig.DEBUG ) {
            Log.d( TAG, "disk cache CLEARED");
        }
        try {
            mDiskCache.delete();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }
}

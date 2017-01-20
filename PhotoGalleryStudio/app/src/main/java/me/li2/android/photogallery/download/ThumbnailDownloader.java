package me.li2.android.photogallery.download;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thumbnail Downloader
 *
 * @author weiyi.li li2.me 2017-01-18
 * @param <T> The ThumbnailDownloader's user will need to use some object to identify each download
 *               and to determine which UI element to update with the image once it is download.
 *               Rather than locking the user into a specific type of object as the identifier,
 *               using a generic makes the implementation more flexible.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "L_ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mUIHandler;
    private Handler mRequestHandler;
    private ConcurrentHashMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    
    public interface ThumbnailDownloadListener<T> {
        /**
         * This method will be eventually be called when an image has been fully downloaded
         * and is ready to be added to the UI target. Using this listener delegates the responsibility of
         * what to do with the downloaded image to a class other than the downloader itself.
         * Doing so separates the downloading task from the UI updating task.
         *
         * @param target
         * @param thumbnail
         * @param url
         */
        void onThumbnailDownloaded(T target, Bitmap thumbnail, String url);
    }
    
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> l) {
        mThumbnailDownloadListener = l;
    }
    
    public ThumbnailDownloader(Handler uiHandler) {
        super(TAG);
        // 后台线程能在主线程上完成任务的一种方式是，让主线程将其自身的Handler传给后台线程；
        // mUIHandler 始终和主线程保持关联，由它发送的消息都将在主线程中得到处理。
        mUIHandler = uiHandler;
        // 我们也可以传递主线程的 context，通过下述方式获取主线程的 handler:
        // mUIHandler = new Handler(mContext.getMainLooper());
    }
    
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        // onLooperPrepared() is called before the Looper checks the queue for the first time.
        // this makes it a good place to create the Handler implementation.
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    T target = (T) msg.obj;
                    Log.d(TAG, "Got a request for url: " + mRequestMap.get(target));
                    handlerRequest(target);
                }
            }
        };
    }

    /**
     * Download thumbnail from url for target.
     *
     * This method will be executed in the background thread. So the UI thread will
     * not be blocked when call this method.
     *
     * @param target the identifier for the download
     * @param url the URL to download
     */
    public void queueThumbnail(T target, String url) {
        Log.d(TAG, "Got an URL: " + url);
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            // mRequestMap is a thread-safe version of HashMap, here, using the download identifier T
            // as a key, we can store and retrieve the URL associated with the specific download request.
            mRequestMap.put(target, url);
            // then post a new message to the background thread's message queue.
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }
    
    private void handlerRequest(final T target) {
        try {
            // pull the URL from requestMap, to ensure that we are always downloading the most recently
            // requested URL for a give target instance. (This is important because RecyclerView item is
            // recycled and reused.)
            final String url = mRequestMap.get(target);

            if (url == null) {
                return;
            }

            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            if (bitmapBytes == null) {
                return;
            }
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.d(TAG, "Bitmap created");

            // because mUIHandler is associated with the main thread's Looper, the Runnable will be
            // executed on the main thread.
            mUIHandler.post(new Runnable() {
                public void run() {
                    // double-check the requestMap, this is necessary because the RecyclerView
                    // recycles its views. By the time the downloader finishes downloading the Bitmap,
                    // RecyclerView may have recycled the ViewHolder and requested a different URL for it.
                    // This check ensures that each ViewHolder gets the correct image,
                    // even if another request has been made in the meantime.
                    String latestUrl = mRequestMap.get(target);
                    if (latestUrl != url) {
                        Log.e(TAG, "NotMatch: " + latestUrl + ", " + url);
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap, url);
                }
            });
            
        } catch (IOException ioe) {
            // java.net.MalformedURLException: Protocol not found: at java.net.URL.<init>
            Log.e(TAG, "downloading image exception: " + ioe);
        }
    }
    
    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }
}

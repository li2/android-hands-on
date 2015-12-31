package me.li2.android.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    
    Handler mHandler;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
    Handler mResponseHandler;
    ThumbnailDownloadListener<Token> mThumbnailDownloadListener;
    
    public interface ThumbnailDownloadListener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail, String url);
    }
    
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<Token> l) {
        mThumbnailDownloadListener = l;
    }
    
    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        // 后台线程能在主线程上完成任务的一种方式是，让主线程将其自身的Handler传给后台线程；
        // mResponseHandler始终和主线程保持关联，由它发送的消息都将在主线程中得到处理。
        mResponseHandler = responseHandler;
        // 我们也可以传递主线程的context，通过下述方式获取主线程的handler:
        // mResponseHandler = new Handler(mContext.getMainLooper());
    }
    
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        // onLooperPrepared()方法发生在Looper.loop()之前，此时消息还没有开始循环，
        // 所以是我们实现mHandler的好地方，在此处下载。
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(token));
                    handlerRequest(token);
                }
            }
        };
    }
    
    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got an URL: " + url);
        // requestMap是一个同步HashMap。 使用Token作为key，可存储或获取与特定Token关联的URL.
        requestMap.put(token, url);
        // mHandler是和后台线程关联的，我们开放这个方法给主线程，主线程调用这个方法来安排后台线程的任务。
        // 我们把下载信息封装成message后放入后台线程的收件箱。         
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
    }
    
    private void handlerRequest(final Token token) {
        try {
            // 下载，并根据获取的数据构建Bitmap对象；
            final String url = requestMap.get(token);
            if (url == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
            
            // 下载完成后，我们在后台线程使用与主线程关联的handler，安排要在主线程上完成的任务。
            // 除了post，我们也可以sendMessage给主线程，那么主线程的handler需要覆写自己的handleMessage()方法。
            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (requestMap.get(token) != url) {
                        return;
                    }
                    requestMap.remove(token);
                    mThumbnailDownloadListener.onThumbnailDownloaded(token, bitmap, url);
                }
            });
            
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
    
    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}

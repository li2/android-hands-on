package me.li2.android.photogallery;

import android.os.HandlerThread;
import android.util.Log;

public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    
    
    public ThumbnailDownloader() {
        super(TAG);
    }
    
    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got an URL: " + url);
    }
}

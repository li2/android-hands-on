package me.li2.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {
    public static final String TAG = "FlickrFetchr";
    
    // Flickr recently changed to https, so we need to change http to https.
    // http://forums.bignerdranch.com/viewtopic.php?f=423&t=8944
    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "5213808bcc415d5632a3dedfcd9a8ac2";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String PARAM_EXTRAS = "extras";
    
    private static final String EXTRA_SMALL_URL = "url_s";
    
    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        
        } finally {
            connection.disconnect();
        }
    }
    
    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
    
    public void fetchItems() {
        String url = Uri.parse(ENDPOINT).buildUpon()
                        .appendQueryParameter("method", METHOD_GET_RECENT)
                        .appendQueryParameter("api_key", API_KEY)
                        .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                        .build().toString();
        try {
            String xmlString = getUrl(url);
            Log.d(TAG, "Received xml: " + xmlString);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        }
    }
}

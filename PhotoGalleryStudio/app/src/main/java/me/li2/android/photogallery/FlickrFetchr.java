package me.li2.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    public static final String TAG = "FlickrFetchr";
    
    public static final String PREF_SEARCH_QUERY = "searchQuery";
    public static final String PREF_LAST_RESULT_ID = "lastResultId";
    
    // Flickr recently changed to https, so we need to change http to https.
    // http://forums.bignerdranch.com/viewtopic.php?f=423&t=8944
    // The Url to get recent photos on flickr.com
    // https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=5213808bcc415d5632a3dedfcd9a8ac2&extras=url_s
    // 上述url获取的是xml文件，加上 &format=json&nojsoncallback=1 可以获取json文件。
    // The Url to search text such as "android" on flickr.com
    // https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=5213808bcc415d5632a3dedfcd9a8ac2&extras=url_s&text=android
    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "5213808bcc415d5632a3dedfcd9a8ac2";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_TEXT = "text"; 
    
    private static final String EXTRA_SMALL_URL = "url_s";
    private static final String XML_PHOTO = "photo";
    
    byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
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
    
    /*
    getUrlBytes()在LG G3(5.1)可以正常工作，但在荣耀3c(4.2.2)上抛出如下异常：
    java.net.SocketException: Socket is closed
    at org.apache.harmony.xnet.provider.jsse.OpenSSLSocketImpl.checkOpen(OpenSSLSocketImpl.java:232)
    at org.apache.harmony.xnet.provider.jsse.OpenSSLSocketImpl.startHandshake(OpenSSLSocketImpl.java:245)
    at libcore.net.http.HttpConnection.setupSecureSocket(HttpConnection.java:209)
    ... 修改为以下代码后，就可以下载xml文件了。但用到的API都是deprecated.
     */
    byte[] getUrlBytes2(String urlSpec) throws IOException {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 2000);
        HttpConnectionParams.setSoTimeout(params, 20000);
        HttpClient httpClient = new DefaultHttpClient(params);
        HttpGet httpGet = new HttpGet(urlSpec);
        HttpPost httpPost = new HttpPost(urlSpec);
        HttpResponse httpResponse = httpClient.execute(httpPost);
        
        if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            return null;
        } else {
            return EntityUtils.toByteArray(httpResponse.getEntity());
        }
    }
    
    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
    
    // Search和getRecent命令获取的xml格式一致，因此可以使用相同的代码解析。
    public List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();

        try {
            String jsonString = getUrl(url);
            Log.d(TAG, "Received json string: " + jsonString);
            parseItems(items, new JSONObject(jsonString));
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse items", je);
        }
        
        return items;
    }
    
    // 封装getRecent的Url请求。
    public List<GalleryItem> fetchItems() {
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_GET_RECENT)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .build().toString();
        return downloadGalleryItems(url);
    }
    
    // 封装Search的Url请求。
    public List<GalleryItem> search(String query) {
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_SEARCH)
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .appendQueryParameter(PARAM_TEXT, query)
                .build().toString();
        Log.d(TAG, "url= " + url);
        return downloadGalleryItems(url);
    }
    
    void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException, IOException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i=0; i<photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setOwner(photoJsonObject.getString("owner"));
            item.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                return;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}

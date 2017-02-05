package me.li2.android.photogallery.download;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.model.GalleryItem;
import me.li2.android.photogallery.util.FileUtils;
import me.li2.android.photogallery.util.IOUtils;

/**
 * Fetch the recent photos list (xml or json) from Flickr
 *
 * Android 6.0 (API level 23) release removes support for the Apache HTTP client.
 * If your app is using this client and targets Android 2.3 (API level 9) or higher,
 * use HttpsURLConnection class instead.
 *
 * flickr.com recently changed to https, so we need to change http to https.
 * http://forums.bignerdranch.com/viewtopic.php?f=423&t=8944
 *
 * @author weiyi.li
 */
public class FlickrFetcher {
    private static final String TAG = "L_FlickrFetcher";

    // exception:
    // javax.net.ssl.SSLHandshakeException:
    // java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.
    // java.io.FileNotFoundException:
    // java.net.UnknownHostException: Unable to resolve host "api.flickr.com": No address associated with hostname
    // java.net.ConnectException: failed to connect to api...: connect failed: ETIMEDOUT (Connection timed out)
    // Flickr is blocked by GFW, so the ETIMEDOUT exception is thrown. Open the link in PC/Phone browser to double check.

    /**
     * Take the given URL and use it to perform an HTTP GET request,
     * sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in byte[] form. Otherwise,
     * it will throw an IOException.
     */
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        Log.d(TAG, "getUrlBytes urlSpec: " + urlSpec);
        InputStream inputStream = null;
        HttpsURLConnection connection = null;
        byte[] result = null;
        URL url = new URL(urlSpec);

        try {
            connection = (HttpsURLConnection)url.openConnection();
            // Timeout for reading InputStream
            connection.setReadTimeout(3000);
            // Timeout for connection.connect()
            connection.setConnectTimeout(5000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();

            // getResponseCode() is a useful way of getting additional information about the connection.
            // A status code of 200 indicates success.
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode + " with " + urlSpec);
            }

            // Retrieve the response body as an InputStream.
            inputStream = connection.getInputStream();
            if (inputStream != null) {
                // Converts Stream to byte[]
                result = IOUtils.readBytes(inputStream);
            }
        } catch (Exception e) {
            Log.e(TAG, "getUrlBytes exception: " + e);
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (inputStream != null) {
                inputStream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    /** The files format of Search and GetRecent are the same. So use same method to parse. */
    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();

        try {
            String jsonString = null;
            byte[] bytes = getUrlBytes(url);
            if (bytes != null) {
                jsonString = new String(bytes);
            }

            if (jsonString != null) {
                Log.d(TAG, "Received json string: " + jsonString);
                parseItems(items, new JSONObject(jsonString));
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse items", je);
        }

        return items;
    }

    /** Get recent photos */
    public List<GalleryItem> getRecent() {
        return downloadGalleryItems(FlickrUrl.buildRecentUrl());
    }

    /** Get photos from local json file */
    public List<GalleryItem> getItemsFromLocal(Context context, int rawId) {
        List<GalleryItem> items = new ArrayList<>();
        String jsonString = FileUtils.loadRawFileToString(context, R.raw.data);
        try {
            parseItems(items, new JSONObject(jsonString));
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse items", je);
        }
        return items;
    }

    /** Search photos */
    public List<GalleryItem> search(String query) {
        return downloadGalleryItems(FlickrUrl.buildSearchUrl(query));
    }
    
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException, IOException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i=0; i<photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            /*
            Use GSON instead
            GalleryItem item = new GalleryItem();
            item.setStableId(i);
            item.setId(photoJsonObject.getString("id"));
            item.setOwner(photoJsonObject.getString("owner"));
            item.setTitle(photoJsonObject.getString("title"));
            if (!photoJsonObject.has("url_s")) {
                return;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            */
            Gson gson = new Gson();
            GalleryItem item = gson.fromJson(photoJsonObject.toString(), GalleryItem.class);
            item.setStableId(i);
            items.add(item);
        }
    }


    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";

    private static String getPrefValue(Context context, String key) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(key, null);
    }

    private static void savePrefValue(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply();
    }

    public static String getSearchQuery(Context context) {
        return getPrefValue(context, PREF_SEARCH_QUERY);
    }

    public static void saveSearchQuery(Context context, String query) {
        savePrefValue(context, PREF_SEARCH_QUERY, query);
    }

    public static String getLastResultId(Context context) {
        return getPrefValue(context, PREF_LAST_RESULT_ID);
    }

    public static void saveLastResultId(Context context, String resultId) {
        savePrefValue(context, PREF_LAST_RESULT_ID, resultId);
    }
}

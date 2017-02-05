package me.li2.android.photogallery.download;

import android.content.res.Resources;
import android.net.Uri;

import me.li2.android.photogallery.R;

/**
 * Created by weiyi on 05/02/2017.
 * @author weiyi.li
 */

public class FlickrUrl {
    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";

    private static final String KEY_METHOD = "method";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_FORMAT = "format";
    private static final String KEY_NO_JSON_CALLBACK = "nojsoncallback";
    private static final String KEY_PARAM_EXTRAS = "extras";
    private static final String KEY_PARAM_TEXT = "text";

    private static final String VALUE_METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String VALUE_METHOD_SEARCH = "flickr.photos.search";
    private static final String VALUE_FORMAT_JSON = "json";
    private static final String VALUE_PARAM_EXTRA_SMALL_URL = "url_s";

    private static final int ID_API_KEY = R.string.api_key;

    /**
     * Builds an request URL to fetch recent contents
     *
     * The Url likes this:
     * https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=XXX&extras=url_s
     * to fetch json, should append &format=json&nojsoncallback=1
     *
     * @return
     */
    public static String buildRecentUrl() {
        String api_key = Resources.getSystem().getString(ID_API_KEY);
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter(KEY_METHOD, VALUE_METHOD_GET_RECENT)
                .appendQueryParameter(KEY_API_KEY, api_key)
                .appendQueryParameter(KEY_FORMAT, VALUE_FORMAT_JSON)
                .appendQueryParameter(KEY_NO_JSON_CALLBACK, "1")
                .appendQueryParameter(KEY_PARAM_EXTRAS, VALUE_PARAM_EXTRA_SMALL_URL)
                .build().toString();
        return url;
    }

    /**
     * Builds an request URL to search contents with given key words.
     *
     * The Url to search text such as "android" on flickr.com
     * https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=XXX&extras=url_s&text=android
     *
     * @param query the keywords to search
     * @return
     */
    public static String buildSearchUrl(String query) {
        String api_key = Resources.getSystem().getString(ID_API_KEY);
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter(KEY_METHOD, VALUE_METHOD_SEARCH)
                .appendQueryParameter(KEY_API_KEY, api_key)
                .appendQueryParameter(KEY_FORMAT, VALUE_FORMAT_JSON)
                .appendQueryParameter(KEY_NO_JSON_CALLBACK, "1")
                .appendQueryParameter(KEY_PARAM_EXTRAS, VALUE_PARAM_EXTRA_SMALL_URL)
                .appendQueryParameter(KEY_PARAM_TEXT, query)
                .build().toString();
        return url;
    }
}

package me.li2.android.photogallery.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.download.FlickrFetcher;
import me.li2.android.photogallery.download.ThumbnailDownloader;
import me.li2.android.photogallery.download.ThumbnailDownloader.ThumbnailDownloadListener;

/**
 * Data Provider, to centralize data.
 *
 * @author weiyi.li
 * li2.me
 * Created on 18/01/2017.
 */
public class GalleryItemProvider<T>
{
    private static final String TAG = "L_GalleryItemProvider";

    // just for testing disk cache
    private static final boolean FETCH_ITEMS_FROM_LOCAL_JSON = true;

    private Context mContext;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mLastRemovedPosition = -1;
    private GalleryItem mLastRemovedData;
    private OnDataUpdatedListener mOnDataUpdatedListener;
    private ThumbnailDownloader<T> mThumbDownloadThread;


    public interface OnDataUpdatedListener<T> {
        void onItemsUpdated(final List<GalleryItem> items);
        void onItemThumbnailDownloaded(T target, Bitmap thumbnail, String url);
    }

    public GalleryItemProvider(Context context, OnDataUpdatedListener listener) {
        mContext = context;
        mOnDataUpdatedListener = listener;
        startItemThumbnailDownload();
    }

    public void destroy() {
        stopItemThumbnailDownload();
    }

    public int getCount() {
        return mItems.size();
    }

    public GalleryItem getItem(int position) {
        return mItems.get(position);
    }

    public int getItemStableId(int position) {
        return mItems.get(position).getStableId();
    }

    public void swapItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        Collections.swap(mItems, fromPosition, toPosition);
        mLastRemovedPosition = -1;
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        final GalleryItem item = mItems.remove(fromPosition);

        mItems.add(toPosition, item);
        mLastRemovedPosition = -1;
    }

    public void removeItem(int position) {
        //noinspection UnnecessaryLocalVariable
        final GalleryItem removedItem = mItems.remove(position);

        mLastRemovedData = removedItem;
        mLastRemovedPosition = position;
    }


    //-------- Items update thread --------------------------------------------

    public void updateData() {
        new FetchItemsTask().execute();
    }

    // Using AsyncTask to run on a background thread.
    // Fetch XML file from Flickr in a background thread, then parse XML file and store into ArrayList,
    // and every item in the list has a thumbnail URL, then we can use the url to download the image.
    //
    // AsyncTask enables proper and easy use of the UI thread. This class allows you to
    // perform background operations and publish results on the UI thread
    // without having to manipulate threads and/or handlers.
    //
    // AsyncTasks should ideally be used for short operations (a few seconds at the most.)

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            String query = FlickrFetcher.getSearchQuery(mContext);
            if (query != null) {
                Log.d(TAG, "Search query: " + query);
                return new FlickrFetcher().search(query);
            } else {
                if (FETCH_ITEMS_FROM_LOCAL_JSON) {
                    Log.d(TAG, "Fetch items from local json file.");
                    return new FlickrFetcher().fetchItemsFromLocal(mContext, R.raw.data);
                } else {
                    Log.d(TAG, "Fetch items from Flickr.");
                    return new FlickrFetcher().fetchItems();
                }
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            if (mOnDataUpdatedListener != null) {
                mOnDataUpdatedListener.onItemsUpdated(items);
            }
        }
    }


    //-------- Item Thumbnail Download Thread ---------------------------------

    private void startItemThumbnailDownload() {
        mThumbDownloadThread = new ThumbnailDownloader<>(new Handler());
        mThumbDownloadThread.setThumbnailDownloadListener(new ThumbnailDownloadListener<T>() {
            public void onThumbnailDownloaded(T target, Bitmap thumbnail, String url) {
                if (mOnDataUpdatedListener != null) {
                    mOnDataUpdatedListener.onItemThumbnailDownloaded(target, thumbnail, url);
                }
            }
        });
        mThumbDownloadThread.setName("ThumbnailDownloadThread");
        mThumbDownloadThread.start();
        mThumbDownloadThread.getLooper();
    }

    private void stopItemThumbnailDownload() {
        mThumbDownloadThread.clearQueue();
        mThumbDownloadThread.quit();
    }

    public void requestItemThumbnail(T target, String url) {
        mThumbDownloadThread.queueThumbnail(target, url);
    }
}

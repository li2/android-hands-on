package me.li2.android.photogallery.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.cache.CacheManager;
import me.li2.android.photogallery.download.FlickrFetcher;
import me.li2.android.photogallery.download.ThumbnailDownloader;
import me.li2.android.photogallery.download.ThumbnailDownloader.ThumbnailDownloadListener;
import me.li2.android.photogallery.model.GalleryItem;

/**
 * PhotoAdapter
 *
 * @author weiyi.li
 * li2.me
 * Created on 17/01/2017.
 */

public class PhotoAdapter
        extends RecyclerView.Adapter<PhotoViewHolder>
{
    private static final String TAG = "LI2_PhotoAdapter";

    // just for testing disk cache
    private static final boolean FETCH_ITEMS_FROM_LOCAL_JSON = true;

    // Horizon orientation with 4 columns
    private static final int LAYOUT_COLUMNS_NUMBER = 4;
    // Vertical orientation with 4 rows
    private static final int LAYOUT_ROWS_NUMBER = 4;

    private Fragment mAttachedFragment;
    private Context mContext;
    private CacheManager mCacheManager;
    private ThumbnailDownloader<ImageView> mThumbDownloadThread;
    private List<GalleryItem> mGalleryItems;

    public PhotoAdapter(Fragment fragment) {
        mAttachedFragment = fragment;
        mContext = fragment.getContext();
        mGalleryItems = new ArrayList<>();
        mCacheManager = new CacheManager(mContext);
        updateItems();
        startThumbDownloadThread();
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View itemView = layoutInflater.inflate(R.layout.gallery_item, parent, false);

        // set the layout parameters associated with this item view
        // fix issue that show only part of the rightmost item view after RecyclerView.addItemDecoration
        int margin = (int)mContext.getResources().getDimension(R.dimen.recycler_view_item_margin);
        // calculate width & height base on orientation
        int width, height;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            width = ViewGroup.LayoutParams.MATCH_PARENT;
            height = (parent.getHeight() - margin * 2 * LAYOUT_ROWS_NUMBER) / LAYOUT_ROWS_NUMBER;
        } else {
            width = (parent.getWidth() - margin * 2 * LAYOUT_COLUMNS_NUMBER) / LAYOUT_COLUMNS_NUMBER;
            height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        itemView.setLayoutParams(new GridLayoutManager.LayoutParams(width, height));

        return new PhotoViewHolder(mContext, mCacheManager, mRequestImageListener, itemView);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        GalleryItem galleryItem = mGalleryItems.get(position);
        holder.bindGalleryItem(galleryItem);
    }

    @Override
    public int getItemCount() {
        return mGalleryItems.size();
    }

    public void updateItems() {
        new FetchItemsTask().execute();
    }

    // Using AsyncTask to run on a background thread.
    // 在后台线程上从Flicker获取XML数据，然后解析XML并将解析结果存入到GalleryItem数组中。最终每个GalleryItem都包含一个缩略图的URL.
    // AsyncTask适合短暂且较少重复的任务。对于重复的、长时间的任务，需要创建一个专用的后台线程。
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if (!isFragmentAttached()) {
                return new ArrayList<>();
            }

            // 取回存储在shared preferences中的搜索信息。
            String query = FlickrFetcher.getSearchQuery(mContext);
            if (query != null) {
                Log.d(TAG, "Search query: " + query);
                return new FlickrFetcher().search(query);
            } else {
                if (FETCH_ITEMS_FROM_LOCAL_JSON) {
                    Log.d(TAG, "Fetch items from local json file.");
                    return new FlickrFetcher().fetchItemsFromLocal(mContext, R.raw.data);
                } else {
                    return new FlickrFetcher().fetchItems();
                }
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            // when using an AsyncTask, you must check to make sure that your fragment is still attached.
            // If it is not, then operations that rely on the that activity (like creating your ArrayAdapter) will fail.
            if (!isFragmentAttached()) {
                return;
            }

            mGalleryItems = items;
            notifyDataSetChanged();
        }
    }

    private void startThumbDownloadThread() {
        // 通过专用线程下载缩略图后，还需要解决的一个问题是，
        // 在无法与主线程直接通信的情况下，如何协同GridView的adapter实现图片显示呢？
        // 把主线程的handler传给后台线程，后台线程就可以通过这个handler传递消息给主线程，以安排主线程显示图片。
        // ImageView是最合适的Token，它是下载的图片最终要显示的地方。
        mThumbDownloadThread = new ThumbnailDownloader<>(new Handler());
        mThumbDownloadThread.setThumbnailDownloadListener(new ThumbnailDownloadListener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail, String url) {
                if (isFragmentAttached()) {
                    // 比如，第1屏触发了下载，但并未下载完，然后向下滚到了第2屏，第1屏的图片加载到了第2屏上，显示就错了。
                    // ViewHolder中持有ImageView，会被循环利用，而图片下载是异步的，
                    // 为了避免下载的图片加载到错误的ImageView上，需要在bind ViewHolder时给imageView设置一个tag（url）
                    // 当图片下载完成后，如果url和当前的imageView tag一致，则显示图片。
                    String tag = (String)(imageView.getTag());
                    if (tag.equals(url)) {
                        imageView.setImageBitmap(thumbnail);
                    } else {
                        Log.e(TAG, "NOTEQUAL-Internet: " + tag + ", " + url);
                    }

                    // Caching bitmap.
                    mCacheManager.addBitmap(url, thumbnail);
                }
            }
        });
        mThumbDownloadThread.setName("ThumbnailDownloadThread");
        mThumbDownloadThread.start();
        mThumbDownloadThread.getLooper();
    }

    public void stopThumbDownloadThread() {
        mThumbDownloadThread.clearQueue();
        mThumbDownloadThread.quit();
    }

    private PhotoViewHolder.RequestImageListener mRequestImageListener =
            new PhotoViewHolder.RequestImageListener() {
                @Override
                public void onRequestImage(ImageView imageView, String url) {
                    mThumbDownloadThread.queueThumbnail(imageView, url);
                }
            };

    private boolean isFragmentAttached() {
        return (mAttachedFragment != null && mAttachedFragment.getActivity() != null);
    }
}

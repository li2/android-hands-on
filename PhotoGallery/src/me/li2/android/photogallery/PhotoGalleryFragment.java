package me.li2.android.photogallery;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import me.li2.android.photogallery.ThumbnailDownloader.ThumbnailDownloadListener;

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);        
        setHasOptionsMenu(true); // 注册选项菜单
        updateItems();
        
        // 通过专用线程下载缩略图后，还需要解决的一个问题是，
        // 在无法与主线程直接通信的情况下，如何协同GridView的adapter实现图片显示呢？
        // 把主线程的handler传给后台线程，后台线程就可以通过这个handler传递消息给主线程，以安排主线程显示图片。
        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setThumbnailDownloadListener(new ThumbnailDownloadListener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    // ImageView是最合适的Token，它是下载的图片最终要显示的地方。                    
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }
    
    // 因为search intent是由Activity接收处理的，所以fragment的刷新时机由activity控制，
    // 所以我们需要提供一个public方法，以供activity刷新fragment。
    public void updateItems() {
        new FetchItemsTask().execute();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mGridView = (GridView) view.findViewById(R.id.gridView);
        setupAdapter();
        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 需要终止HandlerThread，否则它会一直运行下去。
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }
    
    // 添加选项菜单的回调方法 Options menu callbacks:
    // 1. Override onCreateOptionsMenu(Menu menu, MenuInflater inflater);
    // 2. Override onOptionsItemSelected(MenuItem item);
    // 3. Call setHasOptionsMenu(true); to report that this fragment would like to
    // participate in populating the options menu 
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ment_item_search:
            // This hook is called when the user signals the desire to start a search.
            // You can use this function as a simple way to launch the search UI.
            // SearchManager是系统级服务，负责展现搜索对话框，并管理搜索相关的交互。这句话拆开来说，是这样的：
            // SearchManager检查manifest以确认当前activity是否支持搜索；若支持，就在其上覆盖一个搜索对话框；
            // 然后把search intent发送给当前activity。
            getActivity().onSearchRequested();
            return true;
        case R.id.menu_item_clear:
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    void setupAdapter() {
        // when using an AsyncTask, you must check to make sure that your fragment is still attached.
        // If it is not, then operations that rely on the that activity (like creating your ArrayAdapter) will fail. 
        if (getActivity() == null || mGridView == null) {
            return;
        }
        if (mItems != null) {
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridView.setAdapter(null);
        }
    }
    
    // Using AsyncTask to run on a background thread.
    // 在后台线程上从Flicker获取XML数据，然后解析XML并将解析结果存入到GalleryItem数组中。最终每个GalleryItem都包含一个缩略图的URL.
    // AsyncTask适合短暂且较少重复的任务。对于重复的、长时间的任务，需要创建一个专用的后台线程。
    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            String query = "android"; // Just for testing
            if (query != null) {
                return new FlickrFetchr().search(query);
            } else {
                return new FlickrFetchr().fetchItems();
            }
        }
        
        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }
    
    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.ic_photo);
            
            // 然后需要下载缩略图。如果再AsyncTask中完成所有缩略图的下载，存在两个问题：下载耗时，在下载完成之前UI无法更新；如果缩略图很多，可能会耗尽内存。
            // 所以实际开发时，通常仅在需要显示图片时才去下载。而Adapter知道何时显示哪些视图，所以adapter负责按需下载。
            // 所以我们在此处安排后台线程的下载任务。
            // Download images only when they need to be displayed on screen.
            // The adapter will trigger the image downloading here.
            GalleryItem item = getItem(position);
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());
            return convertView;
        }
    }
}

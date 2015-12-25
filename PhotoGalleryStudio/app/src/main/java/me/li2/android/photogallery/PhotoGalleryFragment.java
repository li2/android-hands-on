package me.li2.android.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.ArrayList;

import me.li2.android.photogallery.ThumbnailDownloader.ThumbnailDownloadListener;

public class PhotoGalleryFragment extends VisibleFragment {
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
        
        // 监听GridView item点击事件，通过隐式 implicit intent 启动默认浏览器以查看图片。
        mGridView.setOnItemClickListener(new OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> gridView, View view, int pos, long id) {
               GalleryItem item = mItems.get(pos);
               
               Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
               // Intent i = new Intent(Intent.ACTION_VIEW, photoPageUri);
               // 使用显示 explicit intent 代替隐式 implicit intent，在应用内的WebView中打开图片，而不是在外部的浏览器。
               Intent i = new Intent(getActivity(), PhotoPageActivity.class);
               i.setData(photoPageUri);
               startActivity(i);
           }
        });
        
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
        // 由于SearchView不会调用到onOptionsItemSelected()回调方法 ，
        // 所以相比于未使用SearchView之前，我们必须把SearchManager在幕后承担的获取搜索配置信息并显示搜索界面的工作，
        // 放到“前台”：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Pull out the SearchView 获取搜索菜单项的操作视图。
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView)searchItem.getActionView();
            
            // Get the data from our searchable.xml as a SearchableInfo
            // 通过SearchManager获取搜索配置信息，包括：接收intent的activity，searchabel.xml中的信息。
            SearchManager searchManager = (SearchManager)getActivity()
                    .getSystemService(Context.SEARCH_SERVICE);
            // me.li2.android.photogallery/me.li2.android.photogallery.PhotoGalleryActivity
            ComponentName name = getActivity().getComponentName();
            SearchableInfo searchableInfo = searchManager.getSearchableInfo(name);
            
            // 然后将相关信息通知给SearchView。
            searchView.setSearchableInfo(searchableInfo);
            
            // Expand and give focus to SearchView automatically
            // http://stackoverflow.com/a/11710098/2722270
            searchView.setIconifiedByDefault(true);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_search:
            Log.d(TAG, "on search item selected");
            // This hook is called when the user signals the desire to start a search.
            // You can use this function as a simple way to launch the search UI.
            // SearchManager是系统级服务，负责展现搜索对话框，并管理搜索相关的交互。这句话拆开来说，是这样的：
            // SearchManager检查manifest以确认当前activity是否支持搜索；若支持，就在其上覆盖一个搜索对话框；
            // 然后把search intent发送给当前activity。
            getActivity().onSearchRequested();
            return true;
        case R.id.menu_item_clear:
            // 清除搜索信息。
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                .commit();
            updateItems();
            return true;
        case R.id.menu_item_toggle_polling:
            // 增加菜单选项，以控制定时器起停。
            boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
            PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
            // 为了确保onPrepareOptionsMenu()被调到。
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                getActivity().invalidateOptionsMenu();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // 选项菜单不会每次使用时都被重新实例化，所以如果需要更新它的菜单项内容，
    // 可以在覆写下述方法，因为每次显示菜单时都会调用该方法，。
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
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
            Activity activity = getActivity();
            if (activity == null) {
                return new ArrayList<GalleryItem>();
            }

            // 取回存储在shared preferences中的搜索信息。
            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
            if (query != null) {
                Log.d(TAG, "Search query: " + query);
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

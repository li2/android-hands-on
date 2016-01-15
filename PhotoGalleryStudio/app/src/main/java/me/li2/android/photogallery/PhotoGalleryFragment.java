package me.li2.android.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import me.li2.android.photogallery.ThumbnailDownloader.ThumbnailDownloadListener;

public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";
    private final static boolean FETCH_ITEMS_FROM_LOCAL_JSON = true; // just for testing disk cache

    RecyclerView mPhotoRecyclerView;
    List<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;
    LruCache<String, Bitmap> mMemoryCache;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);        
        setHasOptionsMenu(true); // 注册选项菜单
        updateItems();

        // Initialize memory cache
        buildMemoryCache();

        // Initialize disk cache on background thread.
        new InitDiskCacheTask().execute();

        // 通过专用线程下载缩略图后，还需要解决的一个问题是，
        // 在无法与主线程直接通信的情况下，如何协同GridView的adapter实现图片显示呢？
        // 把主线程的handler传给后台线程，后台线程就可以通过这个handler传递消息给主线程，以安排主线程显示图片。
        // ImageView是最合适的Token，它是下载的图片最终要显示的地方。
        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setThumbnailDownloadListener(new ThumbnailDownloadListener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail, String url) {
                if (isVisible()) {
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
                    addBitmapToDiskCache(url, thumbnail);
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
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
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
        if (getActivity() == null || mPhotoRecyclerView == null) {
            return;
        }
        if (mItems != null) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        } else {
            mPhotoRecyclerView.setAdapter(null);
        }
    }

    // Using AsyncTask to run on a background thread.
    // 在后台线程上从Flicker获取XML数据，然后解析XML并将解析结果存入到GalleryItem数组中。最终每个GalleryItem都包含一个缩略图的URL.
    // AsyncTask适合短暂且较少重复的任务。对于重复的、长时间的任务，需要创建一个专用的后台线程。
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            Activity activity = getActivity();
            if (activity == null) {
                return new ArrayList<>();
            }

            // 取回存储在shared preferences中的搜索信息。
            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
            if (query != null) {
                Log.d(TAG, "Search query: " + query);
                return new FlickrFetchr().search(query);
            } else {
                if (FETCH_ITEMS_FROM_LOCAL_JSON) {
                    Log.d(TAG, "Fetch items from local json file.");
                    return new FlickrFetchr().fetchItemsFromLocal(getContext(), R.raw.data);
                } else {
                    return new FlickrFetchr().fetchItems();
                }
            }
        }
        
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }
    
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new PhotoViewHolder(layoutInflater.inflate(R.layout.gallery_item, parent, false));
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
    }

    private class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.gallery_item_imageView);
            mImageView.setOnClickListener(this);
        }

        // Load bitmap
        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
            mImageView.setTag(mGalleryItem.getUrl());

            // check memory cache.
            Bitmap bitmap = getBitmapFromMemoryCache(mGalleryItem.getUrl());
            if (bitmap != null) {
                mImageView.setImageBitmap(bitmap);
            } else {
                mImageView.setImageResource(R.drawable.ic_photo);
                // check disk cache in a background task
                BitmapWorkerTask task = new BitmapWorkerTask(mImageView);
                task.execute(galleryItem.getUrl());
            }
        }

        @Override
        public void onClick(View view) {
            Uri photoPageUri = Uri.parse(mGalleryItem.getPhotoPageUrl());
            // Intent i = new Intent(Intent.ACTION_VIEW, photoPageUri);
            // 使用显示explicit intent 代替隐式implicit intent，在应用内的WebView中打开图片，而不是在外部的浏览器。
            Intent intent = new Intent(getActivity(), PhotoPageActivity.class);
            intent.setData(photoPageUri);
            startActivity(intent);
        }
    }

    // Use a memory cache:
    // Caching bitmaps, keeping recently referenced objects in a strong referenced LinkedHashMap
    // and evicting the least recently used memory before the cache exceeds its designed size.
    // Refer to Android Training: caching bitmaps.
    private void buildMemoryCache() {
        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        // Stored in kilobytes as LruCache takes an int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in KB rather than number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }


    // Use a disk cache:
    // JakeWharton/DiskLruCache: Java Implementation of a Disk-based LRU cache.
    // A cache that uses a bounded amount of space on a filesystem.
    // Refer:
    // http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
    // http://stackoverflow.com/a/10235381/2722270
    // https://jsonformatter.curiousconcept.com/
    private DiskLruImageCache mDiskLruImageCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private static final CompressFormat COMPRESS_FORMAT = CompressFormat.JPEG;
    private static int COMPRESS_QUALITY = 70;

    private class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... files) {
            synchronized(mDiskCacheLock) {
                mDiskLruImageCache = new DiskLruImageCache(getContext(),
                        DISK_CACHE_SUBDIR, DISK_CACHE_SIZE, COMPRESS_FORMAT, COMPRESS_QUALITY);
                mDiskCacheStarting = false; // Finished initialization
                mDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String url = null;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background
        @Override
        protected Bitmap doInBackground(String... urls) {
            url = urls[0];
            // Check disk cache in background thread
            Bitmap bitmap = getBitmapFromDiskCache(url);

            if (bitmap == null) {
                // not found in disk cache
                mThumbnailThread.queueThumbnail(imageViewReference.get(), url);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                ImageView imageView = imageViewReference.get();

                String tag = (String)(imageView.getTag());
                if (tag.equals(url)) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    Log.e(TAG, "NOTEQUAL-readDiskCacheTask: " + tag + ", " + url);
                }
            }
        }
    }

    private void addBitmapToDiskCache(String url, Bitmap bitmap) {
        // Add to memory cache as before
        addBitmapToMemoryCache(url, bitmap);

        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            mDiskLruImageCache.put(Utils.hashKeyForDisk(url), bitmap);
        }
    }

    private Bitmap getBitmapFromDiskCache(String url) {
        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //sleep(1); // for test purpose
            return mDiskLruImageCache.getBitmap(Utils.hashKeyForDisk(url));
        }
    }

    /**
     * Pauses the current thread for the specified number of seconds.
     *
     * @param seconds The number of seconds to pause.
     */
    void sleep(float seconds) {
        long endTime = System.currentTimeMillis() + (long)(seconds*1000);
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
    }

}

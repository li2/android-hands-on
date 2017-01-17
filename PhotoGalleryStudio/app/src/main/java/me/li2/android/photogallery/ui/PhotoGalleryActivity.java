package me.li2.android.photogallery.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import me.li2.android.photogallery.download.FlickrFetcher;
import me.li2.android.photogallery.R;
import me.li2.android.photogallery.ui.basic.SingleFragmentActivity;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    private static final String TAG = "LI2_PhotoGalleryActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
    }
    
    @Override
    protected Fragment createFragment() {
        return new PhotoGalleryFragment();
    }
    
    // 对于一个可搜索的activity，需要覆写onNewIntent()方法，以接收新的intent。
    @Override
    protected void onNewIntent(Intent intent) {
        PhotoGalleryFragment fragment = (PhotoGalleryFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "Received a new search query: " + query);
            FlickrFetcher.saveSearchQuery(this, query);
        }
        
        // 然后刷新PhotoGalleryFragment的当前图片。
        fragment.updateItems();
    }
}

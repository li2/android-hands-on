package me.li2.android.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    private static final String TAG = "PhotoGalleryActivity";
    
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
            Log.i(TAG, "Received a new search query: " + query);
        }
        
        // 然后刷新PhotoGalleryFragment的当前图片。
        fragment.updateItems();
    }
}

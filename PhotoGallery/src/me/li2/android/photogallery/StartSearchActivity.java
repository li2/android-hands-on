package me.li2.android.photogallery;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/*
为了解决问题：ToolBar中的SearchView如何让点击之后跳转到一个新的Activity？
http://segmentfault.com/q/1010000002539102

PhotoGalleryActivity是一个可搜索的Activity，
这个Activity演示了：点击一个按钮（点击MenuItem同理）启动一个可搜索的Activity，启动后SearchView自动展开。
*/

public class StartSearchActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_start_search);
        Button search = (Button) findViewById(R.id.search);
        search.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {
                // Start a searchable activity
                startActivity(new Intent(StartSearchActivity.this, PhotoGalleryActivity.class));
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_start_search_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_search:
            startActivity(new Intent(StartSearchActivity.this, PhotoGalleryActivity.class));        
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}

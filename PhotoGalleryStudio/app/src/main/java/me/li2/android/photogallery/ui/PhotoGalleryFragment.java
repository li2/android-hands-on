package me.li2.android.photogallery.ui;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.download.FlickrFetcher;
import me.li2.android.photogallery.download.PollService;
import me.li2.android.photogallery.ui.basic.VisibleFragment;
import me.li2.android.photogallery.ui.widget.RecyclerViewMarginDecoration;

public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "L_PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter mPhotoAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);        
        setHasOptionsMenu(true); // 注册选项菜单
    }
    
    // 因为search intent是由Activity接收处理的，所以fragment的刷新时机由activity控制，
    // 所以我们需要提供一个public方法，以供activity刷新fragment。
    public void updateItems() {
        if (mPhotoAdapter != null) {
            mPhotoAdapter.updateItems();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setScrollContainer(false);

        int orientation;
        int spanCount;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // shows items in a vertical scrolling list with 3 columns
            orientation = GridLayoutManager.VERTICAL;
            spanCount = 3;
        } else {
            // shows items in a horizontal scrolling list with 2 rows
            orientation = GridLayoutManager.HORIZONTAL;
            spanCount = 2;
        }

        // setup RecyclerView layout manger
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), spanCount, orientation, false);
        mPhotoRecyclerView.setLayoutManager(layoutManager);

        // setup RecyclerView item margin
        float margin = getResources().getDimension(R.dimen.recycler_view_item_margin);
        mPhotoRecyclerView.addItemDecoration(new RecyclerViewMarginDecoration((int)margin));

        // setup RecyclerView adapter
        mPhotoAdapter = new PhotoAdapter(this);
        mPhotoRecyclerView.setAdapter(mPhotoAdapter);

        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPhotoAdapter != null) {
            // 需要终止HandlerThread，否则它会一直运行下去。
            mPhotoAdapter.stopThumbDownloadThread();
            mPhotoAdapter = null;
        }
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
            // me.li2.android.photogallery/me.li2.android.photogallery.ui.PhotoGalleryActivity
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
            FlickrFetcher.saveSearchQuery(getActivity(), null);
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
}

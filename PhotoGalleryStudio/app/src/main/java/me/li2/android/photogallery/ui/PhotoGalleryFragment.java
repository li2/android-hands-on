package me.li2.android.photogallery.ui;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

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
            mPhotoAdapter.updateData();
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

        createDragDropManager();

        return view;
    }

    @Override
    public void onPause() {
        cancelDrag();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPhotoAdapter != null) {
            mPhotoAdapter.destroy();
            mPhotoAdapter = null;
        }

        if (mPhotoRecyclerView != null) {
            mPhotoRecyclerView.setItemAnimator(null);
            mPhotoRecyclerView.setAdapter(null);
            mPhotoRecyclerView = null;
        }

        destroyDragDropManager();
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


    //-------- Draggable ------------------------------------------------------

    private static final int ITEM_DRAG_MODE = RecyclerViewDragDropManager.ITEM_MOVE_MODE_SWAP;
    private static final int ITEM_DRAG_SHADOW_DRAWABLE = R.drawable.material_shadow_z3;
    private static final int ITEM_DRAG_ANIMATION_DURATION = 500;
    private static final float ITEM_DRAG_ALPHA = 1.0f;
    private static final float ITEM_DRAG_SCALE = 1.2f;
    private static final float ITEM_DRAG_ROTATION = 0.0f;

    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mDragDropManager;

    private void createDragDropManager() {
        // create drag & drop manager
        mDragDropManager = new RecyclerViewDragDropManager();

        // start dragging after long press
        mDragDropManager.setInitiateOnLongPress(true);
        mDragDropManager.setInitiateOnMove(false);
        mDragDropManager.setLongPressTimeout(750);

        // setup dragging item effects
        mDragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) ContextCompat.getDrawable(getContext(), ITEM_DRAG_SHADOW_DRAWABLE));
        mDragDropManager.setDragStartItemAnimationDuration(ITEM_DRAG_ANIMATION_DURATION);
        mDragDropManager.setDraggingItemAlpha(ITEM_DRAG_ALPHA);
        mDragDropManager.setDraggingItemScale(ITEM_DRAG_SCALE);
        mDragDropManager.setDraggingItemRotation(ITEM_DRAG_ROTATION);
        mDragDropManager.setItemMoveMode(ITEM_DRAG_MODE);
        // DraggableItemAnimator is required to make item animations properly.
        GeneralItemAnimator animator = new DraggableItemAnimator();
        mPhotoRecyclerView.setItemAnimator(animator);

        // requires *wrapped* adapter for dragging
        mWrappedAdapter = mDragDropManager.createWrappedAdapter(mPhotoAdapter);
        mPhotoRecyclerView.setAdapter(mWrappedAdapter);

        // attach the manager to RecyclerView
        mDragDropManager.attachRecyclerView(mPhotoRecyclerView);

        // also tell the item drag mode to PhotoAdapter
        mPhotoAdapter.setItemMoveMode(ITEM_DRAG_MODE);
    }

    private void destroyDragDropManager() {
        if (mDragDropManager != null) {
            mDragDropManager.release();
            mDragDropManager = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
    }

    private void cancelDrag() {
        if (mDragDropManager != null) {
            mDragDropManager.cancelDrag();
        }
    }

    protected boolean isDragging() {
        if (mDragDropManager != null) {
            return mDragDropManager.isDragging();
        }
        return false;
    }
}

package me.li2.android.photogallery.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;

import java.util.List;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.cache.CacheManager;
import me.li2.android.photogallery.model.GalleryItem;
import me.li2.android.photogallery.model.GalleryItemProvider;

/**
 * PhotoAdapter
 *
 * @author weiyi.li
 * li2.me
 * Created on 17/01/2017.
 */

public class PhotoAdapter
        extends RecyclerView.Adapter<PhotoViewHolder>
        implements DraggableItemAdapter<PhotoViewHolder>
{
    private static final String TAG = "L_PhotoAdapter";

    // Horizon orientation with 4 columns
    private static final int LAYOUT_COLUMNS_NUMBER = 4;
    // Vertical orientation with 4 rows
    private static final int LAYOUT_ROWS_NUMBER = 4;

    private Fragment mAttachedFragment;
    private Context mContext;
    private CacheManager mCacheManager;
    private GalleryItemProvider mDataProvider;

    public PhotoAdapter(Fragment fragment) {
        mAttachedFragment = fragment;
        mContext = fragment.getContext();
        mCacheManager = new CacheManager(mContext);
        mDataProvider = new GalleryItemProvider(mContext, mOnDataUpdatedListener);

        updateData();

        // DraggableItemAdapter requires stable ID, and also
        // have to implement the Adapter.getItemId() method appropriately.
        setHasStableIds(true);
    }

    public void destroy() {
        if (mDataProvider != null) {
            mDataProvider.destroy();
            mDataProvider = null;
        }
    }

    public void updateData() {
        mDataProvider.updateData();
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
        GalleryItem galleryItem = mDataProvider.getItem(position);
        holder.bindGalleryItem(galleryItem);
    }

    @Override
    public int getItemCount() {
        return mDataProvider.getCount();
    }

    @Override
    public long getItemId(int position) {
        return mDataProvider.getItemStableId(position);
    }

    private GalleryItemProvider.OnDataUpdatedListener<ImageView> mOnDataUpdatedListener =
            new GalleryItemProvider.OnDataUpdatedListener<ImageView>() {
                @Override
                public void onItemsUpdated(List<GalleryItem> items) {
                    if (isFragmentAttached()) {
                        notifyDataSetChanged();
                    }
                }

                // 比如，第 1 屏触发了下载，但未下完就滚到了第 2 屏，第 1 屏的图片加载到了第 2 屏上，显示就错了。
                // ViewHolder 中持有 ImageView，会被循环利用，而图片下载是异步的，
                // 为了避免下载的图片加载到错误的 ImageView，需要在 bind ViewHolder 时给 imageView 设置一个 tag（url）
                // 当图片下载完成后，如果 url 和当前的 imageView tag 一致，则显示图片。
                // 这个验证其实在 ThumbnailDownloader 中已经做了，这里大可不必重复验证。
                @Override
                public void onItemThumbnailDownloaded(ImageView imageView, Bitmap thumbnail, String url) {
                    if (isFragmentAttached()) {
                        String tag = (String)(imageView.getTag());
                        if (tag.equals(url)) {
                            imageView.setImageBitmap(thumbnail);
                        } else {
                            Log.e(TAG, "NotMatch: " + tag + ", " + url);
                        }

                        // Caching bitmap.
                        mCacheManager.addBitmap(url, thumbnail);
                    }
                }
            };

    private PhotoViewHolder.RequestImageListener mRequestImageListener =
            new PhotoViewHolder.RequestImageListener() {
                @Override
                public void onRequestImage(ImageView imageView, String url) {
                    mDataProvider.requestItemThumbnail(imageView, url);
                }
            };

    // when using an AsyncTask, you must check to make sure that your fragment is still attached.
    // If it is not, then operations that rely on the that activity will fail,
    // like creating your ArrayAdapter, update UI view.
    private boolean isFragmentAttached() {
        return (mAttachedFragment != null && mAttachedFragment.getActivity() != null);
    }


    //-------- Draggable ------------------------------------------------------

    private int mItemMoveMode = RecyclerViewDragDropManager.ITEM_MOVE_MODE_DEFAULT;

    public void setItemMoveMode(int itemMoveMode) {
        mItemMoveMode = itemMoveMode;
    }

    // NOTE: Make accessible with short name
    private interface Draggable extends DraggableItemConstants {
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");

        if (fromPosition == toPosition) {
            return;
        }

        if (mItemMoveMode == RecyclerViewDragDropManager.ITEM_MOVE_MODE_DEFAULT) {
            mDataProvider.moveItem(fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        } else {
            mDataProvider.swapItem(fromPosition, toPosition);
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCheckCanStartDrag(PhotoViewHolder holder, int position, int x, int y) {
        return true;
    }

    @Override
    public boolean onCheckCanDrop(int draggingPosition, int dropPosition) {
        return true;
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(PhotoViewHolder holder, int position) {
        return null;
    }
}

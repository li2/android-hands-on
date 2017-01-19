package me.li2.android.photogallery.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import java.lang.ref.WeakReference;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.cache.CacheManager;
import me.li2.android.photogallery.model.GalleryItem;

/**
 * PhotoViewHolder
 *
 * @author weiyi.li
 * li2.me
 * Created on 17/01/2017.
 */

public class PhotoViewHolder extends AbstractDraggableItemViewHolder implements View.OnClickListener
{
    private static final String TAG = "L_PhotoViewHolder";
    private Context mContext;
    private CacheManager mCacheManager;
    private ImageView mImageView;
    private TextView mImageIndicator;
    private GalleryItem mGalleryItem;
    private RequestImageListener mRequestImageListener;

    public enum IndicatorType {
        DEFAULT,
        MEMORY_CACHE,
        DISK_CACHE,
        INTERNET,
    }

    public interface RequestImageListener {
        void onRequestImage(final PhotoViewHolder photoViewHolder, final String url);
    }

    public PhotoViewHolder(Context context, CacheManager cacheManager, RequestImageListener listener, View itemView) {
        super(itemView);
        mContext = context;
        mCacheManager = cacheManager;
        mImageView = (ImageView) itemView.findViewById(R.id.gallery_item_imageView);
        mImageIndicator = (TextView) itemView.findViewById(R.id.gallery_item_indicator);
        mImageView.setOnClickListener(this);
        mRequestImageListener = listener;
    }

    // Load bitmap
    public void bindGalleryItem(GalleryItem galleryItem) {
        mGalleryItem = galleryItem;
        mImageView.setTag(mGalleryItem.getUrl());

        // check memory cache.
        Bitmap bitmap = mCacheManager.getBitmapFromMemory(mGalleryItem.getUrl());
        if (bitmap != null) {
            updateImageView(IndicatorType.MEMORY_CACHE, bitmap);
        } else {
            // check disk cache in a background task
            BitmapWorkerTask task = new BitmapWorkerTask(this);
            task.execute(galleryItem.getUrl());
        }

        if (galleryItem.getUrl() == null || galleryItem.getUrl().isEmpty()) {
            mImageIndicator.setText(""+galleryItem.getStableId());
        }
    }

    // 比如，第 1 屏触发了下载，但未下完就滚到了第 2 屏，第 1 屏的图片加载到了第 2 屏上，显示就错了。
    // ViewHolder 中持有 ImageView，会被循环利用，而图片下载是异步的，
    // 为了避免下载的图片加载到错误的 ImageView，需要在 bind ViewHolder 时给 imageView 设置一个 tag（url）
    // 当图片下载完成后，如果 url 和当前的 imageView tag 一致，则显示图片。
    // 这个验证其实在 ThumbnailDownloader 中已经做了，这里大可不必重复验证。

    public void updateImageView(final IndicatorType type, final Bitmap bitmap, final String url) {
        String latestUrl = (String)(mImageView.getTag());

        if (url == null || latestUrl.equals(url)) {
            mImageView.setImageBitmap(bitmap);
            updateImageIndicator(type);
        } else {
            Log.e(TAG, "NotMatch: " + latestUrl + ", " + url);
        }
    }

    private void updateImageView(IndicatorType type, Bitmap bitmap) {
        updateImageView(type, bitmap, null);
    }

    private void resetImageView() {
        mImageView.setImageResource(R.drawable.ic_default_photo);
        updateImageIndicator(IndicatorType.DEFAULT);
    }

    private void updateImageIndicator(IndicatorType type) {
        String indicator = "";
        switch (type) {
            case MEMORY_CACHE:
                indicator = "M";
                break;
            case DISK_CACHE:
                indicator = "D";
                break;
            case INTERNET:
                indicator = "I";
                break;
        }
        mImageIndicator.setVisibility(indicator.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        mImageIndicator.setText(indicator);
    }

    @Override
    public void onClick(View view) {
        // Use getAdapterPosition() instead of getPosition
        Toast.makeText(mContext, "Click item " + getAdapterPosition(), Toast.LENGTH_LONG).show();

        Uri photoPageUri = Uri.parse(mGalleryItem.getPhotoPageUrl());
        // Intent i = new Intent(Intent.ACTION_VIEW, photoPageUri);
        // 使用显示explicit intent 代替隐式implicit intent，在应用内的WebView中打开图片，而不是在外部的浏览器。
        Intent intent = new Intent(mContext, PhotoPageActivity.class);
        intent.setData(photoPageUri);
        mContext.startActivity(intent);
    }

    // check disk cache in a background task
    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<PhotoViewHolder> photoHolderReference;
        private String url = null;

        public BitmapWorkerTask(PhotoViewHolder photoViewHolder) {
            photoHolderReference = new WeakReference<>(photoViewHolder);
        }

        // Decode image in background
        @Override
        protected Bitmap doInBackground(String... urls) {
            url = urls[0];
            // Check disk cache in background thread
            Bitmap bitmap = mCacheManager.getBitmapFromDisk(url);

            if (bitmap == null) {
                // not found in disk cache
                if (mRequestImageListener != null) {
                    mRequestImageListener.onRequestImage(photoHolderReference.get(), url);
                }
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            PhotoViewHolder photoViewHolder = photoHolderReference.get();

            if (photoViewHolder != null) {
                if (bitmap != null) {
                    photoViewHolder.updateImageView(IndicatorType.DISK_CACHE, bitmap, url);
                } else {
                    photoViewHolder.resetImageView();
                }
            }
        }
    }
}

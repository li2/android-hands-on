package me.li2.android.photogallery.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
    private GalleryItem mGalleryItem;
    private RequestImageListener mRequestImageListener;

    public interface RequestImageListener {
        void onRequestImage(ImageView imageView, String url);
    }

    public PhotoViewHolder(Context context, CacheManager cacheManager, RequestImageListener listener, View itemView) {
        super(itemView);
        mContext = context;
        mCacheManager = cacheManager;
        mImageView = (ImageView) itemView.findViewById(R.id.gallery_item_imageView);
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
            mImageView.setImageBitmap(bitmap);
        } else {
            mImageView.setImageResource(R.drawable.ic_default_photo);
            // check disk cache in a background task
            BitmapWorkerTask task = new BitmapWorkerTask(mImageView);
            task.execute(galleryItem.getUrl());
        }
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
            Bitmap bitmap = mCacheManager.getBitmapFromDisk(url);

            if (bitmap == null) {
                // not found in disk cache
                if (mRequestImageListener != null) {
                    mRequestImageListener.onRequestImage(imageViewReference.get(), url);
                }
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
                    Log.e(TAG, "NotMatch: " + tag + ", " + url);
                }
            }
        }
    }
}

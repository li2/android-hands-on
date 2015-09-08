package me.li2.android.criminalintent;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.Display;
import android.widget.ImageView;

public class PictureUtils {
    // Get a BitmapDrawable from a local file that is scaled down to fit the current window size.
    // 通常无法及时获得用来显示图片的视图尺寸。例如，在onCreateView(...)方法中，就无法获得ImageView视图的尺寸。
    // 设备的默认屏幕大小是固定可知的，因此，稳妥起见，可以缩放图片至设备的默认显示屏大小。
    @SuppressWarnings("deprecation")
    public static BitmapDrawable getScaledDrawable(Activity a, String path) {    
        Display display = a.getWindowManager().getDefaultDisplay();
        float destWidth = display.getWidth();
        float destHeight = display.getHeight();
        
        // Read in the dimensions of the image on disk
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        
        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;
        
        int inSampleSize = 1;
        if (srcHeight > destHeight || srcWidth > destWidth) {
            int size1 = Math.round(srcHeight / destHeight);
            int size2 = Math.round(srcWidth / destWidth);
            inSampleSize = (size1 > size2) ? size1 : size2;
        }
        
        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return new BitmapDrawable(a.getResources(), bitmap);
    }
    
    public static void cleanImageView(ImageView imageView) {
        if (!(imageView.getDrawable() instanceof BitmapDrawable)) {
            return;
        }
        
        // Clean up the view's image for the sake of memory
        BitmapDrawable b = (BitmapDrawable) imageView.getDrawable();
        // Bitmap.recycle()方法释放了bitmap占用的原始存储空间。
        // 如果不主动调用recycle()方法释放内存，占用的内存也会被清理。但是，它是在将来某个时点在finalizer中清理，
        // 而不是在bitmap自身的垃圾回收时清理。这意味着很可能在finalizer调用之前，应用已经耗尽了内存资源。
        b.getBitmap().recycle();
        imageView.setImageDrawable(null);
    }
}

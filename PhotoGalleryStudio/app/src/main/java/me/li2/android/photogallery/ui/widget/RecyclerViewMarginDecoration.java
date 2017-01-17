package me.li2.android.photogallery.ui.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 *
 * {@link RecyclerView} support the concept of {@link android.support.v7.widget.RecyclerView.ItemDecoration}:
 *
 * It allows the application to add a special drawing and layout offset
 * to specific item views from the adapter's data set. This can be useful for drawing dividers
 * between items, highlights, visual grouping boundaries and more.
 *
 * Created by weiyi.li on 09/01/2017.
 * http://li2.me
 */

public class RecyclerViewMarginDecoration extends RecyclerView.ItemDecoration {
    private int margin;

    public RecyclerViewMarginDecoration(int margin) {
        this.margin = margin;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.top = margin;
        outRect.bottom = margin;
        outRect.left = margin;
        outRect.right = margin;
    }
}

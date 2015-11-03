package me.li2.android.boxdrawingview;

import android.support.v4.app.Fragment;

public class DragAndDraw extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new DragAndDrawFragment();
    }
}

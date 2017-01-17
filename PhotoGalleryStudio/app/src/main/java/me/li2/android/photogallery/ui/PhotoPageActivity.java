package me.li2.android.photogallery.ui;

import android.support.v4.app.Fragment;

import me.li2.android.photogallery.ui.basic.SingleFragmentActivity;

public class PhotoPageActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new PhotoPageFragment();
    }
}

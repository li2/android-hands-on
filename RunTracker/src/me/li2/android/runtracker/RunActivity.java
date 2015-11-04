package me.li2.android.runtracker;

import android.support.v4.app.Fragment;


public class RunActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new RunFragment();
    }
}

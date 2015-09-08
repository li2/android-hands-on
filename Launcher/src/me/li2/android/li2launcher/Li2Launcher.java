package me.li2.android.li2launcher;

import android.support.v4.app.Fragment;

public class Li2Launcher extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new Li2LauncherFragment();
    }
}

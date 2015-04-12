package me.li2.android.criminalintent;

import android.app.ActionBar;
import android.support.v4.app.Fragment;

public class CrimeListActivity extends SingleFragmentActivity {
    private static final String TAG = "CrimeListActivity";

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        // should use @android:style/Theme.Holo.Light as app theme,
        // Theme.AppCompat.Light without action bar.
        if (actionBar != null) {
            actionBar.setTitle(getResources().getString(R.string.app_name));
        }
    }
    
    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

}
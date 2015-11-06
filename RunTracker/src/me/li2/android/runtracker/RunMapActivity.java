package me.li2.android.runtracker;

import android.support.v4.app.Fragment;

public class RunMapActivity extends SingleFragmentActivity{
    public static final String EXTRA_RUN_ID = "me.li2.android.runtracker.run_id";
    
    @Override
    protected Fragment createFragment() {
        long runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        if (runId == -1) {
            return new RunMapFragment();
        } else {
            return RunMapFragment.newInstance(runId);
        }
    }
}

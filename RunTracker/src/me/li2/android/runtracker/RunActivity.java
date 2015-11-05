package me.li2.android.runtracker;

import android.support.v4.app.Fragment;


public class RunActivity extends SingleFragmentActivity {
    // 实现查看任意旅程列表项的明细信息，要查看明细， RunFragment需要传入的旅程ID参数（ argument）的支持。
    // RunActivity负责托管RunFragment，因此它也需要旅程ID附加信息。
    // public static final String EXTRA_RUN_ID = "EXTRA_RUN_ID"; // 差劲的命名
    public static final String EXTRA_RUN_ID = "me.li2.android.runtracker.run_id";
    
    @Override
    protected Fragment createFragment() {
        long runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        if (runId != -1) {
            return RunFragment.newInstance(runId);
        } else {
            return new RunFragment();
        }
    }
}

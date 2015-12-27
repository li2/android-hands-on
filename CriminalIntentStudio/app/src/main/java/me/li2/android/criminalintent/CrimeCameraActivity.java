package me.li2.android.criminalintent;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Window;
import android.view.WindowManager;

public class CrimeCameraActivity extends SingleFragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Hide the window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Hide the status bar and other OS-leverl chrome
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // 在创建activity视图（即setContentView）之前，必须调用requestWindowFeature()方法及addFlags()方法。
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected Fragment createFragment() {
        return new CrimeCameraFragment();
    }

}

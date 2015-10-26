package me.li2.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.widget.Toast;

public abstract class VisibleFragment extends Fragment {
    public static final String TAG = "VisibleFragment";

    // 我们在PollService中发送了自己的broadcast intent，接下来是接收。
    // 可以在配置文件中声明独立的receiver, 然后实现它，类似StartupReceiver.
    // 但因为我们需要在PhotoGalleryFragment存在的时候接收intent，而独立的receiver很难做到，
    // 因为需要某种方法把fragment存在的状态通知这个独立的receiver;
    // 而使用dynamic broadcast receiver可解决问题：通过代码创建intent filter，登记receiver.    
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String toast = "Got a broadcast:" + intent.getAction();
            Toast.makeText(getActivity(), toast, Toast.LENGTH_LONG).show();
        }
    };
    
    @Override
    public void onResume() {
        super.onResume();
        /* 通过代码创建intent filter类似于：
        <intent-filter>
            <action android:name="me.li2.android.photogallery.SHOW_NOTIFICATION"/>
        </intent-filter>
        */
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        // 配置filter：filter.addCategory(category); filter.addAction(action); ...
        getActivity().registerReceiver(mOnShowNotification, filter);
    };
    
    @Override
    public void onPause() {
        super.onPause();
        // 在onResume()相应的onPause()方法中清除动态登记的receiver.
        // 注意保留fragment中onCreate和onDestroy方法的运用，因为设备旋转时，getActivity()会返回不同的值；
        // 若想在onCreate和onDestroy中实现登记和清除，需要调用getApplicationContext().
        getActivity().unregisterReceiver(mOnShowNotification);
    }
}

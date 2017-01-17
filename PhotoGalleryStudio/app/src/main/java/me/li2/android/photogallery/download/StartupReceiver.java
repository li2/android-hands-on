package me.li2.android.photogallery.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "LI2_StartupReceiver";
    
    // 在配置文件中完成声明后，即使app未运行，只要有匹配的broadcast intent发来，
    // broadcast receiver就会接收，onReceive方法就被调到，
    // 执行完了broadcast receiver就会被销毁。
    // onReceive运行在主线程，所以不能执行耗时操作。
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received broadcast intent: " + intent.getAction());
        
        // 所以broadcast receiver非常适于处理小任务，比如
        // 设备重启后启动定时器。
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAlarmOn = prefs.getBoolean(PollService.PREF_IS_ALARM_ON, false);
        PollService.setServiceAlarm(context, isAlarmOn);
    }
}

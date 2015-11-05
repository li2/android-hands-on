package me.li2.android.runtracker;

import android.content.Context;
import android.location.Location;
import android.util.Log;

// 选择了独立的Broadcast- Receiver。使用它完成此项任务，可保证location intent能够得到及时处理，
// 而不用关心RunTraker应用的其余部分是否正在运行。

public class TrackingLocationReceiver extends LocationReceiver {
    
    private static final String TAG = "run_TrackingLocationReceiver";
    
    @Override
    protected void onLocationReceived(Context context, Location loc) {
        Log.d(TAG, "Got location then insert into db: "
                + "latitude " + loc.getLatitude() + ", longitude" + loc.getLatitude());
        RunManager.get(context).insertLocation(loc);
    }
    
}

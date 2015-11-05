package me.li2.android.runtracker;

import me.li2.android.runtracker.RunDatabaseHelper.RunCursor;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class RunManager {
    private static final String TAG = "run_RunManager";
    
    private static final String PREFS_FILE = "runs";
    private static final String PREF_CURRENT_RUN_ID = "RunManager.currentRunId";
    
    public static final String ACTION_LOCATION = "me.li2.android.runtracker.ACTION_LOCATION";
    
    private static final String TEST_PROVIDER = "TEST_PROVIDER";
    
    private static RunManager sRunManager;
    private Context mAppContext;
    private LocationManager mLocationManager;
    private RunDatabaseHelper mDatabaseHelper;
    private SharedPreferences mPrefs;
    private long mCurrentRunId;
    
    // The private constructor forces users to use RunManager.get(context)
    private RunManager(Context appContext) {
        mAppContext = appContext;
        mLocationManager = (LocationManager)mAppContext.getSystemService(Context.LOCATION_SERVICE);
        mDatabaseHelper = new RunDatabaseHelper(appContext);
        mPrefs = mAppContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        mCurrentRunId = mPrefs.getLong(PREF_CURRENT_RUN_ID, -1);
    }
    
    public static RunManager get(Context c) {
        if (sRunManager == null) {
            // Use the application context to avoid leaking activities
            sRunManager = new RunManager(c.getApplicationContext());
        }
        return sRunManager;
    }
    
    private PendingIntent getLocationPendingIntent(boolean shouldCreate) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;
        return PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
    }
    
    // Android系统中的地理位置数据是由 LocationManager系统服务提供的。该系统服务向所有需要地理位置数据的应用提供数据更新。
    // 更新数据的传送通常采用两种方式：
    // (1) mLocationManager.requestLocationUpdates(provider, minTime, minDistance, LocationListener l);
    // 使用LocationListener.onLocationChanged(Location)：地理位置更新、状态更新以及定位服务提供者启停状态的通知消息。
    // 如只需将地理位置数据发送给应用中的单个组件，使用LocationListener接口会很方便；
    // 但如果不管用户界面是否存在（如应用在后台运行），应用都需要持续定位用户地理位置时，
    // (2) mLocationManager.requestLocationUpdates(provider, 0, 0, PendingIntent pi);
    // 实际是要求LocationManager在将来某个时点帮忙发送某种类型的Intent;
    // 即使应用组件，甚至是整个应用进程都销毁了，LocationManager仍会一直发送intent，直到要求它停止并按需启动新组件响应它们。
    // 利用这种优势，即使持续进行设备定位，也可以避免应用消耗过多的资源。

    public void startLocationUpdates() {
        // 明确要求LocationManager通过GPS定位装置提供实时的定位数据更新。
        String provider = LocationManager.GPS_PROVIDER;
        
        // If you have the test provider and it's enabled, use it.
        // 使用big nerd ranch提供的TestProvider项目，在真机上使用模拟地址测试定位。
        // https://github.com/bignerdranch/AndroidCourseResources
        if (mLocationManager.getProvider(TEST_PROVIDER) != null &&
                mLocationManager.isProviderEnabled(TEST_PROVIDER)) {
            provider = TEST_PROVIDER;
        }
        Log.d(TAG, "Using provider " + provider);
        
        // Get the last known location and broadcast it if you have one
        // 利用LocationManager的最近一次地理位置（适应于各种定位方式，如GPS、 WIFI网络、手机基站等），
        // 减少等待定位的时间，然后把自己看作是LocationManager，发送一个Intent。
        Location lastKnown = mLocationManager.getLastKnownLocation(provider);
        if (lastKnown != null) {
            lastKnown.setTime(System.currentTimeMillis());
            broadcastLocation(lastKnown);
        }
        
        // Start updates from the location manager
        PendingIntent pi = getLocationPendingIntent(true);
        mLocationManager.requestLocationUpdates(provider, 0, 0, pi);
    }
    
    public void stopLocationUpdates() {
        PendingIntent pi = getLocationPendingIntent(false);
        if (pi != null) {
            mLocationManager.removeUpdates(pi);
            pi.cancel();
        }
    }
    
    public boolean isTrackingRun() {
        return getLocationPendingIntent(false) != null;
    }
    
    private void broadcastLocation(Location location) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        mAppContext.sendBroadcast(broadcast);
    }
    
    public Run startNewRun() {
        // Insert a run into the db
        Run run = insertRun();
        // Start tracking the run
        startTrackingRun(run);
        return run;
    }
    
    public void startTrackingRun(Run run) {
        // Keep the ID
        mCurrentRunId = run.getId();
        // Store it in shared preferences
        mPrefs.edit().putLong(PREF_CURRENT_RUN_ID, mCurrentRunId).commit();
        // Start location updates
        startLocationUpdates();
    }
    
    public void stopRun() {
        stopLocationUpdates();
        mCurrentRunId = -1;
        mPrefs.edit().remove(PREF_CURRENT_RUN_ID).commit();
    }
    
    private Run insertRun() {
        Run run = new Run();
        run.setId(mDatabaseHelper.insertRun(run));
        return run;
    }
    
    public RunCursor queryRuns() {
        return mDatabaseHelper.queryRuns();
    }
    
    public void insertLocation(Location location) {
        if (mCurrentRunId != -1) {
            mDatabaseHelper.insertLocation(mCurrentRunId, location);
        } else {
            Log.e(TAG, "Location received with no tracking run; ignoring.");
        }
    }
}

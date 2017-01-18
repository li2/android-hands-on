package me.li2.android.photogallery.download;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import me.li2.android.photogallery.R;
import me.li2.android.photogallery.model.GalleryItem;
import me.li2.android.photogallery.ui.PhotoGalleryActivity;

public class PollService extends IntentService {
    private static final String TAG = "L_PollService";
    
    private static final int POLL_INTERVAL = 1000 * 60 * 5; // 5 minutes
    public static final String PREF_IS_ALARM_ON = "isAlarmOn";
    
    public static final String ACTION_SHOW_NOTIFICATION = 
            "me.li2.android.photogallery.SHOW_NOTIFICATION";
    // 这个表示权限的字符串，在App中出现3次，另外2次在manifest，必须保证完全一致。
    public static final String PERM_PRIVATE = "me.li2.android.photogallery.PRIVATE";
    
    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Received an intent: " + intent);
        
        // PollService将在后台查询Flickr，为保证后台网络连接的安全性，需要确认网络连接是否可用。
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getBackgroundDataSetting() && 
                cm.getActiveNetworkInfo() != null;
        if (!isNetworkAvailable) {
            Log.w(TAG, "Network isn't available.");
            return;
        }
        
        String query = FlickrFetcher.getSearchQuery(this);
        String lastResultId = FlickrFetcher.getLastResultId(this);
        
        // 使用FlickrFetcher获取最新的图片集。
        List<GalleryItem> items;
        if (query != null) {
            items = new FlickrFetcher().search(query);
        } else {
            items = new FlickrFetcher().fetchItems();
        }
        
        if (items.size() == 0) {
            return;
        }
        
        String resultId = items.get(0).getId();
        
        if (!resultId.equals(lastResultId)) {
            Log.d(TAG, "Got a new result: " + resultId);
            // Service可以通过“通知信息”Notification与用户进行信息沟通。
            Resources r = getResources();
            Intent i = new Intent(this, PhotoGalleryActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            // 使用构造对象创建一个notification
            Notification notification = new NotificationCompat.Builder(this)
                .setTicker(r.getString(R.string.new_pictures_title)) // 首次显示通知信息时，在状态栏上显示的ticker text。
                .setSmallIcon(android.R.drawable.ic_menu_report_image) // ticker text消失后，在状态栏上显示的图标。
                .setContentTitle(r.getString(R.string.new_pictures_title)) // 代表通知信息自身，在通知抽屉中显示的标准视图：
                .setContentText(r.getString(R.string.new_pictures_text)) // 包括：图标、标题、文本。
                .setContentIntent(pi) // 用户点击抽屉中的通知信息，触发PendingIntent
                .setAutoCancel(true) // 被点击后将该消息从消息抽屉中删除
                .build();
            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            // notification的标识符，在整个应用中该值应该是唯一的；
            // 如使用同一ID发送两条消息，则第二条消息会替换掉第一条消息；
            // 这也是进度条或其他动态视觉效果的实现方式。
            notificationManager.notify(0, notification);
            
            // 发送带有权限的的自定义broadcast intent：创建intent，和权限字串，一并传入sendBroadcast()；
            // 这样就指定了接收权限，任何应用必须使用同样的权限才能接收从这儿发出的intent.
            // @receiverPermission: String naming a permission that a receiver must hold
            // in order to receive your broadcast. If null, no permission is required.
            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);
        }
        
        // 把最新图片集的第一条ID保存到shared preferences。
        FlickrFetcher.saveLastResultId(this, resultId);
    }
    
    // 当没有activity在运行时，需通过某种方式在后台执行一些任务。比如说，设置一个5分钟间隔的定时器。
    // 而Handler的sendMessageDelayed(...)或者postDelayed(...)方法，
    // 在用户离开当前应用时，进程就会停止， Handler消息也会随之消亡，不可行。
    // 而使用AlarmManager延迟运行服务。可行。
    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = new Intent(context, PollService.class);        
        // 创建一个启动PollService的PendingIntent。
        // PendingIntent.getService()打包了一个Context.startService()方法的调用。它有四个参数：
        // 用来发送intent的Context、区分pi来源的请求代码、待发送的Intent、一组决定如何创建pi的标志符。
        // 该方法告诉操作系统，“我需要使用startService(Intent) 方法发送这个intent”，
        // 换句话说就是“我想启动PollService”，随后，
        // 调用PendingIntent对象的send()方法时，操作系统会按照我们的要求发送原来封装的intent。
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        
        if (isOn) {
            // Schedule a repeating alarm. 它有四个参数：
            // 描述定时器时间基准的常量、定时器运行的开始时间、定时器循环的时间间隔、到时要执行的pi。
            alarmManager.setRepeating(AlarmManager.RTC, 
                    System.currentTimeMillis(), POLL_INTERVAL, pi);
        } else {
            // 取消Alarm，同时取消PendingIntent.
            alarmManager.cancel(pi);
            pi.cancel();
        }
        
        // 保存定时器开关的状态
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PollService.PREF_IS_ALARM_ON, isOn)
            .apply();
    }
    
    // 由于在取消Alarm的同时也取消了pi，并且一个PendingIntent只能登记给一个Alarm，
    // 所以可通过检查pi是否存在，来确认Alarm是否激活。
    public static boolean isServiceAlarmOn(Context context) {
        Intent i = new Intent(context, PollService.class);
        // FLAG_NO_CREATE表示如果描述的pi不存在，则返回null，而不是创建它。
        PendingIntent pi = PendingIntent.getService(
                context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }
}

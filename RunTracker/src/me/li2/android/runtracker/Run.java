package me.li2.android.runtracker;

import java.util.Date;
import java.util.Locale;

public class Run {
    // 为了从数据库中查询Run，为其添加ID属性，为插入数据库时返回的长整型值。
    private long mId;
    private Date mStartDate;
    
    public Run() {
        mId = -1;
        mStartDate = new Date();
    }
    
    public long getId() {
        return mId;
    }
    
    public void setId(long id) {
        mId = id;
    }
    
    public Date getStartDate() {
        return mStartDate;
    }
    
    public void setStartDate(Date startDate) {
        mStartDate = startDate;
    }
    
    public int getDurationSeconds(long endMillis) {
        return (int)((endMillis - mStartDate.getTime()) / 1000);
    }
    
    public static String formatDuration(int durationSeconds) {
        // SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        // return sdf.format(new Date(durationSeconds * 1000));
        int seconds = durationSeconds % 60;
        int minutes = ((durationSeconds - seconds) / 60) % 60;
        int hours = (durationSeconds - (minutes * 60) - seconds) / 3600;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }
}

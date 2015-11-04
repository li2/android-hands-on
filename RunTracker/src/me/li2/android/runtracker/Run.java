package me.li2.android.runtracker;

import java.util.Date;
import java.util.Locale;

public class Run {
    private Date mStartDate;
    
    public Run() {
        mStartDate = new Date();
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

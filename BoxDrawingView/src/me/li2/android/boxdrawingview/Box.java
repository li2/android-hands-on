package me.li2.android.boxdrawingview;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

// 新建一个Box类，用于存储一个矩形框两个角的坐标：
// 原始坐标点（手指的初始位置）、当前坐标点（手指的当前位置）。

public class Box implements Parcelable {
    private PointF mOrigin;
    private PointF mCurrent;
    
    public Box(PointF origin) {
        mOrigin = mCurrent = origin;
    }
    
    public PointF getCurrent() {
        return mCurrent;
    }
    
    public void setCurrent(PointF current) {
        mCurrent = current;
    }
    
    public PointF getOrigin() {
        return mOrigin;
    }

    // public interface Parcelable
    // Interface for classes whose instances can be written to and restored from a Parcel.
    // http://developer.android.com/intl/zh-cn/reference/android/os/Parcelable.html

    // Parcelable接口允许类的实例可以被写入Parcel，并从中恢复。
    // 然后我们就可以在View.onSaveInstanceState()方法中存储 ArrayList of Objects: mBoxes，调用
    // Bundle.putParcelableArrayList(String key, ArrayList<? extends Parcelable> value)
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(mOrigin);
        out.writeValue(mCurrent);
    }
    
    public static final Parcelable.Creator<Box> CREATOR = new Parcelable.Creator<Box>() {
        @Override
        public Box createFromParcel(Parcel in) {
            return new Box(in);
        }

        @Override
        public Box[] newArray(int size) {
            return new Box[size];
        }
    };
    
    private Box(Parcel in) {
        mOrigin = (PointF) in.readValue(null);
        mCurrent = (PointF) in.readValue(null);
    }
}

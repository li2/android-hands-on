package me.li2.android.boxdrawingview;

import android.graphics.PointF;

// 新建一个Box类，用于存储一个矩形框两个角的坐标：
// 原始坐标点（手指的初始位置）、当前坐标点（手指的当前位置）。
public class Box {
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
}

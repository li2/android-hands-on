package me.li2.android.runtracker;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class DataLoader<D> extends AsyncTaskLoader<D> {
    // 使用D泛型类存储加载的数据实例。
    private D mData;
    
    public DataLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        // 检查数据是否已加载，如已加载则立即发送；否则就调用超类的forceLoad()去获取数据。
        if (mData != null) {
            deliverResult(mData);
        } else {
            forceLoad();
        }
    }
    
    @Override
    public void deliverResult(D data) {
        // 先将新数据对象存储起来，然后如果loader已启动，则调用超类版本的方法将数据发送出去。
        mData = data;
        if (isStarted()) {
            super.deliverResult(data);
        }
    }
}

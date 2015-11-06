package me.li2.android.runtracker;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

/*
`SQLiteCursorLoader` 类继承自 `AsyncTaskLoader<D>` 类，
AsyncTaskLoader 是一个抽象 Loader。它使用 AsyncTask 将数据加载任务转移到其他线程处理。
几乎所有我们创建的有用loader类都是AsyncTaskLoader的一个子类。

AsyncTaskLoader 类在内部定义了一个继承自 `ModernAsyncTask` 的子类 `LoadTask`，
然后实例化了一个内部成员 `LoadTask mTask` 来管理异步任务：

1. `doInBackground` 调用了 `AsyncTaskLoader.onLoadInBackground()`，继而调用了抽象方法`D loadInBackground()`
所以需要在 `SQLiteCursorLoader` 类中覆写loadInBackground，以加载数据；
2. 数据加载完后，`onPostExecute(D data)`得以在UI线程中执行，它调用了 `dispatchOnLoadComplete(this, data)`;
继续调用 deliverResult(data);
继续调用 mListener.onLoadComplete(this, data);
这个接口是什么呢？

LoaderManager内部定义了一个类LoaderInfo，它实现了Loader.OnLoadCompleteListener<Object>,
LoaderInfo.onLoadComplete(Loader<Object> loader, Object data) {
    callOnLoadFinished(loader, data);
}

LoaderManager.callOnLoadFinished(Loader<Object> loader, Object data) {
    mCallbacks.onLoadFinished(loader, data); // 这是需要覆写的一个接口方法。
}
*/

public abstract class SQLiteCursorLoader extends AsyncTaskLoader<Cursor> {
    private Cursor mCursor;
    
    public SQLiteCursorLoader(Context context) {
        super(context);
    }
    
    protected abstract Cursor loadCursor();
    
    // 
    // D AsyncTask.doInBackground(Void... params)调用 AsyncTaskLoader.onLoadInBackground(),
    // 继而调用 public abstract D loadInBackground(); 覆写这个方法，以在后台线程加载数据。
    @Override
    public Cursor loadInBackground() {
        Cursor cursor = loadCursor();
        if (cursor != null) {
            // Ensure that the content window is filled
            cursor.getCount();
        }
        return cursor;
    }
    
    // void AsyncTask.onPostExecute(D data)调用AsyncTaskLoader.dispatchOnLoadComplete(this, data);
    // 继而调用Loader.deliverResult(data).
    @Override
    public void deliverResult(Cursor data) {
        Cursor oldCursor = mCursor;
        mCursor = data;
        
        if (isStarted()) {
            super.deliverResult(data);
        }
        
        if (oldCursor != null && oldCursor != data && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }
    
    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }
    
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible
        cancelLoad();
    }
    
    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
    
    @Override
    protected void onReset() {
        super.onReset();
        
        // Ensure the loader is stopped
        onStopLoading();
        
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }
}

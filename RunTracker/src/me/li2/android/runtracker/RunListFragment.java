package me.li2.android.runtracker;

import me.li2.android.runtracker.RunDatabaseHelper.RunCursor;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RunListFragment extends ListFragment {

    private RunCursor mCursor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Query the list of runs
        mCursor = RunManager.get(getActivity()).queryRuns();
        // Create an adapter to point at this cursor
        RunCursorAdapter adapter = new RunCursorAdapter(getActivity(), mCursor);
        setListAdapter(adapter);
    }
    
    @Override
    public void onDestroy() {
        mCursor.close();
        super.onDestroy();
    }
    
    // CursorAdapter: adapter that exposes data from a Cursor to a ListView widget.
    // 如果不能将数据提供给关联着RunListFragment的ListView，仅有RunCursor用处也不大，
    // 而CursorAdapter类恰好可以解决这一问题。
    private static class RunCursorAdapter extends CursorAdapter {

        private RunCursor mRunCursor;
        
        public RunCursorAdapter(Context context, RunCursor cursor) {
            super(context, cursor, 0);
            mRunCursor = cursor;
        }
        
        // Makes a new view to hold the data pointed to by cursor.
        // 返回一个代表cursor中当前数据行的View.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // Use a layout inflater to get a row view
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        
        // Bind an existing view to the data pointed to by cursor.
        // 当需要配置视图显示 cursor中的一行数据时， CursorAdapter 将调用 bindView()。
        // 该方法需要的View参数应该总是前面newView()方法返回的View。
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Get the run for the current row
            Run run = mRunCursor.getRun();
            
            // Set up the start date text view
            TextView startDateTextView = (TextView) view;
            String cellText = context.getString(R.string.cell_text, run.getStartDate());
            startDateTextView.setText(cellText);
        }
    }
}

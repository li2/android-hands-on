package me.li2.android.runtracker;

import me.li2.android.runtracker.RunDatabaseHelper.RunCursor;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class RunListFragment extends ListFragment {
    private static final int REQUEST_NEW_RUN = 0;
    
    private RunCursor mCursor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    
    @Override
    public void onResume() {
        super.onResume();
        ((RunCursorAdapter)getListAdapter()).notifyDataSetChanged();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.run_list_options, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_new_run:
            Intent i = new Intent(getActivity(), RunActivity.class);
            startActivityForResult(i, REQUEST_NEW_RUN);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_NEW_RUN == requestCode) {
            mCursor.requery();
            ((RunCursorAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }
    
    // 在RunListFragment中响应列表项选择，使用已选旅程ID启动RunActivity
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // The id argument will be the Run ID; CursorAdapter gives up this for free
        // 因为我们指定了run_id表中的ID字段， CursorAdapter检测到该字段并将其作为id参数传递给了onListItemClick().
        // The Cursor must include a column named "_id" or CursorAdapter will not work.
        // 所以此处的 @id 就是run的id
        Intent intent = new Intent(getActivity(), RunActivity.class);
        intent.putExtra(RunActivity.EXTRA_RUN_ID, id);
        startActivity(intent);
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
            
            // 通过区别于其它列表项的颜色来识别当前跟踪的旅程。
            boolean trackingThisRun = RunManager.get(context).isTrackingRun(run);
            if (trackingThisRun) {
                view.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
            } else {
                view.setBackgroundResource(0);
            }
        }
    }
}

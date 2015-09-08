package me.li2.android.criminalintent;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class CrimeListFragment extends ListFragment{
    private static final String TAG = "CrimeListFragment";
    private static final int REQUEST_CRIME = 1;
    private ArrayList<Crime> mCrimes;
    private boolean mSubtitleVisible;
    private Callbacks mCallbacks;
    
    // Required interface for hosting activities.
    public interface Callbacks {
        void onCrimeSelected(Crime crime);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // let the FragmentManager know that CrimeListFragment needs to receive options menu callbacks.
        setHasOptionsMenu(true);
        
        getActivity().setTitle(R.string.crimes_title);
        mCrimes = CrimeLab.get(getActivity()).getCrimes();
        
        CrimeAdapter adapter = new CrimeAdapter(mCrimes);
        setListAdapter(adapter);
        
        setRetainInstance(true);
        mSubtitleVisible = false;
    }
    
    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
       View v = super.onCreateView(inflater, parent, savedInstanceState);
       
       if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
           if (mSubtitleVisible) {
               getActivity().getActionBar().setSubtitle(R.string.subtitle);
           }
       }
       
       // 使用android.R.id.list资源ID获取ListFragment管理着的ListView
       ListView listView = (ListView) v.findViewById(android.R.id.list);
       if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
           // Use floating context menus on Froyo and Gingerbread
           // By default, a long-press on a view does not trigger the creation of a context menu.
           // Must register a view for a floating context menu by calling the following Fragment method:
           registerForContextMenu(listView);
       } else {
           // Use contextual action bar on Honeycomb and higher
           // 所谓Contextual Action Mode(上下文操作模式)，菜单项会覆盖操作栏.
           listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
           listView.setMultiChoiceModeListener(mMultiChoiceClickListener);
       }
       return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        ((CrimeAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // 在fragment附加给activity时，将托管activity强制类型转换为Callbacks对象并赋值给Callbacks类型变量。
        // 强制转换而未经类安全性检查，所以托管activity必须实现CrimeListFragment.Callbacks接口。
        mCallbacks = (Callbacks)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void updateUI() {
        ((CrimeAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Crime crime = ((CrimeAdapter) l.getAdapter()).getItem(position);
        // 定义fragment的Callbacks，委托fragment的“托管activity”完成具体的工作，
        // 以此保证fragment作为独立的开发构件，而不必知道其“托管activity”是如何工作的。
        // “托管activity”要做的事情是：
        // 针对平板，把CrimeFragment添加到detailFragmentContainer；针对手机，把启动CrimePagerActivity.
        mCallbacks.onCrimeSelected(crime);
        // 因此需要删除以下代码。
        // Start CrimePagerActivity with this crime
        // Intent i = new Intent(getActivity(), CrimePagerActivity.class);
        // i.putExtra(CrimeFragment.EXTRA_CRIME_ID, c.getId());
        // startActivityForResult(i, REQUEST_CRIME);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CRIME) {
            // Handle result
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);
        MenuItem showSubtitle = menu.findItem(R.id.menu_item_show_subtitle);
        if (mSubtitleVisible && showSubtitle != null) {
            showSubtitle.setTitle(R.string.hide_subtitle);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_new_crime:
            Crime crime = new Crime();
            CrimeLab.get(getActivity()).addCrime(crime);
            ((CrimeAdapter)getListAdapter()).notifyDataSetChanged();
            mCallbacks.onCrimeSelected(crime);
            return true;
        case R.id.menu_item_show_subtitle:
            if (getActivity().getActionBar().getSubtitle() == null) {
                getActivity().getActionBar().setSubtitle(R.string.subtitle);
                mSubtitleVisible = true;
                item.setTitle(R.string.hide_subtitle);
            } else {
                getActivity().getActionBar().setSubtitle(null);
                mSubtitleVisible = false;
                item.setTitle(R.string.show_subtitle);
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.crime_list_item_context, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        CrimeAdapter adapter = (CrimeAdapter) getListAdapter();
        Crime crime = adapter.getItem(position);
        
        switch (item.getItemId()) {
        case R.id.menu_item_delete_crime:
            CrimeLab.get(getActivity()).deleteCrime(crime);
            CrimeLab.get(getActivity()).saveCrimes();
            // The content of the adapter has changed but ListView did not receive a notification.
            // Make sure the content of your adapter is not modified from a background thread, but only from the UI thread.
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onContextItemSelected(item);
    }
    
    private MultiChoiceModeListener mMultiChoiceClickListener = new MultiChoiceModeListener() {
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
            // Required, but not used in this implementation
        }
        
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Required, but not used in this implementation
        }
        
        @Override
        // ActionMode Callback methods
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.crime_list_item_context, menu);
            return true;
        }
        
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
            case R.id.menu_item_delete_crime:
                CrimeAdapter adapter = (CrimeAdapter) getListAdapter();
                CrimeLab crimeLab = CrimeLab.get(getActivity());
                for (int i = adapter.getCount() - 1; i >= 0; i--) {
                    if (getListView().isItemChecked(i)) {
                        crimeLab.deleteCrime(adapter.getItem(i));
                    }
                }
                crimeLab.saveCrimes();
                mode.finish();
                adapter.notifyDataSetChanged();
                return true;
            default:
                return false;
            }
        }
        
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            // Required, but not used in this implementation
        }
    };
    
    private class CrimeAdapter extends ArrayAdapter<Crime> {
        public CrimeAdapter(ArrayList<Crime> crimes) {
            super(getActivity(), 0, crimes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If we weren't given a view, inflate one
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_crime, null);
            }
            
            // Configure the view for this crime
            Crime c = getItem(position);
            
            TextView titleTextView = (TextView) convertView.findViewById(R.id.crime_list_item_titleTextView);
            titleTextView.setText(c.getTitle());
            
            TextView dateTextView = (TextView) convertView.findViewById(R.id.crime_list_item_dateTextView);
            dateTextView.setText(c.getDate().toString());
            
            CheckBox solvedCheckBox = (CheckBox) convertView.findViewById(R.id.crime_list_item_solvedCheckBox);
            solvedCheckBox.setChecked(c.isSolved());
            
            return convertView;
        }
    }    
}

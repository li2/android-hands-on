package me.li2.android.criminalintent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

public class CrimeListFragment extends Fragment {
    private static final String TAG = "CrimeListFragment";
    private static final int REQUEST_CRIME = 1;
    private List<Crime> mCrimes;
    private boolean mSubtitleVisible;
    private Callbacks mCallbacks;
    private CrimeAdapter mCrimeAdapter;
    private RecyclerView mCrimeRecyclerView;
    
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

        setRetainInstance(true);
        mSubtitleVisible = false;
    }
    
    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_crime_list, parent, false);
       
       if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
           if (mSubtitleVisible) {
               getActivity().getActionBar().setSubtitle(R.string.subtitle);
           }
       }

       mCrimeRecyclerView = (RecyclerView) view.findViewById(R.id.crime_recycler_view);
       mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

       updateUI();

       return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
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
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        if (mCrimeAdapter == null) {
            mCrimeAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mCrimeAdapter);
        } else {
            mCrimeAdapter.notifyDataSetChanged();
        }
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
            ((CrimeAdapter)mCrimeRecyclerView.getAdapter()).notifyDataSetChanged();
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
    
    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {
        private List<Crime> mCrimes;
        public CrimeAdapter(List<Crime> crimes) {
            mCrimes = crimes;
        }

        @Override
        public CrimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_crime, parent, false);
            return new CrimeHolder(view);
        }

        @Override
        public void onBindViewHolder(CrimeHolder holder, int position) {
            Crime crime = mCrimes.get(position);
            holder.bindCrime(crime);
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;

        private TextView mTitleTextView;
        private TextView mDateTextView;
        private CheckBox mSolvedCheckBox;

        public CrimeHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.crime_list_item_titleTextView);
            mDateTextView = (TextView) itemView.findViewById(R.id.crime_list_item_dateTextView);
            mSolvedCheckBox = (CheckBox) itemView.findViewById(R.id.crime_list_item_solvedCheckBox);
        }

        public void bindCrime(Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(crime.getTitle());
            mDateTextView.setText(crime.getDate().toString());
            mSolvedCheckBox.setChecked(crime.isSolved());
        }

        @Override
        public void onClick(View view) {
            // 定义fragment的Callbacks，委托fragment的“托管activity”完成具体的工作，
            // 以此保证fragment作为独立的开发构件，而不必知道其“托管activity”是如何工作的。
            // “托管activity”要做的事情是：
            // 针对平板，把CrimeFragment添加到detailFragmentContainer；针对手机，把启动CrimePagerActivity.
            mCallbacks.onCrimeSelected(mCrime);
            // 因此需要删除以下代码。
            // Start CrimePagerActivity with this crime
            // Intent i = new Intent(getActivity(), CrimePagerActivity.class);
            // i.putExtra(CrimeFragment.EXTRA_CRIME_ID, c.getId());
            // startActivityForResult(i, REQUEST_CRIME);
        }
    }
}

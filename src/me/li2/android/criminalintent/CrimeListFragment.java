package me.li2.android.criminalintent;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CrimeListFragment extends ListFragment{
    private static final String TAG = "CrimeListFragment";
    private ArrayList<Crime> mCrimes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.crimes_title);
        mCrimes = CrimeLab.get(getActivity()).getCrimes();
        
        ArrayAdapter<Crime> adapter =
                new ArrayAdapter<Crime>(getActivity(), android.R.layout.simple_list_item_1, mCrimes);
        setListAdapter(adapter);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Crime c = (Crime) l.getAdapter().getItem(position);
        Log.d(TAG, c.getTitle() + " was clicked");
    }
}

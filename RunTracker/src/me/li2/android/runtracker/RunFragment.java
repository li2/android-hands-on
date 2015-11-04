package me.li2.android.runtracker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class RunFragment extends Fragment {

    private Button mStartBtn, mStopBtn;
    private TextView mStartedTv, mLatitudeTv, mLongitudeTv, mAltitudeTv, mDurationTv;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_run, container, false);
        
        mStartedTv = (TextView) v.findViewById(R.id.run_startedTextView);
        mLatitudeTv = (TextView) v.findViewById(R.id.run_latitudeTextView);
        mLongitudeTv = (TextView) v.findViewById(R.id.run_longitudeTextView);
        mAltitudeTv = (TextView) v.findViewById(R.id.run_altitudeTextView);
        mDurationTv = (TextView) v.findViewById(R.id.run_durationTextView);
        
        mStartBtn = (Button) v.findViewById(R.id.run_startButton);
        
        mStopBtn = (Button) v.findViewById(R.id.run_stopButton);        
        
        return v;
    }
}

package me.li2.android.runtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RunFragment extends Fragment {

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location loc) {
            mLastLocation = loc;
            if (isVisible()) {
                updateUI();
            }
        };
        
        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        };
    };
    
    private Run mRun;
    private Location mLastLocation;
    private RunManager mRunManager;
    
    private Button mStartBtn, mStopBtn;
    private TextView mStartedTv, mLatitudeTv, mLongitudeTv, mAltitudeTv, mDurationTv;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mRunManager = RunManager.get(getActivity());
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
        mStartBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mRun = mRunManager.startNewRun();
                updateUI();
            }
        });
        
        mStopBtn = (Button) v.findViewById(R.id.run_stopButton);
        mStopBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mRunManager.stopRun();
                updateUI();
            }
        });
        
        return v;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mLocationReceiver, new IntentFilter(RunManager.ACTION_LOCATION));
    }
    
    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mLocationReceiver);
        super.onStop();
    }
    
    private void updateUI() {
        boolean started = mRunManager.isTrackingRun();
        
        if (mRun != null) {
            mStartedTv.setText(mRun.getStartDate().toString());
        }
        
        int durationSeconds = 0;
        if (mRun != null && mLastLocation != null) {
            durationSeconds = mRun.getDurationSeconds(mLastLocation.getTime());
            mLatitudeTv.setText(Double.toString(mLastLocation.getLatitude()));
            mLongitudeTv.setText(Double.toString(mLastLocation.getLongitude()));
            mAltitudeTv.setText(Double.toString(mLastLocation.getAltitude()));
        }
        mDurationTv.setText(Run.formatDuration(durationSeconds));
        
        mStartBtn.setEnabled(!started);
        mStopBtn.setEnabled(started);
    }
}

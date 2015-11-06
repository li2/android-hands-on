package me.li2.android.runtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RunFragment extends Fragment {
    private static final String ARG_RUN_ID = "RUN_ID";
    private static final int LOAD_RUN = 0;
    private static final int LOAD_LOCATION = 1;

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {
        @Override
        protected void onLocationReceived(Context context, Location loc) {
            if (!mRunManager.isTrackingRun(mRun)) {
                return;
            }
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
    
    private Button mStartBtn, mStopBtn, mMapBtn;
    private TextView mStartedTv, mLatitudeTv, mLongitudeTv, mAltitudeTv, mDurationTv;
    
    public static RunFragment newInstance(long runId) {
        Bundle args = new Bundle();
        args.putLong(ARG_RUN_ID, runId);
        RunFragment fragment = new RunFragment();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mRunManager = RunManager.get(getActivity());
        
        // Check for a Run ID as an argument, and find the run
        Bundle args = getArguments();
        if (args != null) {
            long runId = args.getLong(ARG_RUN_ID, -1);
            if (runId != -1) {
                // 从数据库中加载当前旅程的最近一次地理位置信息，以在界面上显示。
//              mRun = mRunManager.getRun(runId);
//              mLastLocation = mRunManager.getLatestLocation(runId);
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_RUN, args, new RunLoaderCallbacks());
                lm.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
            }
        }
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
                if (mRun == null) {
                    mRun = mRunManager.startNewRun();
                } else {
                    mRunManager.startTrackingRun(mRun);
                }
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
        
        mMapBtn = (Button) v.findViewById(R.id.run_mapButton);
        mMapBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), RunMapActivity.class);
                i.putExtra(RunMapActivity.EXTRA_RUN_ID, mRun.getId());
                startActivity(i);
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
        boolean trackingThisRun = mRunManager.isTrackingRun(mRun);
        
        if (mRun != null) {
            mStartedTv.setText(mRun.getStartDate().toString());
        }
        
        int durationSeconds = 0;
        if (mRun != null && mLastLocation != null) {
            durationSeconds = mRun.getDurationSeconds(mLastLocation.getTime());
            mLatitudeTv.setText(Double.toString(mLastLocation.getLatitude()));
            mLongitudeTv.setText(Double.toString(mLastLocation.getLongitude()));
            mAltitudeTv.setText(Double.toString(mLastLocation.getAltitude()));
            mMapBtn.setEnabled(true);
        } else {
            mMapBtn.setEnabled(false);
        }
        mDurationTv.setText(Run.formatDuration(durationSeconds));
        
        mStartBtn.setEnabled(!started);
        mStopBtn.setEnabled(started && trackingThisRun);
    }
    
    private class RunLoaderCallbacks implements LoaderCallbacks<Run> {
        @Override
        public Loader<Run> onCreateLoader(int id, Bundle args) {
            return new RunLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Run> loader, Run run) {
            mRun = run;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Run> loader) {
            // Do nothing
        }
    }
    
    private class LocationLoaderCallbacks implements LoaderCallbacks<Location> {
        @Override
        public Loader<Location> onCreateLoader(int id, Bundle args) {
            return new LastLocationLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Location> loader, Location location) {
            mLastLocation = location;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Location> loader) {
            // Do nothing
        }
    }
}

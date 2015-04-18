package me.li2.android.criminalintent;

import java.util.ArrayList;
import java.util.UUID;

import android.content.Context;
import android.util.Log;

public class CrimeLab {
    private static final String TAG = "CrimeLab";
    private static final String FILENAME = "crimes.json";
    
    private CriminalIntentJSONSerializer mSerializer;
    
    // Singletons and centralized data storage
    private static CrimeLab sCrimeLab;
    private ArrayList<Crime> mCrimes; 
    private Context mAppContex;
    
    private CrimeLab(Context appContext) {
        mAppContex = appContext;
        mSerializer = new CriminalIntentJSONSerializer(mAppContex, FILENAME);
        
        try {
            mCrimes = mSerializer.loadCrimes();
        } catch (Exception e) {
            mCrimes = new ArrayList<Crime>();
            Log.e(TAG, "Error loading crimes: ", e);
        }
    }
    
    // Setting up the singleton
    public static CrimeLab get(Context c) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(c.getApplicationContext());
        }
        return sCrimeLab;
    }
    
    public void addCrime(Crime c) {
        mCrimes.add(c);
    }
    
    public boolean saveCrimes() {
        try {
            mSerializer.saveCrimes(mCrimes);
            Log.d(TAG, "crimes saved to file");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving crimes: ", e);
            return false;
        }
    }
    
    public ArrayList<Crime> getCrimes() {
        return mCrimes;
    }
    
    public Crime getCrime(UUID id) {
        for (Crime c : mCrimes) {
            if (c.getId() .equals(id)) {
                return c;
            }
        }
        return null;
    }
}

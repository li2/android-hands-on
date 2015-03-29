package me.li2.android.criminalintent;

import java.util.ArrayList;
import java.util.UUID;

import android.content.Context;

public class CrimeLab {
    // Singletons and centralized data storage
    private static CrimeLab sCrimeLab;
    private ArrayList<Crime> mCrimes; 
    private Context mAppContex;
    
    private CrimeLab(Context appContext) {
        mAppContex = appContext;
        mCrimes = new ArrayList<Crime>();
        
        for (int i=0; i<100; i++) {
            Crime c = new Crime();
            c.setTitle("Crime #" + i);
            c.setSolved(i % 2 == 0);
            mCrimes.add(c);
        }
    }
    
    // Setting up the singleton
    public static CrimeLab get(Context c) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(c.getApplicationContext());
        }
        return sCrimeLab;
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

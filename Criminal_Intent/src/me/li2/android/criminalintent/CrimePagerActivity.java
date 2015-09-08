package me.li2.android.criminalintent;

import java.util.ArrayList;
import java.util.UUID;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

public class CrimePagerActivity  extends FragmentActivity 
    implements CrimeFragment.Callbacks {
    
    private ViewPager mViewPager;
    private ArrayList<Crime>mCrimes;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.viewPager);
        setContentView(mViewPager);
        
        mCrimes = CrimeLab.get(this).getCrimes();
        
        FragmentManager fm = getSupportFragmentManager();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fm) {
            @Override
            public int getCount() {
                return mCrimes.size();
            }
            
            @Override
            // 因为需要返回Fragment（用于构建activity），所以在构建adapter时，还需传入FragmentManager给它的构造方法。
            public Fragment getItem(int pos) {
                Crime crime = mCrimes.get(pos);
                return CrimeFragment.newInstance(crime.getId());
            }
        });
        
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int pos) {
                Crime crime = mCrimes.get(pos);
                if (crime.getTitle() != null) {
                    setTitle(crime.getTitle());
                }
            }
            
            @Override
            public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) { }
            
            @Override
            public void onPageScrollStateChanged(int state) {}
        });
        
        UUID crimeId = (UUID) getIntent().getSerializableExtra(CrimeFragment.EXTRA_CRIME_ID);
        for (int i=0; i<mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(crimeId)) {
                mViewPager.setCurrentItem(i);
                break;
            }
        }
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        // 因为CrimePagerActivity托管了CrimeFragment，所以必须要实现其callbacks，
        // 而对于单版界面，列表的刷新已经在 CrimeListFragment.onResume()中完成了，
        // 所以这个方法留空即可。
    }
}

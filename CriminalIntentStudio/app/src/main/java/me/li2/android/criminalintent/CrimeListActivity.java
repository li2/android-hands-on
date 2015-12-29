package me.li2.android.criminalintent;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class CrimeListActivity extends SingleFragmentActivity
    implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {
    private static final String TAG = "CrimeListActivity";

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }
    
    @Override
    protected int getLayoutResId() {
        // 继承了这个接口，而 activity_masterdetail 是资源别名；
        // 根据屏幕大小，和资源文件的限定词（values, values-sw600dp），就可以在运行时决定填充哪个layout。
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onCrimeSelected(Crime crime) {
        // 当 Fragment中的 list item 被点击后，由托管的 Activity 负责决定做的事情，因为它知道是手机还是平板：
        if (findViewById(R.id.detailFragmentContainer) == null) {
            // Start an instance of CrimePagerActivity.
            Intent intent = new Intent(this, CrimePagerActivity.class);
            intent.putExtra(CrimeFragment.EXTRA_CRIME_ID, crime.getId());
            startActivity(intent);
        } else {
            Fragment newDetail = CrimeFragment.newInstance(crime.getId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detailFragmentContainer, newDetail)
                    .commit();
        }
    }

    @Override
    // 之所以需要定义并实现这个接口，是为了当 list-detail 的detail界面更新时，需要立刻更新list界面。
    public void onCrimeUpdated(Crime crime) {
        FragmentManager fm = getSupportFragmentManager();
        CrimeListFragment listFragment = (CrimeListFragment)fm.findFragmentById(R.id.fragmentContainer);
        listFragment.updateUI();
    }
}
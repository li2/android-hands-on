package me.li2.android.criminalintent;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;

public class DatePickerFragment extends DialogFragment {
    public static final String EXTRA_DATE = "me.li2.android.criminalintent.date";
    
    private Date mDate;
    
    public static DatePickerFragment newInstance(Date date) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DATE, date);
        
        DatePickerFragment fragment = new DatePickerFragment();
        fragment.setArguments(args);
        
        return fragment;
    }
    
    private void sendResult(int resultCode) {
        if (getTargetFragment() == null) {
            return;
        }
        
        Intent i = new Intent();
        i.putExtra(EXTRA_DATE, mDate);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDate = (Date) getArguments().getSerializable(EXTRA_DATE);
        
        // Create a Calendar to get the year, month, and day
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        // 虽然DatePicker可以直接生成视图，但使用布局文件容易修改对话框的显示内容
        // DatePicker v = new DatePicker(getActivity());
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_date, null);

        DatePicker datePicker = (DatePicker) v.findViewById(R.id.dialog_date_datePicker);
        datePicker.init(year, month, day, new OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Translate year, month, day into a Date object using a calendar
                mDate = new GregorianCalendar(year, monthOfYear, dayOfMonth).getTime();
                // Update argument to preserve selected value on rotation
                // 为防止设备旋转时发生Date数据的丢失，将数据写到fragment argument中，
                // 如发生设备旋转，那么FragmentManager会销毁当前实例并创建一个新的实例，
                // FragmentManager会调用它的onCreateDialog(...)方法，这样新实例便可从argument中获得保存的日期数据，
                // 这比onSaveInstanceState(...)方法保存状态要简单。
                getArguments().putSerializable(EXTRA_DATE, mDate);
            }
        });
        
        return new AlertDialog.Builder(getActivity())
            .setView(v)
            .setTitle(R.string.date_picker_title)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendResult(Activity.RESULT_OK);
                }
            })
            .create();
    }

}

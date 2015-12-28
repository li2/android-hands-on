package me.li2.android.criminalintent;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DatePickerFragment extends DialogFragment {
    public static final String EXTRA_DATE = "me.li2.android.criminalintent.date";

    private Date mDate;
    private DatePicker mDatePicker;
    
    public static DatePickerFragment newInstance(Date date) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DATE, date);

        DatePickerFragment fragment = new DatePickerFragment();
        fragment.setArguments(args);
        
        return fragment;
    }
    
    private void sendResult(int resultCode, Date date) {
        if (getTargetFragment() == null) {
            return;
        }
        
        Intent i = new Intent();
        i.putExtra(EXTRA_DATE, date);
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

        mDatePicker = (DatePicker) v.findViewById(R.id.dialog_date_datePicker);
        mDatePicker.init(year, month, day, null);

        return new AlertDialog.Builder(getActivity())
            .setView(v)
            .setTitle(R.string.date_picker_title)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int year = mDatePicker.getYear();
                    int month = mDatePicker.getMonth();
                    int day = mDatePicker.getDayOfMonth();
                    Date date = new GregorianCalendar(year, month, day).getTime();
                    sendResult(Activity.RESULT_OK, date);
                }
            })
            .create();
    }
}

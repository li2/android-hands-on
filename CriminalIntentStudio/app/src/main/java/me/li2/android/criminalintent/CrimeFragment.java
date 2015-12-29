package me.li2.android.criminalintent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.Date;
import java.util.UUID;

/*
 * CrimeFragment is a controller that interacts with model and view objects. 
 * Its job is to present the details of a specific crime and update those details as the user changes them.
 */
public class CrimeFragment extends Fragment {
    private static final String TAG = "CrimeFragment";
    public static final String EXTRA_CRIME_ID = "me.li2.android.criminalintent.crime_id";
    
    private static final String DIALOG_DATE = "date";
    private static final String DIALOG_IMAGE = "image";
    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_PHOTO = 1;
    private static final int REQUEST_CONTACT = 2;
    
    private Crime mCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckBox;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private Button mReportButton;
    private Button mSuspectButton;
    private Callbacks mCallbacks;
    
    // Required interface for hosting activities.
    public interface Callbacks {
        void onCrimeUpdated(Crime crime);
    }
    
    public static CrimeFragment newInstance(UUID crimeId) {
        // TODO
        // 如果复用fragment，而不是重建，那么应该怎么传递数据呢？
        // argument已经在实例化时设置过了，再次设置将会导致exception。
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_CRIME_ID, crimeId);
        
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    // Configure the fragment instance.
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        UUID crimeId = (UUID) getArguments().getSerializable(EXTRA_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
    }

    @TargetApi(11)
    @Override
    // Create and configure the fragment's view.
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState) {
        // Inflate the layout for the fragment’s view and return the inflated View to the hosting activity.
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        // Wiring widgets in a fragment.
        mTitleField  = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
                mCallbacks.onCrimeUpdated(mCrime);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,  int after) {
                // This space intentionally left blank
            }
            @Override
            public void afterTextChanged(Editable s) {
                // This one too
            }
        });
        
        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(fm, DIALOG_DATE);
            }
        });
        
        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
                mCallbacks.onCrimeUpdated(mCrime);
            }
        });
        
        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_imageButton);
        mPhotoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CrimeCameraActivity.class);
                startActivityForResult(intent, REQUEST_PHOTO);
            }
        });
        
        mPhotoView = (ImageView) v.findViewById(R.id.crime_imageView);
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Photo p = mCrime.getPhoto();
                if (p == null) {
                    return;
                }
                
                FragmentManager fm = getActivity().getSupportFragmentManager();
                String path = getActivity().getFileStreamPath(p.getFilename()).getAbsolutePath();
                ImageFragment.newInstance(path).show(fm, DIALOG_IMAGE);
            }
        });
        
        // If camera is not available, disable camera functionality
        PackageManager pm = getActivity().getPackageManager();
        boolean hasACamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) ||
                Camera.getNumberOfCameras() > 0;
        if (!hasACamera) {
            mPhotoButton.setEnabled(false);
        }
        
        mReportButton = (Button) v.findViewById(R.id.crime_reportButton);
        mReportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // the action that you are trying to perform
                // 任务是发送一段文字信息，因此，隐式intent的操作是ACTION_SEND
                Intent i = new Intent(Intent.ACTION_SEND);
                
                // the type of data that the action is for，
                // 不指向任何数据，也不包含任何类别， 但会指定数据类型为text/plain
                i.setType("text/plain");
                
                // 报告文本以及报告主题字符串作为extra附加到intent上
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
                
                // 若看不到候选activity列表，原因有2：针对某个隐式intent设置了默认响应应用，
                // 要么是设备上只有唯一一个activity可以响应隐式intent。
                // 为了将选择权交给用户，创建一个每次都显示的activity选择器。
                i = Intent.createChooser(i, getString(R.string.send_report));
//                startActivity(i);

                // IntentBuilder 是构造 ACTION_SEND intent 的便利类，method-chaining 方式。
                ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getCrimeReport())
                        .setSubject(getString(R.string.crime_report_subject))
                        .setChooserTitle(getString(R.string.send_report))
                        .startChooser();
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspectButton);
        mSuspectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 请求Android协助从联系人数据库里获取某个具体联系人。第2个参数是数据获取位置 Uri
                // 统一资源标识符 Uniform Resource Identifier 用于标识某一互联网资源名称的字符串。
                // 该种标识允许用户对任何（包括本地和互联网）的资源通过特定的协议进行交互操作。
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        // 如果没有与目标隐式intent相匹配的activity, 应用会立即崩溃。解决办法是首先通过操作系统中的PackageManager类进行自检。
        // call resolveActivity(...) to find an activity that matches the Intent you gave it.
        // MATCH_DEFAULT_ONLY 被隐式添加在所有隐式 Intent 中。
        // 在这个函数调用中添加该flag，就会仅限于查找在 manifest 中声明了该flag的 Activity。
        if (pm.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        return v;
    }
    
    private void showPhoto() {
        // (Re)set the image button's image based on our photo
        Photo p = mCrime.getPhoto();
        BitmapDrawable b = null;
        if (p != null) {
            String path = getActivity().getFileStreamPath(p.getFilename()).getAbsolutePath();
            Log.d(TAG, "Crime " + mCrime.getTitle() + " photo path " + path);
            b = PictureUtils.getScaledDrawable(getActivity(), path);
            mPhotoView.setImageDrawable(b);
        }
    }
    
    // onStart()和onStop()是activity可见和消失的时间点。
    // 最佳实践：activity的视图一出现时就加载图片，然后等到activity再也不可见的情况下，再卸载图片。
    @Override
    public void onStart() {
        super.onStart();
        showPhoto();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        PictureUtils.cleanImageView(mPhotoView);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // java.lang.ClassCastException: CrimePagerActivity cannot be cast to CrimeFragment$Callbacks
        // 如果在手机上运行，将会崩溃；因为任何托管CrimeFragment的activity都必须实现CrimeFragment.Callbacks接口。
        mCallbacks = (Callbacks)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }
    
    @Override
    // Saving application data in onPause() lifecycle method, this is the safest choice.
    public void onPause() {
        super.onPause();
        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }
    
    public void returnResult() {
        getActivity().setResult(Activity.RESULT_OK, null);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            mCallbacks.onCrimeUpdated(mCrime);
            updateDate();
            
        } else if (requestCode == REQUEST_PHOTO) {
            // Create a new photo object and attach it to the crime
            String filename = data.getStringExtra(CrimeCameraFragment.EXTRA_PHOTO_FILENAME);
            if (filename != null) {
                Photo photo = new Photo(filename);
                mCrime.setPhoto(photo);
                mCallbacks.onCrimeUpdated(mCrime);
                showPhoto();
                Log.i(TAG, "Crime: " + mCrime.getTitle() + " has a photo");
            }
            
        } else if (requestCode == REQUEST_CONTACT) {
           // 联系人应用返回包含在intent中的URI数据给父activity时，它会添加一个Intent.FLAG_GRANT_READ_URI_PERMISSION标志。
           // 该标志向Android示意，CriminalIntent应用中的父activity可以使用联系人数据一次。
           // 因此我们就获得读取联系人数据库的权限，是被临时授予的。
           Uri contactUri = data.getData();
           
           // Specify which fields you want your query to return values for.
           String[] queryFields = new String[] { ContactsContract.Contacts.DISPLAY_NAME };
           
           // Perform your query - the contactUri is like a "where" clause here.
           // 创建了一条查询语句，要求返回全部联系人的显示名字(由第2个参数queryFields决定只返回名字)
           // ContentProvider类的实例封装了联系人数据库并提供给其他应用使用，通过ContentResolver访问ContentProvider.
           Cursor c = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);

           try {
               // Double-check that you actually got results
               if (c.getCount() == 0) {
                   return;
               }

               // Pull out the first column of the first row of data that is your suspect's name.
               c.moveToFirst();
               String suspect = c.getString(0);
               mCrime.setSuspect(suspect);
               mCallbacks.onCrimeUpdated(mCrime);
               mSuspectButton.setText(suspect);
           } finally {
               c.close();
           }
        }
    }
    
    public void updateDate() {
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        mDateButton.setText(dateString);
    }
    
    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        
        String report = getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);        
        return report;
    }
}

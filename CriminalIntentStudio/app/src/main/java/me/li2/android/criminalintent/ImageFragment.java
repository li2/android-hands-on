package me.li2.android.criminalintent;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageFragment extends DialogFragment {
    public static final String EXTRA_IMAGE_PATH = 
            "me.li2.android.criminalintent.image_path";
    
    public static ImageFragment newInstance(String imagePath) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_IMAGE_PATH, imagePath);
        
        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return fragment;
    }
    
    private ImageView mImageView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mImageView = new ImageView(getActivity());

        String path = (String) getArguments().getSerializable(EXTRA_IMAGE_PATH);
        Bitmap bitmap = PictureUtils.getScaledBitmap(path, getActivity());
        mImageView.setImageBitmap(bitmap);

        return mImageView;
    }
}

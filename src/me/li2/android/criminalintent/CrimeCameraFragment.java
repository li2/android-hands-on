package me.li2.android.criminalintent;

import java.io.IOException;
import java.util.List;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

@SuppressWarnings("deprecation") // 使用该注解来消除弃用代码相关的警告信息
public class CrimeCameraFragment extends Fragment {
    private static final String TAG = "CrimeCameraFragment";
    
    private Camera mCamera;
    private SurfaceView mSurfaceView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInatanceState) {
        // View v = inflater.inflate(R.layout.fragment_crime_camera, parent)导致exception:
        // java.lang.IllegalStateException: The specified child already has a parent.
        // You must call removeView() on the child's parent first.
        // http://stackoverflow.com/questions/23149981/fragments-the-specified-child-already-has-a-parent-you-must-call-removeview
        // @attachToRoot
        // Whether the inflated hierarchy should be attached to parent.
        // If false, root is only used to create the correct subclass of LayoutParams for the root view in the XML.
        View v = inflater.inflate(R.layout.fragment_crime_camera, parent, false);

        Button takePictureButton = (Button) v.findViewById(R.id.crime_camera_takePictureButton);
        // 为按钮设置监听器，用户点击时，退出当前托管的activity
        // set a listener on the button that simply finishes the hosting activity.
        takePictureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        mSurfaceView = (SurfaceView) v.findViewById(R.id.crime_camera_surfaceView);
        SurfaceHolder holder = mSurfaceView.getHolder();
        // setType() and SURFACE_TYPE_PUSH_BUFFERS are both deprecated,
        // but are required for Camera preview to work on pre-3.0 devices.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(mSurfaceHolderCallback);

        return v;
    }

    @TargetApi(9)
    @Override
    // 在onResume()和onPause()回调方法中打开和释放相机资源。
    // 可确定用户能同fragment视图交互的时间边界，只有在用户能够同fragment视图交互时，相机才可以使用。
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            // 传入参数0打开设备可用的第一相机（通常指的是后置相机）
            mCamera = Camera.open(0);
        } else {
            mCamera = Camera.open();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 调用release()方法之前，首先要确保存在Camera实例。
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // SurfaceView及其协同工作对象都不会自我绘制内容。
    // 对于任何想将内容绘制到Surface缓冲区的对象，我们称其为Surface的客户端。Camera实例是Surface的客户端。
    //
    // SurfaceHolder是我们与Surface对象联系的纽带。Surface对象代表着原始像素数据的缓冲区。
    // Surface对象也有生命周期：SurfaceView出现在屏幕上时，会创建Surface；SurfaceView从屏幕上消失时，Surface随即被销毁。
    // Surface不存在时，必须保证没有任何内容要在它上面绘制。
    // 为完成以上任务，SurfaceHolder提供了另一个接口：SurfaceHolder.Callback.
    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        // SurfaceView从屏幕上移除时，Surface也随即被销毁。通过该方法，可以通知Surface的客户端停止使用Surface。
        public void surfaceDestroyed(SurfaceHolder holder) {
            // we can no longer display on this surface, so stop the preview.
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }

        @Override
        // 包含SurfaceView的视图层级结构被放到屏幕上时调用该方法。是Surface与其客户端进行关联的地方。
        public void surfaceCreated(SurfaceHolder holder) {
            // Tell the camera to use this surface as its preview area.
            try {
                if (mCamera != null) {
                    mCamera.setPreviewDisplay(holder);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error setting up preview display", e);
            }
        }

        @Override
        // Surface首次显示在屏幕上时调用该方法。通过传入的参数，可以知道Surface的像素格式以及它的宽度和高度。
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCamera == null) {
                return;
            }
            
            // The surface has changed size, update the camera preview size
            Camera.Parameters parameters = mCamera.getParameters();
            // 确定预览界面大小，通过Camera.Parameters嵌套类获取系统支持的相机预览尺寸列表。
            Size s = getBestSupportedSize(parameters.getSupportedPreviewSizes(), width, height);
            parameters.setPreviewSize(s.width, s.height);
            mCamera.setParameters(parameters);
            try {
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Could not start preview", e);
                mCamera.release();
                mCamera = null;
            }
        }
    };
    
    // A simple algorithm to get the largest size available.
    private Size getBestSupportedSize(List<Size> sizes, int width, int height) {
        Size bestSize = sizes.get(0);
        int largestArea = bestSize.width * bestSize.height;
        for (Size size : sizes) {
            int area = size.width * size.height;
            if (area > largestArea) {
                bestSize = size;
                largestArea = area;
            }
        }
        
        return bestSize;
    }

}

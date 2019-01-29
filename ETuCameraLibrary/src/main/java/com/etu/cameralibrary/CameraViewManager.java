package com.etu.cameralibrary;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;

import com.etu.cameralibrary.base.BaseCameraView;
import com.etu.cameralibrary.view.Camera2Activity;
import com.etu.cameralibrary.view.CameraActivity;

/**
 * Camera management Class
 */
public class CameraViewManager {
    public static final String TAG = CameraViewManager.class.getSimpleName();

    public CameraViewManager mCameraManager;
    public Activity mActivity;
    public TextureView mTextureView;

    public CameraViewManager(Activity activity) {
        this.mActivity = activity;

    }

    public static CameraViewManager sInstance;

    public static synchronized CameraViewManager getInstance(Activity activity) {
        if (sInstance == null) {
            sInstance = new CameraViewManager(activity);
        }
        return sInstance;
    }

    /**
     * Get the camera for your current device
     *
     * @return
     */
    public BaseCameraView getCameraView() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? new CameraView2(mActivity) : new CameraView1(mActivity);
    }


    /**
     * This UI Refers to WeChat
     * Enter the  Camera Activity
     *
     * @param resultCode
     */
    public void startCameraActivity(int resultCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(mActivity, Camera2Activity.class);
            intent.putExtra("resultCode", resultCode);
            mActivity.startActivityForResult(intent,resultCode);
        } else {
            Intent intent = new Intent(mActivity, CameraActivity.class);
            intent.putExtra("resultCode", resultCode);
            mActivity.startActivityForResult(intent,resultCode);
        }


    }
}

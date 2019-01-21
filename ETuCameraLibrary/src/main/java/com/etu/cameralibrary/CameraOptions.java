package com.etu.cameralibrary;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;

public class CameraOptions {
    public final String TAG = CameraOptions.class.getSimpleName();
    private Context mContext;

    public CameraOptions(Context context) {

    }


    /**
     * 检查是否有摄像头
     *
     * @return
     */
    private boolean checkCameraSupport() {
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * 获取相机实例
     *
     * @return
     */
    public Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open(); // 试图获取Camera实例

        } catch (Exception e) {
            // 摄像头不可用（正被占用或不存在）
            Log.e(TAG, "摄像头不可用");
        }
        return camera;
    }


}

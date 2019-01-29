package com.etu.cameralibrary.view;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.etu.cameralibrary.AutoFitTextureView;
import com.etu.cameralibrary.CameraView2;
import com.etu.cameralibrary.CameraViewManager;
import com.etu.cameralibrary.CaptureButton;
import com.etu.cameralibrary.CaptureButtonListener;
import com.etu.cameralibrary.R;
import com.etu.cameralibrary.utils.PhoneDisplay;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends FragmentActivity implements CameraView2.TakePhotoListener, CaptureButtonListener {
    public final String TAG = Camera2Activity.class.getSimpleName();
    private int mResultCode;
    private ImageView mIvSwitchCamera;

    private AutoFitTextureView mTextureView;
    private CameraView2 mCamera2;
    private CaptureButton mCaptureBt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        mResultCode = getIntent().getIntExtra("resultCode", -1);
        PhoneDisplay.init(this);
        initView();
        initCamera();
    }

    private void initCamera() {
        mCamera2 = (CameraView2) CameraViewManager.getInstance(this).getCameraView();
        mCamera2.initCameraOptions(mTextureView);
        mCamera2.setTakePhotoListener(this);
    }

    private void initView() {
        mCaptureBt = findViewById(R.id.capture_button);
        mTextureView = findViewById(R.id.auto_textureView);
        mIvSwitchCamera = findViewById(R.id.iv_switch_camear);
        mCaptureBt.setCaptureLisenter(this);
        mIvSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera2.switchCamera();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mCamera2.closeCamera();
        mCamera2.stopBackgroundThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera2.startBackgroundThread();
        if (mTextureView.isAvailable()) {
            mCamera2.openCamera(mTextureView.getWidth(), mTextureView.getHeight());

        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mCamera2.openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            mCamera2.configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };


    @Override
    public void takePhotoSuccess(File file) {

    }

    @Override
    public void takePhotoFailure(String error) {

    }


    @Override
    public void takePictures() {
        mCamera2.takePicture();

    }

    @Override
    public void recordShort(long time) {

    }

    @Override
    public void startRecord() {
        mCamera2.startRecord();

    }

    @Override
    public void stopRecord(long time) {
        mCamera2.stopRecord();
    }

    @Override
    public void recordError() {

    }
}

package com.etu.camera;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.etu.cameralibrary.AutoFitTextureView;
import com.etu.cameralibrary.CameraView2;
import com.etu.cameralibrary.CameraViewManager;
import com.etu.cameralibrary.utils.PhoneDisplay;

/**
 *
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TestActivity extends FragmentActivity {
    public final String TAG = TestActivity.class.getSimpleName();
    private AutoFitTextureView mAutoFitTextureView;
    private TextView mIvSwitch;
    private TextView mTvTakePicture;
    private ImageView mIv;
    private CameraView2 mCamera2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        PhoneDisplay.init(this);
        mAutoFitTextureView = findViewById(R.id.auto_textureView);
        CameraViewManager.getInstance(this).startCameraActivity(1);
//        mCamera2 = (CameraView2) CameraViewManager.getInstance(this).getCameraView();
//        mCamera2.initCameraOptions(mAutoFitTextureView);
//
//        mTvTakePicture = findViewById(R.id.tv_take_picture);
//        mIvSwitch = findViewById(R.id.tv_switch);
//        mIv = findViewById(R.id.iv);
//        mIvSwitch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCamera2.switchCamera();
//            }
//        });
//
//        mTvTakePicture.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCamera2.takePicture();
//            }
//        });
//        mTvTakePicture.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                Log.e("hehe","setOnLongClickListener");
//                mCamera2.startRecord();
//                return true;
//            }
//        });
//        mIv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCamera2.stopRecord();
//            }
//        });
//        mTvTakePicture.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    Log.e("hehe","ACTION_UP");
//                }
//                return false;
//            }
//        });
    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
////        mCamera2.closeCamera();
//        mCamera2.stopBackgroundThread();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mCamera2.startBackgroundThread();
//        if (mAutoFitTextureView.isAvailable()) {
//            mCamera2.openCamera(mAutoFitTextureView.getWidth(), mAutoFitTextureView.getHeight());
//
//        } else {
//            mAutoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
//        }
//    }
//
//    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
//            = new TextureView.SurfaceTextureListener() {
//
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
//            mCamera2.openCamera(width, height);
//        }
//
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
//            mCamera2.configureTransform(width, height);
//        }
//
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
//            return true;
//        }
//
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
//        }
//
//    };

}
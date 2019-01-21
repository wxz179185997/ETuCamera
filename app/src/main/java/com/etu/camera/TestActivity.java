package com.etu.camera;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.etu.cameralibrary.AutoFitTextureView;
import com.etu.cameralibrary.CameraView2;

/**
 *
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TestActivity extends FragmentActivity {
    public final String TAG = TestActivity.class.getSimpleName();
    private AutoFitTextureView mAutoFitTextureView;
    private CameraView2 mCamera2;
    private TextView mIvSwitch;
    private TextView mTvTakePicture;
    private ImageView mIv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mAutoFitTextureView = findViewById(R.id.auto_textureView);
        mCamera2 = new CameraView2(this, mAutoFitTextureView);
        mTvTakePicture = findViewById(R.id.tv_take_picture);
        mIvSwitch = findViewById(R.id.tv_switch);
        mIv = findViewById(R.id.iv);
        mIvSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera2.switchCamera();
            }
        });

        mTvTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera2.takePicture();
            }
        });
        mTvTakePicture.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mCamera2.startRecord();
                Log.e(TAG, "onLongClick");
                return true;
            }
        });
        mIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera2.stopRecord();
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
        if (mAutoFitTextureView.isAvailable()) {
//            mCamera2.openCamera(mAutoFitTextureView.getWidth(), mAutoFitTextureView.getHeight());
            mCamera2.startPreview();

        } else {
            mAutoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
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

}
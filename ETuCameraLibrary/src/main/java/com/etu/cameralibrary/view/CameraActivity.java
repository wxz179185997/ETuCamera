package com.etu.cameralibrary.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;

import com.etu.cameralibrary.CameraSurfaceView;
import com.etu.cameralibrary.R;

public class CameraActivity extends FragmentActivity implements View.OnClickListener {

    private int mResultCode;
    private ImageView mIvTakePhoto;
    private ImageView mIvSwitchCamera;
    private CameraSurfaceView mCameraSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mResultCode = getIntent().getIntExtra("resultCode", -1);
        initView();
        initCamera();
    }

    private void initCamera() {
    }

    private void initView() {
        mIvTakePhoto = findViewById(R.id.iv_take_photo);
        mIvSwitchCamera = findViewById(R.id.iv_switch_camear);
        mIvTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mIvSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    @Override
    public void onClick(View v) {


    }

    private void takePhoto() {

    }
}

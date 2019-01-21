package com.etu.cameralibrary;

/**
 * 相机操作
 */
public interface CameraActionInterface {

    void takePicture();

    void startVideoRecorder();

    void stopVideoRecorder();

    void startPreView();

    void stopPreView();

    void getCameraView();

    void setCameraParms();


    void switchCamera();
}

package com.etu.cameralibrary.base;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Camera 管理类
 */
public abstract class BaseCameraView {

    public final String TAG = BaseCameraView.class.getSimpleName();

    /**
     * 开启预览
     */
    public abstract void startPreview();

    public abstract void switchCamera();

    public abstract void takePicture();

    public abstract void startRecord();

    public abstract void stopRecord();

    public abstract void releaseCamera();

    /**
     * 获取图片地址
     *
     * @return
     */
    public File getImageFilePath() {
        File imageFile = null;
        String path = "";
        path = Environment
                .getExternalStorageDirectory() + "/ETuCamera/Image/" + "IMG_" + System.currentTimeMillis() + ".jpg";
        try {
            File file = new File(Environment
                    .getExternalStorageDirectory() + "/ETuCamera/Image/");
            if (!file.exists()) {
                file.mkdirs();
            }
            imageFile = new File(path);
            if (!imageFile.exists()) {
                imageFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageFile;
    }

    /**
     * 获取视频地址
     *
     * @return
     */
    public String getVideoFilePath() {
        String path = "";
        path = Environment
                .getExternalStorageDirectory() + "/ETuCamera/Video/"+ "Video_"+ System.currentTimeMillis() + ".mp4";
        try {
            File file = new File(Environment
                    .getExternalStorageDirectory() + "/ETuCamera/Video/");
            if (!file.exists()) {
                file.mkdirs();
            }
            File fileVideo = new File(path);
            if (!fileVideo.exists()) {
                fileVideo.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }
}

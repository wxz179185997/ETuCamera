package com.etu.cameralibrary.utils;

import android.media.Image;
import android.util.Log;

import com.etu.cameralibrary.CameraView2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Camera2ImageSaveUtils implements Runnable {

    /**
     * JPEG 格式
     */
    private final Image mImage;
    private final File mFile;
    private CameraView2.TakePhotoListener mTakePhotoListener;


    public Camera2ImageSaveUtils(Image image, File file) {
        mImage = image;
        mFile = file;
    }


    public Camera2ImageSaveUtils(Image image, File file, CameraView2.TakePhotoListener listener) {
        mImage = image;
        mFile = file;
        this.mTakePhotoListener = listener;
    }

    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            if (mTakePhotoListener != null)
                mTakePhotoListener.takePhotoFailure(e.toString());
        } finally {
            mImage.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mTakePhotoListener != null)
                        mTakePhotoListener.takePhotoFailure(e.toString());
                }
            }
            if (mTakePhotoListener != null)
                mTakePhotoListener.takePhotoSuccess(mFile);
        }
    }
}

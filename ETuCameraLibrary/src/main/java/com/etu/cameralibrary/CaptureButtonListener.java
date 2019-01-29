package com.etu.cameralibrary;


public interface CaptureButtonListener {
    void takePictures();

    void recordShort(long time);

    void startRecord();

    void stopRecord(long time);


    void recordError();
}

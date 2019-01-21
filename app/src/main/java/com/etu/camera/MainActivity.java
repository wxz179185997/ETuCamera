package com.etu.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.etu.cameralibrary.CameraSurfaceView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Camera.ShutterCallback {
    private Camera mCamera;
    private CameraSurfaceView mPreview;
    private TextView mTvPhoto;
    private String PATH = Environment
            .getExternalStorageDirectory() + "/DCIM/Camera/";// 保存图像的路径;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvPhoto = findViewById(R.id.tv_photo);
        mCamera = getCamera();
        paramters(mCamera);
        // 创建Preview view并将其设为activity中的内容
        mPreview = new CameraSurfaceView(this, mCamera);
        FrameLayout preview = findViewById(R.id.frame_layout);
        preview.addView(mPreview);
        mTvPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(MainActivity.this, rawPictureCallBack, jpegPictureCallBack);
            }
        });

    }

    public Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open(); // 试图获取Camera实例
        } catch (Exception e) {
            // 摄像头不可用（正被占用或不存在）
            Log.e("hehe", "摄像头不可用");
        }
        return camera;
    }

    /**
     * 设置 Paramters
     *
     * @param camera
     */
    private void paramters(Camera camera) {
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        Camera.Size psize;
        for (int i = 0; i < pictureSizes.size(); i++) {
            psize = pictureSizes.get(i);
            Log.i("pictureSize", psize.width + " x " + psize.height);
        }
        for (int i = 0; i < previewSizes.size(); i++) {
            psize = previewSizes.get(i);
            Log.i("previewSize", psize.width + " x " + psize.height);
        }
    }

    Camera.PictureCallback rawPictureCallBack = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };


    Camera.PictureCallback jpegPictureCallBack = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            /*
             * if (Environment.getExternalStorageState().equals(
             * Environment.MEDIA_MOUNTED)) // 判断SD卡是否存在，并且可以可以读写 {
             *
             * } else { Toast.makeText(EX07_16.this, "SD卡不存在或写保护",
             * Toast.LENGTH_LONG) .show(); }
             */
            // Log.w("============", _data[55] + "");

            try {
                /* 取得相片 */
                Bitmap bitMap = BitmapFactory.decodeByteArray(data, 0,
                        data.length);
                android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
                android.hardware.Camera.getCameraInfo(0, info);
                Bitmap newBitMap = rotateBitmapByDegree(bitMap, info.orientation);

                /* 创建文件 */
                File myCaptureFile = new File(PATH, getImageName());
                BufferedOutputStream bos = new BufferedOutputStream(
                        new FileOutputStream(myCaptureFile));

                /* 采用压缩转档方法 */
                newBitMap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

                /* 调用flush()方法，更新BufferStream */
                bos.flush();

                /* 结束OutputStream */
                bos.close();
//                Log.e("hehe",getBitmapDegree(myCaptureFile.getPath())+"");

                /* 让相片显示3秒后圳重设相机 */
                // Thread.sleep(2000);
                /* 重新设定Camera */
                stopCamera();
                initCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    /* 停止相机的method */
    private void stopCamera() {
        if (mCamera != null) {
            try {
                /* 停止预览 */
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }}
    }

    /* 相机初始化的method */
    private void initCamera() {
//        if//            try {
////                Camera.Parameters parameters = mCamera.getParameters();
////                /*
////                 * 设定相片大小为1024*768， 格式为JPG
////                 */
////                // parameters.setPictureFormat(PixelFormat.JPEG);
////                parameters.setPictureSize(1024, 768);
////                mCamera.setParameters(parameters);
////                /* 打开预览画面 */
////                mCamera.startPreview();
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
////        } (mCamera != null) {

        paramters(mCamera);
        mCamera.startPreview();
    }

    @Override
    public void onShutter() {
        /* 按下快门瞬间会调用这里的程序 */

    }


    private String getImageName() {
        String name = "IMAGE_";
        name = name + System.currentTimeMillis() + ".jpg";
        return name;
    }


    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm     需要旋转的图片
     * @param degree 旋转角度
     * @return 旋转后的图片
     */
    public Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

}

package com.etu.cameralibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.Toast;

import com.etu.cameralibrary.base.BaseCameraView;
import com.etu.cameralibrary.utils.Camera2ImageSaveUtils;
import com.etu.cameralibrary.utils.PhoneDisplay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.etu.cameralibrary.base.CameraContants.STATE_PICTURE_TAKEN;
import static com.etu.cameralibrary.base.CameraContants.STATE_PREVIEW;
import static com.etu.cameralibrary.base.CameraContants.STATE_RECORD_DING;
import static com.etu.cameralibrary.base.CameraContants.STATE_START_RECORD;
import static com.etu.cameralibrary.base.CameraContants.STATE_STOP_RECORD;
import static com.etu.cameralibrary.base.CameraContants.STATE_WAITING_LOCK;
import static com.etu.cameralibrary.base.CameraContants.STATE_WAITING_NON_PRECAPTURE;
import static com.etu.cameralibrary.base.CameraContants.STATE_WAITING_PRECAPTURE;

/**
 * Camera 2
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraView2 extends BaseCameraView implements AutoFitTextureView.OnScaleGestureListener {
    public String TAG = CameraView2.class.getSimpleName();

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private Size mPreviewSize;

    private AutoFitTextureView mTextureView;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private Activity mActivity;
    private Semaphore mCameraOpenCloseLock;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mCaptureBuilder;
    private Handler mCameraHandler;
    private HandlerThread mCameraBackThread;
    private int mCameraState;//当前相机状态位
    private String mCameraId;

    private boolean mIsFlashSupported;//是否支持闪光
    private int mSensorOrientation;//传感器方向
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private CameraFocusView mFocusView;

    /**
     * Record
     */
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();


    public String mVideoFilesPath;
    private MediaRecorder mMediaRecorder;
    private String mVideoAbsolutePath;
    public boolean isCameraFront = false;
    private Rect mScaleZoom;
    //Test
    public float finger_spacing = 0;
    public int zoom_level = 1;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    public TakePhotoListener mTakePhotoListener;

    public interface TakePhotoListener {
        void takePhotoSuccess(File file);

        void takePhotoFailure(String error);
    }

    public void setTakePhotoListener(TakePhotoListener listener) {
        this.mTakePhotoListener = listener;
    }


    public CameraView2(Activity activity) {
        this.mActivity = activity;
    }

    public CameraView2(Activity activity, AutoFitTextureView textureView) {
        this.mActivity = activity;
    }


    /**
     * 初始化
     *
     * @param textureView
     */
    public void initCameraOptions(AutoFitTextureView textureView) {
        mCameraOpenCloseLock = new Semaphore(1);
        this.mTextureView = textureView;
        this.mTextureView.setOnScaleGestureListener(this);
        try {
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            String[] cameraList = manager.getCameraIdList();
            if (cameraList.length > 0)
                mCameraId = cameraList[0];
            else {
                Toast.makeText(mActivity, "当前设备不支持相机", Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraDevice.StateCallback mCameraStateBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            startCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            Toast.makeText(mActivity, "打开失败,请重新尝试", Toast.LENGTH_LONG).show();
        }
    };

    /**
     * 从ImageReader获取新图像时调用的回调。
     */
    ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mCameraHandler.post(new Camera2ImageSaveUtils(reader.acquireNextImage(), getImageFilePath(), mTakePhotoListener));
            startPreview();

        }
    };

    /**
     * 一个回调对象，用于跟踪提交给{@link CaptureRequest}的进度相机设备。
     */
    CameraCaptureSession.CaptureCallback mCaptureSessionBack = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mCameraState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    Log.e(TAG, "afState---  " + afState);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mCameraState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mCameraState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mCameraState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mCameraState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mCaptureBuilder.build(), mCaptureSessionBack, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setVideoPath(String path) {
        mVideoFilesPath = path;
    }

    /**
     * 关闭当前相机设备
     */
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    public void openCamera(int width, int height) {
        if (!hasPermissionsGranted(CAMERA_PERMISSIONS)) {
            Toast.makeText(mActivity, "缺少相机相关权限", Toast.LENGTH_SHORT).show();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("打开相机超时");
            }
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            manager.openCamera(mCameraId, mCameraStateBack, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
            throw new RuntimeException("打开相机异常", e);
        }
    }

    public void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize || null == mActivity) {
            return;
        }
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void captureStillPicture() {
        try {
            if (null == mActivity || null == mCameraDevice) {
                return;
            }
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                //当图像捕获完全完成且所有结果元数据都可用时，将调用此方法。
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//                    Toast.makeText(mActivity, "Saved: " + mFile, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "onCaptureCompleted");
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
//            mCaptureSession.abortCaptures();//该方法华为手机NOTE 8 不知道，其他机型暂未测试
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "captureStillPicture----  " + e.toString());
            e.printStackTrace();
        }
    }


    private Integer getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;

    }

    /**
     * 开启Thread
     */
    public void startBackgroundThread() {
        mCameraBackThread = new HandlerThread("CameraBackground");
        mCameraBackThread.start();
        mCameraHandler = new Handler(mCameraBackThread.getLooper());
    }

    /**
     * 停止Thread
     */
    public void stopBackgroundThread() {
        mCameraBackThread.quitSafely();
        try {
            mCameraBackThread.join();
            mCameraBackThread = null;
            mCameraHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(mActivity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mIsFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private void lockFocus() {
        try {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
//            mState = STATE_WAITING_LOCK;
            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//
                }
            };

            mCaptureSession.capture(mCaptureBuilder.build(), captureCallback,
                    mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "lockFocus----  " + e.toString());
            e.printStackTrace();
        }
    }


    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mCaptureBuilder);
            mCaptureSession.capture(mCaptureBuilder.build(), mCaptureSessionBack,
                    mCameraHandler);
            // After this, the camera will go back to the normal state of preview.
            mCameraState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureSessionBack,
                    mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Log.e("hehe", "setUpCameraOutputs-----  ");

        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);

            //判断是否为前置摄像头,暂时不处理前置
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return;
            }

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }

            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(
                    mImageAvailableListener, mCameraHandler);

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.e(TAG, "SensorOrientation----   " + mSensorOrientation);
            Log.e(TAG, "CameraId----   " + mCameraId);

            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            mActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.

//            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
//                    maxPreviewHeight, largest);

            mPreviewSize=chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),mTextureView.getWidth(),mTextureView.getHeight());
            // We fit the aspect ratio of TextureView to the size of preview we picked.

            //暂时注释代码
//            int orientation = mActivity.getResources().getConfiguration().orientation;
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mTextureView.setAspectRatio(
//                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            } else {
//                mTextureView.setAspectRatio(
//                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
//            }

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mIsFlashSupported = available == null ? false : available;
            Log.e("hehe", "cameraId-----  " + mCameraId);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * 选择合适的预览尺寸
     *
     * @param choices
     * @param textureViewWidth
     * @param textureViewHeight
     * @return
     */
    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight) {
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Size option : choices) {
            if ((option.getWidth() == textureViewWidth) && (option.getHeight() == textureViewHeight)) {
                return option;
            }
        }
        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) textureViewWidth) / textureViewHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE-1;
        Size retSize = null;
        for (Size size : choices) {
            curRatio = ((float) size.getWidth()) / size.getHeight();
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        Log.e("hehe","width-- "+retSize.getWidth());
        Log.e("hehe","height-- "+retSize.getHeight());
        return retSize;
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                   int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void startCameraPreview() {
        try {
            if (mTextureView == null || !mTextureView.isAvailable() || mCameraDevice == null) {
                return;
            }
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            mCaptureBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureBuilder.addTarget(surface);
            // 创建 CameraCaptureSession用于预览
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (mCameraDevice == null) {
                                return;
                            }
                            mCaptureSession = cameraCaptureSession;
                            try {
                                //设置对焦模式
                                mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                //设置闪光灯
                                setAutoFlash(mCaptureBuilder);
                                mCaptureRequest = mCaptureBuilder.build();
                                mCaptureSession.setRepeatingRequest(mCaptureRequest,
                                        mCaptureSessionBack, mCameraHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e("hehe", "failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public void switchCamera() {
        try {
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            String[] cameraNum = manager.getCameraIdList();
            if (cameraNum.length == 1 && cameraNum[0].equals("0")) {
                Toast.makeText(mActivity, "该设备只支持后置相机", Toast.LENGTH_SHORT).show();
                return;
            } else if (cameraNum.length == 1 && cameraNum[0].equals("1")) {
                Toast.makeText(mActivity, "该设备只支持前置相机", Toast.LENGTH_SHORT).show();
                return;
            }
            releaseCamera();
            if (mCameraId.equals("0")) {
                mCameraId = "1";
                isCameraFront = true;
            } else if (mCameraId.equals("1")) {
                mCameraId = "0";
                isCameraFront = false;
            }
            try {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("打开相机超时");
                }
                mCaptureBuilder.set(CaptureRequest.SCALER_CROP_REGION, new Rect());

                manager.openCamera(mCameraId, mCameraStateBack, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } catch (CameraAccessException e) {
            Log.e(TAG, "切换相机异常");
        }
    }

    public void releaseCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }

    }

    /**
     * 拍照
     */
    public void takePicture() {
        if (mCameraState == STATE_START_RECORD) {
            return;
        }
//        lockFocus();
        try {
            if (mCameraDevice == null) {
                return;
            }
            // 创建拍照请求
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(mImageReader.getSurface());

            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            //获取屏幕方向
            captureBuilder.addTarget(mImageReader.getSurface());
            //isCameraFront是自定义的一个boolean值，用来判断是不是前置摄像头，是的话需要旋转180°，不然拍出来的照片会歪了
            if (isCameraFront) {
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(Surface.ROTATION_180));
            } else {
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            }


            captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mScaleZoom);


            mCaptureSession.stopRepeating();
//            mCaptureSession.abortCaptures();//该方法华为Note 8不支持
            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//
                }
            };

            mCaptureSession.capture(captureBuilder.build(), captureCallback
                    , mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "takePicture--  " + e.toString());
            e.printStackTrace();
        }

    }


    @Override
    public void onZoom(MotionEvent event) {
        Log.d(TAG, "onZoom");
        try {
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            float maxzoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int action = event.getAction();
            float currenFingerSpace;
            if (event.getPointerCount() > 1) {
                currenFingerSpace = getFingerSpacing(event);
                if (finger_spacing != 0) {
                    if (currenFingerSpace > finger_spacing && maxzoom > zoom_level) {
                        zoom_level++;
                    } else if (currenFingerSpace < finger_spacing && zoom_level > 1) {
                        zoom_level--;
                    }
                    int minW = (int) (rect.width() / maxzoom);
                    int minH = (int) (rect.height() / maxzoom);
                    //获取当前的宽 高差
                    int difW = rect.width() - minW;
                    int difH = rect.height() - minH;
                    int cropW = difW / 100 * zoom_level;
                    int cropH = difH / 100 * zoom_level;
                    cropW -= cropW & 3;
                    cropH -= cropH & 3;
                    Rect zoom = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
                    mScaleZoom = zoom;
                    mCaptureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                finger_spacing = currenFingerSpace;
            } else {
                if (action == MotionEvent.ACTION_UP) {
                    //single touch logic
                }
            }

            try {
                if (mCaptureSession != null) {
                    mCaptureSession
                            .setRepeatingRequest(mCaptureBuilder.build(), mCaptureSessionBack, null);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("can not access camera.", e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void clickDown(final MotionEvent event) {
        Log.e(TAG, "clickDown---   ");
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

//                final CameraFocusView focusView = new CameraFocusView(mActivity);
//                focusView.setCenterPoint(event.getX(), event.getY());
//                focusView.invalidate();
            }
        });

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                focusView.setVisibility(View.GONE);
//            }
//        }, 1000);
        setManualFocus(event);
    }

    /**
     * 手动对焦模式
     */
    private void setManualFocus(MotionEvent event) {
        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(getFocusRect(event.getX(), event.getY()), 1000)});
        mCaptureBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(getFocusRect(event.getX(), event.getY()), 1000)});
        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mCaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), mCaptureSessionBack, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setRepeatingRequest failed, " + e.getMessage());
        }


    }

    private Rect getFocusRect(float x, float y) {
        Rect rect;
        try {
            int screenW = PhoneDisplay.SCREEN_WIDTH_PIXELS;//获取屏幕长度
            int screenH = PhoneDisplay.SCREEN_HEIGHT_PIXELS;//获取屏幕宽度

            //因为获取的SCALER_CROP_REGION是宽大于高的，也就是默认横屏模式，竖屏模式需要对调width和height
            int realPreviewWidth = mTextureView.getHeight();
            int realPreviewHeight = mTextureView.getWidth();

            //根据预览像素与拍照最大像素的比例，调整手指点击的对焦区域的位置
            int focusX = (int) (realPreviewWidth / screenW * x);
            int focusY = (int) (realPreviewHeight / screenH * y);
            Log.i(TAG, "focusX=$focusX,focusY=$focusY");

            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            Rect totalPicSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            //获取SCALER_CROP_REGION，也就是拍照最大像素的Rect  此处利用CaptureBuilder获取的为null 所以改为CameraCharacteristics 获取
//        Rect totalPicSize = mCaptureBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            Log.i(TAG, "camera pic area size=$totalShowSize");

            //计算出摄像头剪裁区域偏移量
            int cutDx = (totalPicSize.height() - mTextureView.getHeight()) / 2;
            Log.i(TAG, "cutDx=$cutDx");

            //我们默认使用10dp的大小，也就是默认的对焦区域长宽是10dp，这个数值可以根据需要调节
            int width = PhoneDisplay.dp2px(10f);
            int height = PhoneDisplay.dp2px(10f);

            //返回最终对焦区域Rect
            return rect = new Rect(focusY, focusX + cutDx, focusY + height, focusX + cutDx + width);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return rect = new Rect();
    }

    //Determine the space between the first two fingers
    @SuppressWarnings("deprecation")
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    /**
     * 开启摄像
     */
    public void startRecord() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        mMediaRecorder = new MediaRecorder();
        closePreviewSession();
        setRecordConfig();
        initRecordSurface();
    }


    /**
     * 配置视频参数
     */
    private void setRecordConfig() {
        try {
            if (mActivity == null) {
                return;
            }
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mVideoAbsolutePath = getVideoFilePath();
            mMediaRecorder.setOutputFile(mVideoAbsolutePath);
            mMediaRecorder.setVideoEncodingBitRate(6000000);


            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            switch (sensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "setRecordConfig---  " + e.toString());
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void initRecordSurface() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mCaptureBuilder.addTarget(previewSurface);
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mCaptureBuilder.addTarget(recorderSurface);
            mCaptureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mScaleZoom);


            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    updatePreview();
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
                            mCameraState = STATE_START_RECORD;
                            // Start recording
                            mMediaRecorder.start();
                            mCameraState = STATE_RECORD_DING;
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null != mActivity) {
                        Toast.makeText(mActivity, "Failed", Toast.LENGTH_LONG).show();
                    }
                }
            }, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mCaptureBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    public void stopRecord() {
        if (mCameraState != STATE_RECORD_DING) {
            return;
        }
        mCameraState = STATE_STOP_RECORD;
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder = null;
            }
            startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }


    public void startPreview() {
        if (null == mTextureView || null == mPreviewSize || null == mActivity) {
            return;
        }
        closePreviewSession();

        try {
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("打开相机超时");
            }
            manager.openCamera(mCameraId, mCameraStateBack, mCameraHandler);
//            startVideoPreview();
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException-- " + e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException-- " + e.toString());
            e.printStackTrace();
        }

    }
}


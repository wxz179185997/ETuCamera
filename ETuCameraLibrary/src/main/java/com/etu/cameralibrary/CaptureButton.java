package com.etu.cameralibrary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import static com.etu.cameralibrary.base.CameraContants.BUTTON_STATE_CLICK;
import static com.etu.cameralibrary.base.CameraContants.BUTTON_STATE_IDEL;
import static com.etu.cameralibrary.base.CameraContants.BUTTON_STATE_LONG_CLICK;
import static com.etu.cameralibrary.base.CameraContants.BUTTON_STATE_RECORD_DING;
import static com.etu.cameralibrary.base.CameraContants.BUTTON_STATE_UN_CLICK;


public class CaptureButton extends View {

    private int mCurrentState;              //当前按钮状态

    private int mProgressColor = 0xEE16AE16;            //进度条颜色
    private int mOutsideColor = 0xEEDCDCDC;             //外圆背景色
    private int mInsideColor = 0xFFFFFFFF;              //内圆背景色


    private float event_Y;  //Touch_Event_Down时候记录的Y值


    private Paint mPaint;
    //长按事件判定时间
    private long mLongPressTimes = 1 * 500;


    private float mStrokeWidth;          //进度条宽度
    private int mOutsideAddSize;       //长按外圆半径变大的Size
    private int mInsideReduceSize;     //长安内圆缩小的Size

    //中心坐标
    private float mCenterX;
    private float mCenterY;

    private float button_radius;            //按钮半径
    private float button_outside_radius;    //外圆半径
    private float button_inside_radius;     //内圆半径
    private int button_size;                //按钮大小

    private float progress;         //录制视频的进度
    private int duration;           //录制视频最大时间长度
    private int min_duration;       //最短录制时间限制
    private int recorded_time;      //记录当前录制的时间

    private RectF rectF;

    private LongPressRunnable longPressRunnable;    //长按后处理的逻辑Runnable
    private RecordCountDownTimer timer;             //计时器

    public CaptureButton(Context context) {
        super(context, null);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initCaptureButton(context, 240);
    }


    public void initCaptureButton(Context context, int size) {
        this.button_size = size;
        button_radius = size / 2.0f;

        button_outside_radius = button_radius;
        button_inside_radius = button_radius * 0.75f;

        mStrokeWidth = size / 15;
        mOutsideAddSize = size / 5;
        mInsideReduceSize = size / 8;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        progress = 0;
        longPressRunnable = new LongPressRunnable();

        mCurrentState = BUTTON_STATE_IDEL;                //初始化为空闲状态

//        button_state = BUTTON_STATE_BOTH;  //初始化按钮为可录制可拍照

        duration = 10 * 1000;              //默认最长录制时间为10s
        min_duration = 1500;              //默认最短录制时间为1.5s

        mCenterX = (button_size + mOutsideAddSize * 2) / 2;
        mCenterY = (button_size + mOutsideAddSize * 2) / 2;

        rectF = new RectF(
                mCenterX - (button_radius + mOutsideAddSize - mStrokeWidth / 2),
                mCenterY - (button_radius + mOutsideAddSize - mStrokeWidth / 2),
                mCenterX + (button_radius + mOutsideAddSize - mStrokeWidth / 2),
                mCenterY + (button_radius + mOutsideAddSize - mStrokeWidth / 2));

        timer = new RecordCountDownTimer(duration, duration / 360);    //录制定时器
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(button_size + mOutsideAddSize * 2, button_size + mOutsideAddSize * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);

        mPaint.setColor(mOutsideColor); //外圆（半透明灰色）
        canvas.drawCircle(mCenterX, mCenterY, button_outside_radius, mPaint);

        mPaint.setColor(mInsideColor);  //内圆（白色）
        canvas.drawCircle(mCenterX, mCenterY, button_inside_radius, mPaint);

        //如果状态为录制状态，则绘制录制进度条
        if (mCurrentState == BUTTON_STATE_RECORD_DING) {
            mPaint.setColor(mProgressColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeWidth);
            canvas.drawArc(rectF, -90, progress, false, mPaint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if (mCurrentState != BUTTON_STATE_IDEL) {
//            return true;
//        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                event_Y = event.getY();
                mCurrentState = BUTTON_STATE_CLICK;
                if (mCurrentState != BUTTON_STATE_RECORD_DING)
                    postDelayed(longPressRunnable, mLongPressTimes);
                break;

            case MotionEvent.ACTION_UP:
                handlerUnpressByState();
                break;

        }
        return true;
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerUnpressByState() {
        Log.e("hehe", "CurrentState----  " + mCurrentState);
        removeCallbacks(longPressRunnable); //移除长按逻辑的Runnable
        //根据当前状态处理
        switch (mCurrentState) {
            //当前是点击按下
            case BUTTON_STATE_CLICK:
                //测试
//                if (captureLisenter != null) {
                startCaptureAnimation(button_inside_radius);
//                } else {
//                    mCurrentState = BUTTON_STATE_IDEL;
//                }
                break;
            //当前是长按状态
            case BUTTON_STATE_RECORD_DING:
                timer.cancel(); //停止计时器
                recordEnd();    //录制结束
                break;
        }
    }

    //录制结束
    private void recordEnd() {
        if (mCaptureListener != null) {
            if (recorded_time < min_duration)
                mCaptureListener.recordShort(recorded_time);//回调录制时间过短
            else
                mCaptureListener.stopRecord(recorded_time);  //回调录制结束
        }
        resetRecordAnim();  //重制按钮状态
    }

    //重制状态
    private void resetRecordAnim() {
        mCurrentState = BUTTON_STATE_UN_CLICK;
        progress = 0;       //重制进度
        invalidate();
        //还原按钮初始状态动画
        startRecordAnimation(
                button_outside_radius,
                button_radius,
                button_inside_radius,
                button_radius * 0.75f
        );
    }

    //内圆动画
    private void startCaptureAnimation(float inside_start) {
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_start * 0.75f, inside_start);
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        inside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //回调拍照接口
                mCaptureListener.takePictures();
                mCurrentState = BUTTON_STATE_UN_CLICK;
            }
        });
        inside_anim.setDuration(100);
        inside_anim.start();
    }

    //内外圆动画
    private void startRecordAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {
        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        //外圆动画监听
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_outside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //内圆动画监听
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        AnimatorSet set = new AnimatorSet();
        //当动画结束后启动录像Runnable并且回调录像开始接口
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //设置为录制状态
                if (mCurrentState == BUTTON_STATE_LONG_CLICK) {
                    if (mCaptureListener != null)
                        mCaptureListener.startRecord();
                    mCurrentState = BUTTON_STATE_RECORD_DING;
                    timer.start();
                }
            }
        });
        set.playTogether(outside_anim, inside_anim);
        set.setDuration(100);
        set.start();
    }


    //更新进度条
    private void updateProgress(long millisUntilFinished) {
        recorded_time = (int) (duration - millisUntilFinished);
        progress = 360f - millisUntilFinished / (float) duration * 360f;
        invalidate();
    }

    //录制视频计时器
    private class RecordCountDownTimer extends CountDownTimer {
        RecordCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateProgress(millisUntilFinished);
        }

        @Override
        public void onFinish() {
            updateProgress(0);
            recordEnd();
        }
    }

    //长按线程
    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            mCurrentState = BUTTON_STATE_LONG_CLICK;
            //没有录制权限
//            if (CheckPermission.getRecordState() != CheckPermission.STATE_SUCCESS) {
//            mCurrentState = BUTTON_STATE_IDEL;
//            if (captureLisenter != null) {
//                captureLisenter.recordError();
//                return;
//            }
//            }
            //启动按钮动画，外圆变大，内圆缩小
            startRecordAnimation(
                    button_outside_radius,
                    button_outside_radius + mOutsideAddSize,
                    button_inside_radius,
                    button_inside_radius - mInsideReduceSize
            );
        }
    }


    public CaptureButtonListener mCaptureListener;

    public void setCaptureLisenter(CaptureButtonListener listener) {
        this.mCaptureListener = listener;
    }


    //设置最长录制时间
    public void setDuration(int duration) {
        this.duration = duration;
        timer = new RecordCountDownTimer(duration, duration / 360);    //录制定时器
    }

    //设置最短录制时间
    public void setMinDuration(int duration) {
        this.min_duration = duration;
    }

    //是否空闲状态
    public boolean isIdle() {
        return mCurrentState == BUTTON_STATE_IDEL ? true : false;
    }

    //设置状态
    public void resetState() {
        mCurrentState = BUTTON_STATE_IDEL;
    }
}
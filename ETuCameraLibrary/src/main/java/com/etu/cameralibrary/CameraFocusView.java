package com.etu.cameralibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CameraFocusView extends View {
    private Paint mPaint;
    private float mX;
    private float mY;
    private Handler mHandle;

    public CameraFocusView(Context context) {
        super(context);
        setWillNotDraw(false);
        mHandle = new Handler();
    }

    public CameraFocusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraFocusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setCenterPoint(float x, float y) {
        this.mX = x;
        this.mY = y;
    }

    public void setCenterPointAndShow(float x, float y) {
        this.mX = x;
        this.mY = y;
//        mHandle.post(new Runnable() {
//            @Override
//            public void run() {
//                invalidate();
//            }
//        });
//        mHandle.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                setVisibility(GONE);
//            }
//        }, 1000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("hehe", "onDraw");
        mPaint = new Paint();
//        给画笔设置颜色
        mPaint.setColor(Color.RED);
//        设置画笔属性
        mPaint.setStyle(Paint.Style.STROKE);//画笔属性是空心圆
        mPaint.setStrokeWidth(8);//设置画笔粗细
        canvas.drawCircle(mX, mY, 200, mPaint);
    }
}

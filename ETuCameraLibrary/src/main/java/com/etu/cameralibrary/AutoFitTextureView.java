/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.etu.cameralibrary;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

public class AutoFitTextureView extends TextureView implements View.OnTouchListener {
    public static final String TAG = AutoFitTextureView.class.getSimpleName();
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;



    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
    }

//    /**
//     * 设置此视图的纵横比.
//     *
//     * @param width  Relative horizontal size
//     * @param height Relative vertical size
//     */
//    public void setAspectRatio(int width, int height) {
//        if (width < 0 || height < 0) {
//            throw new IllegalArgumentException("Size cannot be negative.");
//        }
//        mRatioWidth = width;
//        mRatioHeight = height;
//        requestLayout();
//    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        Log.e("hehe","MeasureSpec--- "+width);
//        Log.e("hehe","MeasureSpec--- "+height);
//
//        if (0 == mRatioWidth || 0 == mRatioHeight) {
//            setMeasuredDimension(width, height);
//        } else {
//            if (width < height * mRatioWidth / mRatioHeight) {
//                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
//            } else {
//                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
//            }
//        }
//    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() > 1) {
            if (mScaleListenr != null)
                mScaleListenr.onZoom(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mScaleListenr != null)
                mScaleListenr.clickDown(event);
        }
        return true;
    }

    public interface OnScaleGestureListener {

        void onZoom(MotionEvent event);

        void clickDown(MotionEvent event);

    }

    public OnScaleGestureListener mScaleListenr;

    public void setOnScaleGestureListener(OnScaleGestureListener listener) {
        this.mScaleListenr = listener;
    }

}

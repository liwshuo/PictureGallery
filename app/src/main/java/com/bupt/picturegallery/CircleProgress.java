package com.bupt.picturegallery;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * A circular progress bar that uses a wedge (arc) to mask a bitmap. Set the
 * indeterminateDrawable to your full circle resource.
 */
public class CircleProgress extends ProgressBar {

    private final Paint mPaint = new Paint();
    int mStokeWidth = 10;
    private float mStartAngle = -90.0f;
    private float mAngle = 0.0f;
    private float mHeight = 0.0f;
    private RectF mRect = null;

    public CircleProgress(Context context) {
        this(context, null);
    }

    public CircleProgress(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.progressBarStyle);
    }

    public CircleProgress(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            int progress = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "progress", 0);
            int max = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "max", 0);
            float per = (float) progress / (float) max;
            mAngle = 360.0f * per;
        }
    }

    /**
     * Sets the starting angle of the progress, 0 = EAST
     *
     * @param startAngle default -90.0f (NORTH)
     */
    public void setStartAngle(float startAngle) {
        mStartAngle = startAngle;
    }

    /**
     * Sets the progress, 0 = no progress shown, max = full progress shown
     *
     * @param progress in the range of 0-max
     */
    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        float per = (float) progress / (float) getMax();
        mAngle = 360.0f * per;
//        Application.getApplication().runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                invalidate();
//            }
//        });

    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if (mRect == null) {
            mHeight = getHeight();
            mRect = new RectF(mStokeWidth / 2, mStokeWidth / 2, mHeight - mStokeWidth / 2 - getPaddingRight(), mHeight - mStokeWidth / 2 - getPaddingBottom());
        }
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStokeWidth);
        mPaint.setColor(0x60000000);

        //draw the transparent circle
        canvas.drawCircle(mHeight / 2, mHeight / 2, (mHeight - mStokeWidth) / 2, mPaint);

        //draw the progress
        mPaint.setColor(Color.rgb(0x31, 0x7a, 0xd4));
        canvas.drawArc(mRect, mStartAngle, mAngle, false, mPaint);
    }
}
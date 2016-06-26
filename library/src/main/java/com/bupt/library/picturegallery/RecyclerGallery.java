package com.bupt.library.picturegallery;

import android.content.Context;
import android.graphics.PointF;
import android.os.Parcelable;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lishuo on 16/5/5.
 */
public class RecyclerGallery extends RecyclerView {

    public static final String TAG = RecyclerGallery.class.getSimpleName();

    public static final boolean DEBUG = true;

    private float mFlingFactor = 0.3f;
    private int mSmoothScrollTargetPosition = 0;
    private int mCurrentCenterPosition = 0;
    private int mBeforeScrollPosition = 0;

    private int mCurrentPosition = 0;

    private VelocityTracker mVelocityTracker = null;
    private float mVelocity = 0;

    private boolean mNeedAdjust = true;
    private boolean mHasScrolled = false;

    private List<OnPageChangedListener> mOnPageChangedListeners;

    public RecyclerGallery(Context context) {
        this(context, null);
    }

    public RecyclerGallery(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(context, attrs, defStyle);
        setNestedScrollingEnabled(false);
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyle) {
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        boolean flinging = super.fling((int) (velocityX * mFlingFactor), (int) (velocityY * mFlingFactor));
        if (mSmoothScrollTargetPosition < 0) {
            mSmoothScrollTargetPosition = 0;
        }
        if (mSmoothScrollTargetPosition >= getAdapter().getItemCount()) {
            mSmoothScrollTargetPosition = getAdapter().getItemCount() - 1;
        }
        if (flinging && getAdapter().getItemCount() > 0) {
            smoothScrollToPosition(mSmoothScrollTargetPosition);
        }
        return flinging;
    }

    @Override
    public void smoothScrollToPosition(int position) {
        if (getLayoutManager() != null && getLayoutManager() instanceof LinearLayoutManager) {
            // exclude item decoration
            LinearSmoothScroller linearSmoothScroller =
                    new LinearSmoothScroller(getContext()) {
                        @Override
                        public PointF computeScrollVectorForPosition(int targetPosition) {
                            if (getLayoutManager() == null) {
                                return null;
                            }
                            return ((LinearLayoutManager) getLayoutManager())
                                    .computeScrollVectorForPosition(targetPosition);
                        }

                        @Override
                        protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                            if (getLayoutManager() == null) {
                                return;
                            }
                            int dx = calculateDxToMakeVisible(targetView,
                                    getHorizontalSnapPreference());
                            int dy = calculateDyToMakeVisible(targetView,
                                    getVerticalSnapPreference());
                            if (dx > 0) {
                                dx = dx - getLayoutManager()
                                        .getLeftDecorationWidth(targetView);
                            } else {
                                dx = dx + getLayoutManager()
                                        .getRightDecorationWidth(targetView);
                            }
                            if (dy > 0) {
                                dy = dy - getLayoutManager()
                                        .getTopDecorationHeight(targetView);
                            } else {
                                dy = dy + getLayoutManager()
                                        .getBottomDecorationHeight(targetView);
                            }
                            final int distance = (int) Math.sqrt(dx * dx + dy * dy);
                            final int time = calculateTimeForDeceleration(distance);
                            if (time > 0) {
                                action.update(-dx, -dy, time, mDecelerateInterpolator);
                            }
                        }
                    };
            linearSmoothScroller.setTargetPosition(position);
            getLayoutManager().startSmoothScroll(linearSmoothScroller);
            mBeforeScrollPosition = mCurrentPosition;
            mCurrentPosition = position;
            for (OnPageChangedListener onPageChangedListener : mOnPageChangedListeners) {
                if (onPageChangedListener != null) {
                    onPageChangedListener.OnPageChanged(mBeforeScrollPosition, mCurrentPosition);
                }
            }
        } else {
            super.smoothScrollToPosition(position);
        }
    }

    @Override
    public void scrollToPosition(int position) {

    }

    /**
     * get item position in center of viewpager
     */
    public int getCurrentPosition() {
        int curPosition = -1;
        curPosition = ViewUtils.getCenterXChildPosition(this);
        if (curPosition < 0) {
            curPosition = mSmoothScrollTargetPosition;
        }
        return curPosition;
    }


    public void addOnPageChangedListener(OnPageChangedListener listener) {
        if (mOnPageChangedListeners == null) {
            mOnPageChangedListeners = new ArrayList<>();
        }
        mOnPageChangedListeners.add(listener);
    }

    public void removeOnPageChangedListener(OnPageChangedListener listener) {
        if (mOnPageChangedListeners != null) {
            mOnPageChangedListeners.remove(listener);
        }
    }

    public void clearOnPageChangedListeners() {
        if (mOnPageChangedListeners != null) {
            mOnPageChangedListeners.clear();
        }
    }

    private float mLastMotionX;
    private float mLastMotionY;
    private long mLastTouchTime;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        mCurrentPosition = getCenterHorizonChildPosition();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = ev.getX();
                mLastMotionY = ev.getY();
                mLastTouchTime = System.currentTimeMillis();
                startVelocityTracker(ev);
                break;
            case MotionEvent.ACTION_UP:
                mCurrentCenterPosition = getCenterHorizonChildPosition();
                if (mVelocity > 100) {
                    mSmoothScrollTargetPosition = mCurrentPosition - 1;
                } else if (mVelocity < -100) {
                    mSmoothScrollTargetPosition = mCurrentPosition + 1;
                } else {
                    mSmoothScrollTargetPosition = mCurrentPosition;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                caculateVelocityTracker(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();

        }
        return super.dispatchTouchEvent(ev);
    }

    private void startVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        mVelocityTracker.addMovement(event);
    }

    private void caculateVelocityTracker(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        mVelocityTracker.computeCurrentVelocity(1000);
        mVelocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker, event.getPointerId(event.getActionIndex()));
    }

    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return super.onTouchEvent(e);
    }

    @Override
    public void onScrollStateChanged(int state) {
        switch (state) {
            case SCROLL_STATE_IDLE:
                if (mNeedAdjust) {
                    smoothScrollToPosition(mCurrentCenterPosition);
                }
//                if (mBeforeScrollPosition != mCurrentPosition) {
//                    for (OnPageChangedListener onPageChangedListener : mOnPageChangedListeners) {
//                        if (onPageChangedListener != null) {
//                            onPageChangedListener.OnPageChanged(mBeforeScrollPosition, mCurrentPosition);
//                        }
//                    }
//                }
//                }else {
//                    smoothScrollToPosition(mSmoothScrollTargetPosition);
//                }
                break;
            case SCROLL_STATE_DRAGGING:
                mNeedAdjust = true;
                break;
            case SCROLL_STATE_SETTLING:
                mNeedAdjust = false;
                break;
        }
    }

    public interface OnPageChangedListener {
        void OnPageChanged(int oldPosition, int newPosition);
    }

    public int getCenterHorizonChildPosition() {
        int childCount = getChildCount();
        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (isChildInCenterHorizon(child)) {
                    return getChildAdapterPosition(child);
                }
            }
        }
        return childCount;
    }

    public boolean isChildInCenterHorizon(View view) {
        int childCount = getChildCount();
        int[] lvLocationOnScreen = new int[2];
        int[] vLocationOnScreen = new int[2];
        getLocationOnScreen(lvLocationOnScreen);
        int middleX = lvLocationOnScreen[0] + getWidth() / 2;
        if (childCount > 0) {
            view.getLocationOnScreen(vLocationOnScreen);
            if (vLocationOnScreen[0] <= middleX && vLocationOnScreen[0] + view.getWidth() >= middleX) {
                return true;
            }
        }
        return false;
    }
}

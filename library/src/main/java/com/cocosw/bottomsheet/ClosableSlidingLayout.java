package com.cocosw.bottomsheet;

import android.annotation.TargetApi;
import android.gesture.Gesture;
import android.gesture.GestureUtils;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/11/25.
 */
class ClosableSlidingLayout extends FrameLayout {

    private final float MINVEL;
    private ViewDragHelper mDragHelper;
    private SlideListener mListener;
    private int height;
    private int top;

    public ClosableSlidingLayout(Context context) {
        this(context, null);
    }

    public ClosableSlidingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ClosableSlidingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragCallback());
        MINVEL = getResources().getDisplayMetrics().density*400;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                height = getChildAt(0).getHeight();
                top = getChildAt(0).getTop();
                break;
            case MotionEvent.ACTION_MOVE:
                mDragHelper.captureChildView(getChildAt(0),0);
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setSlideListener(SlideListener listener){
        mListener = listener;
    }

    /**
     *Callback
     */
    private class ViewDragCallback extends ViewDragHelper.Callback{


        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            //Is off the screen ?
            if (state == ViewDragHelper.STATE_IDLE && mDragHelper.getCapturedView().getLeft()!=0) {
                mListener.onClosed();
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (yvel > MINVEL) {
                dismiss(releasedChild);
            } else if (yvel < -MINVEL) {
             //   mDragHelper.smoothSlideViewTo(releasedChild, 0, -getHeight());
            } else {
                if (releasedChild.getTop() >= top+height/2) {
                    dismiss(releasedChild);
//                } else if (releasedChild.getTop() < height / 2) {
//                    mDragHelper
//                            .smoothSlideViewTo(releasedChild, 0, -getHeight());
                } else {
                    mDragHelper.smoothSlideViewTo(releasedChild, 0, top);
                }
            }
            invalidate();
        }


        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return Math.max(top, ClosableSlidingLayout.this.top);
        }
    }

    private void dismiss(View view) {
        mDragHelper.abort();
        if (mListener!=null) mListener.onClosed();
    }

    /**
     * set listener
     */
    interface SlideListener{
        void onClosed();
        void onOpened();
    }

}
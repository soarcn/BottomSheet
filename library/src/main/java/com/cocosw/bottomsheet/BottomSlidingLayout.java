package com.cocosw.bottomsheet;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;

public class BottomSlidingLayout extends FrameLayout {

    private String TAG = "BottomSlidingLayout.class";
    private final double AUTO_OPEN_SPEED_LIMIT = 800.0;
    private final int DEFAULT_TOP_POSTITION = 400;
    private final ViewDragHelper mDragHelper;

    private static final int STATE_IDLE = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_CLOSED = 3;

    private ViewGroup mSuperParent;
    private View mDragView;
    private int mSlideLayoutState = STATE_IDLE;

    private int mDraggingBorder;
    private int mDraggingState = 0;
    private int mVerticalRange;
    private boolean mIsOpen;

    public interface SlideListener {
        public void onOpen();

        public void onDragging();

        public void onClose();
    }

    private SlideListener mSlideListener;

    public BottomSlidingLayout(Context context) {
        this(context, null);
    }

    public BottomSlidingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public BottomSlidingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 0.8f, new ViewDragCallback());
    }

    public void setSlideListener(SlideListener listener) {
        this.mSlideListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        if (getChildCount() > 1) {
            throw new IllegalArgumentException("Can have only 1 parent view.");
        }
        mSuperParent = (ViewGroup) getChildAt(0);
        mDragView = mSuperParent;
        super.onFinishInflate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mVerticalRange = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mDragHelper.smoothSlideViewTo(mSuperParent, mSuperParent.getLeft(), DEFAULT_TOP_POSTITION);
    }

    @Override
    public void computeScroll() { // needed for automatic settling.
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    private boolean canChildScrollUp(View view) {
        if (view == null) {
            view = mSuperParent;
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return view.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(view, -1);
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public View findTopChildUnder(ViewGroup group, int x, int y) {
        final int childCount = group.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = group.getChildAt(i);
            if (x >= child.getLeft() && x < child.getRight() &&
                    y >= child.getTop() && y < child.getBottom()) {
                if (child instanceof AbsListView) {
                    return child;
                }
                if (child instanceof ViewGroup) {
                    ViewGroup innerGroup = (ViewGroup) child;
                    findTopChildUnder(innerGroup, x, y);
                }
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isDragViewTarget(event)) {
            mDragView = findTopChildUnder(mSuperParent, (int) event.getRawX(), (int) event.getRawY());
            if (canChildScrollUp(mDragView) && isOpen()) {
                return false;
            }
            return mDragHelper.shouldInterceptTouchEvent(event);
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isDragViewTarget(event) || isMoving()) {
            mDragHelper.processTouchEvent(event);
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    private boolean isDragViewTarget(MotionEvent event) {
        int[] dragViewLocation = new int[2];
        mSuperParent.getLocationOnScreen(dragViewLocation);
        int upperLimit = dragViewLocation[1] + mSuperParent.getMeasuredHeight();
        int lowerLimit = dragViewLocation[1];
        int y = (int) event.getRawY();
        return (y > lowerLimit && y < upperLimit);
    }

    public boolean isMoving() {
        return (mDraggingState == ViewDragHelper.STATE_DRAGGING ||
                mDraggingState == ViewDragHelper.STATE_SETTLING);
    }

    /**
     * Callback
     */
    private class ViewDragCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        @Override
        public void onViewDragStateChanged(int state) {

            if (state == mDraggingState) { // no change
                return;
            }

            mSlideLayoutState = STATE_IDLE;

            if ((mDraggingState == ViewDragHelper.STATE_DRAGGING || mDraggingState == ViewDragHelper.STATE_SETTLING) &&
                    state == ViewDragHelper.STATE_IDLE) {
                // the view stopped from moving.
                if (mDraggingBorder == 0) {
                    mIsOpen = true;
                    mSlideLayoutState = STATE_OPEN;
                } else if (mDraggingBorder == mVerticalRange) {
                    mIsOpen = false;
                    mSlideLayoutState = STATE_CLOSED;
                }
            }
            if (state == ViewDragHelper.STATE_DRAGGING) {
                mSlideLayoutState = STATE_DRAGGING;
            }
            dispatchSlideState(mSlideLayoutState);
            mDraggingState = state;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final float rangeToCheck = mVerticalRange;
            if (mDraggingBorder == 0) {
                mIsOpen = true;
                mSlideLayoutState = STATE_OPEN;
                return;
            }
            if (mDraggingBorder == rangeToCheck) {
                mIsOpen = false;
                mSlideLayoutState = STATE_CLOSED;
                return;
            }
            boolean settleToOpen = false;
            if (yvel > AUTO_OPEN_SPEED_LIMIT) { // speed has priority over position
                settleToOpen = true;
            } else if (yvel < -AUTO_OPEN_SPEED_LIMIT) {
                settleToOpen = false;
            } else if (mDraggingBorder > rangeToCheck / 2) {
                settleToOpen = true;
            } else if (mDraggingBorder < rangeToCheck / 2) {
                settleToOpen = false;
            }

            final int settleDestY = settleToOpen ? mVerticalRange : 0;
            if (mDragHelper.settleCapturedViewAt(0, settleDestY)) {
                ViewCompat.postInvalidateOnAnimation(BottomSlidingLayout.this);
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mDraggingBorder = top;
            if (mDraggingBorder == 0) {
                mSlideLayoutState = STATE_OPEN;
                requestLayout();
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                requestLayout();
            }
        }

        public int getViewVerticalDragRange(View child) {
            return mVerticalRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            final int bottomBound = mVerticalRange;
            return Math.min(Math.max(top, topBound), bottomBound);
        }
    }

    public boolean isOpen() {
        return mIsOpen;
    }

    private void dispatchSlideState(int state) {
        if (mSlideListener == null) {
            return;
        }
        switch (state) {
            case STATE_DRAGGING: {
                mSlideListener.onDragging();
                break;
            }
            case STATE_CLOSED: {
                mSlideListener.onClose();
                break;
            }
            case STATE_OPEN: {
                mSlideListener.onOpen();
                break;
            }
        }
    }

}
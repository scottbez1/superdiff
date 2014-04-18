package com.scottbezek.superdiff.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.ListView;
import android.widget.OverScroller;

/**
 * ListView that scrolls vertically normally, but allows pseudo scrolling
 * horizontally simulatneously. The horizontal pseudo-scrolling don't directly
 * affect any views in the list; instead you can specify the horizontal scroll
 * range and register for horizontal scroll change events and then use those to
 * adjust views inside the list.
 */
public class HorizontalScrollObservingListView extends ListView {

    public HorizontalScrollObservingListView(Context context) {
        super(context);
        initScrollView();
    }

    public HorizontalScrollObservingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScrollView();
    }

    public HorizontalScrollObservingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initScrollView();
    }


    private OverScroller mScroller;

    /**
     * X position of the last focal point.
     */
    private int mLastFocusX;

    /**
     * True if the value of mLastFocusX is still valid.
     */
    private boolean mHasLastFocusX;

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    private boolean mIsBeingDragged = false;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private int mScrollX;

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    private int getScrollRange() {
        // TODO(sbezek): this is probably user-supplied?
        return 100;
    }

    private void onHorizontalScrollChanged(int newX, int newY) {
        // TODO(sbezek): notify listener?
        // TODO(sbezek): is this method actually necessary? should I be using the normal onScrollChanged???
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
        * Shortcut the most recurring case: the user is in the dragging
        * state and he is moving his finger.  We want to intercept this
        * motion.
        */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                if (!mHasLastFocusX) {
                    // If we don't have a valid mLastFocusX, the touch down wasn't
                    // on content.
                    break;
                }

                final int x = getFocusX(ev);
                final int xDiff = Math.abs(x - mLastFocusX);
                if (xDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastFocusX = x;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                if (!inChild(x, (int) ev.getY())) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 */
                mLastFocusX = getFocusX(ev);
                mHasLastFocusX = true;

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);

                /*
                * If being flinged and user touches the screen, initiate drag;
                * otherwise don't.  mScroller.isFinished should be false when
                * being flinged.
                */
                mIsBeingDragged = !mScroller.isFinished();
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mIsBeingDragged = false;
                mHasLastFocusX = false;
                if (mScroller.springBack(mScrollX, /*mScrollY*/0, 0, getScrollRange(), 0, 0)) {
                    postInvalidateOnAnimation();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                mLastFocusX = getFocusX(ev);
                mHasLastFocusX = true;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastFocusX = getFocusX(ev);
                break;
        }

        /*
        * The only time we want to intercept motion events is if we are in the
        * drag mode.
        */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                if (getChildCount() == 0) {
                    return false;
                }
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastFocusX = getFocusX(ev);
                mHasLastFocusX = true;
                break;
            }
            case MotionEvent.ACTION_MOVE:
                final int x = getFocusX(ev);
                int deltaX = mLastFocusX - x;
                if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    mLastFocusX = x;

                    final int oldX = mScrollX;
                    final int range = getScrollRange();

//                    if (overScrollBy(deltaX, 0, mScrollX, 0, range, 0,
//                            mOverscrollDistance, 0, true)) {
//                        // Break our velocity if we hit a scroll barrier.
//                        mVelocityTracker.clear();
//                    }
                    // TODO(sbezek): maybe need to do the clamping manually?
                    onHorizontalScrollChanged(mScrollX, oldX);
//                    onScrollChanged(mScrollX, mScrollY, oldX, oldY);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    final int pointerId = ev.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getXVelocity(pointerId);

                    if (getChildCount() > 0) {
                        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                            fling(-initialVelocity);
                        } else {
                            if (mScroller.springBack(mScrollX, /*mScrollY*/0, 0,
                                    getScrollRange(), 0, 0)) {
                                postInvalidateOnAnimation();
                            }
                        }
                    }

                    mHasLastFocusX = false;
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    if (mScroller.springBack(mScrollX, /*mScrollY*/0, 0, getScrollRange(), 0, 0)) {
                        postInvalidateOnAnimation();
                    }
                    mHasLastFocusX = false;
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        mLastFocusX = getFocusX(ev);
        mHasLastFocusX = true;
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private static int getFocusX(MotionEvent ev) {
        final int action = ev.getAction();
        final boolean pointerUp =
                (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? ev.getActionIndex() : -1;

        // Determine focal point
        float sumX = 0;
        final int count = ev.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex == i) {
                continue;
            }
            sumX += ev.getX(i);
        }
        final int div = pointerUp ? count - 1 : count;
        final float focusX = sumX / div;
        return (int) focusX;
    }

    private boolean inChild(int x, int y) {
        // This method is from the HorizontalScrollView; for us, we always assume the touch is on "content"
        return true;
    }

    /**
     * Fling the scroll view
     *
     * @param velocityX The initial velocity in the X direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the left.
     */
    public void fling(int velocityX) {
            int width = getWidth() - mPaddingRight - mPaddingLeft;
            int right = getChildAt(0).getWidth();

            mScroller.fling(mScrollX, mScrollY, velocityX, 0, 0,
                    Math.max(0, right - width), 0, 0, width/2, 0);

            postInvalidateOnAnimation();
    }

}

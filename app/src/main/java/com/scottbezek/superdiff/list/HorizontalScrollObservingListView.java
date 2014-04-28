package com.scottbezek.superdiff.list;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.ListView;
import android.widget.OverScroller;

import com.scottbezek.util.AndroidAssert;
import com.scottbezek.util.Assert;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * ListView that scrolls vertically normally, but allows pseudo scrolling
 * horizontally simultaneously. The horizontal pseudo-scrolling doesn't directly
 * affect any views in the list; instead you can specify the horizontal scroll
 * range and register for horizontal scroll change events and then use those to
 * adjust views inside the list.
 * <p>
 * This must be done at the ListView level, because any child view would
 * otherwise have its touches intercepted by the parent ListView whenever
 * there's a large enough vertical component.
 */
public class HorizontalScrollObservingListView extends ListView implements HorizontalScrollController {

    private final OverScroller mScroller;
    private final int mTouchSlop;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;

    private final Set<HorizontalScrollListener> mScrollListeners = new HashSet<HorizontalScrollListener>();

    public HorizontalScrollObservingListView(Context context) {
        this(context, null);
    }

    public HorizontalScrollObservingListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalScrollObservingListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }


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

    private int mScrollX;
    private int mScrollRange;


    @Override
    public void setHorizontalScrollRange(int range) {
        AndroidAssert.mainThreadOnly();
        mScrollRange = range;
        // TODO(sbezek): need to adjust mScrollX here?
    }

    @Override
    public void registerHorizontalScrollListener(@Nonnull HorizontalScrollListener listener) {
        AndroidAssert.mainThreadOnly();
        Assert.notNull(listener);
        Assert.isTrue(mScrollListeners.add(listener));
    }

    @Override
    public void unregisterHorizontalScrollListener(@Nonnull HorizontalScrollListener listener) {
        AndroidAssert.mainThreadOnly();
        Assert.notNull(listener);
        Assert.isTrue(mScrollListeners.remove(listener));
    }

    @Override
    public int getHorizontalScrollPosition() {
        AndroidAssert.mainThreadOnly();
        return mScrollX;
    }

    private void onHorizontalScrollChanged(int newX, int oldX) {
        AndroidAssert.mainThreadOnly();
        for (HorizontalScrollListener listener : mScrollListeners) {
            listener.onHorizontalScroll(newX, oldX);
        }
    }

    private int getScrollRange() {
        return mScrollRange;
    }

    // TODO(sbezek): would it be possible to simplify things with a GestureDetector here instead?
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        boolean superIntercept = super.onInterceptTouchEvent(ev);

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
                    ViewCompat.postInvalidateOnAnimation(this);
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
        return mIsBeingDragged || superIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
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

                    if (fauxOverScrollBy(deltaX, mScrollX, range)) {
                        // Break our velocity if we hit a scroll barrier.
                        mVelocityTracker.clear();
                    }
                    onHorizontalScrollChanged(mScrollX, oldX);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    final int pointerId = ev.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getXVelocity(pointerId);

                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        fling(-initialVelocity);
                    } else {
                        if (mScroller.springBack(mScrollX, 0, 0,
                                getScrollRange(), 0, 0)) {
                            ViewCompat.postInvalidateOnAnimation(this);
                        }
                    }

                    mHasLastFocusX = false;
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    if (mScroller.springBack(mScrollX, 0, 0, getScrollRange(), 0, 0)) {
                        ViewCompat.postInvalidateOnAnimation(this);
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

    private boolean fauxOverScrollBy(int deltaX, int scrollX, int scrollRangeX) {
        boolean clamped = false;

        int result = scrollX + deltaX;
        if (result < 0) {
            result = 0;
            clamped = true;
        } else if (result > scrollRangeX) {
            result = scrollRangeX;
            clamped = true;
        }

        mScrollX = result;

        return clamped;
    }

    /**
     * Fling the scroll view
     *
     * @param velocityX The initial velocity in the X direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the left.
     */
    public void fling(int velocityX) {
            int maxX = getScrollRange();

            mScroller.fling(mScrollX, 0, velocityX, 0, 0,
                    maxX, 0, 0, maxX/2, 0);

            ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        computeFauxScroll();
    }

    private void computeFauxScroll() {
            if (mScroller.computeScrollOffset()) {
                // This is called at drawing time by ViewGroup.  We don't want to
                // re-show the scrollbars at this point, which scrollTo will do,
                // so we replicate most of scrollTo here.
                //
                //         It's a little odd to call onScrollChanged from inside the drawing.
                //
                //         It is, except when you remember that computeScroll() is used to
                //         animate scrolling. So unless we want to defer the onScrollChanged()
                //         until the end of the animated scrolling, we don't really have a
                //         choice here.
                //
                //         I agree.  The alternative, which I think would be worse, is to post
                //         something and tell the subclasses later.  This is bad because there
                //         will be a window where mScrollX/Y is different from what the app
                //         thinks it is.
                //
                int oldX = mScrollX;
                int x = mScroller.getCurrX();

                if (oldX != x) {
                    final int range = getScrollRange();
                    fauxOverScrollBy(x - oldX, oldX, range);
                    onHorizontalScrollChanged(mScrollX, oldX);
                }

                ViewCompat.postInvalidateOnAnimation(this);
            }
    }


    public interface HorizontalScrollListener {
        void onHorizontalScroll(int newX, int oldX);
    }
}

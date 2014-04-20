package com.scottbezek.superdiff.list;

import javax.annotation.Nonnull;

import com.scottbezek.superdiff.list.HorizontalScrollObservingListView.HorizontalScrollListener;

/**
 * Controls horizontal scrolling of some other object. All methods must only be
 * called from the UI thread.
 *
 * @see HorizontalScrollObservingListView
 */
public interface HorizontalScrollController {

    void setHorizontalScrollRange(int range);

    void registerHorizontalScrollListener(@Nonnull HorizontalScrollListener listener);

    void unregisterHorizontalScrollListener(@Nonnull HorizontalScrollListener listener);

    int getHorizontalScrollPosition();
}
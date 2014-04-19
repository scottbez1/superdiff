package com.scottbezek.superdiff.list;

import javax.annotation.Nonnull;

import com.scottbezek.superdiff.list.HorizontalScrollObservingListView.HorizontalScrollListener;

/**
 * Controls horizontal scrolling of some other object.
 *
 * @see HorizontalScrollObservingListView
 */
public interface HorizontalScrollController {

    void setHorizontalScrollRange(int range);

    void setHorizontalScrollListener(@Nonnull HorizontalScrollListener listener);

    int getHorizontalScrollPosition();
}
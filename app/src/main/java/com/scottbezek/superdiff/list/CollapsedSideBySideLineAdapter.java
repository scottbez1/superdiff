package com.scottbezek.superdiff.list;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.scottbezek.superdiff.R;
import com.scottbezek.superdiff.list.HorizontalScrollObservingListView.HorizontalScrollListener;
import com.scottbezek.superdiff.list.SideBySideLineView.ItemWidths;
import com.scottbezek.difflib.unified.SideBySideLine;
import com.scottbezek.util.Assert;

public class CollapsedSideBySideLineAdapter extends BaseAdapter {

    private final String mFilename;
    private final List<CollapsedOrLine> mItems;
    private final ItemWidths mItemWidthInfo;
    private final HorizontalScrollController mScrollController;
    private final Set<SideBySideLineView> mAttachedViews = new HashSet<SideBySideLineView>();

    private final OnAttachStateChangeListener mRowAttachStateListener = new OnAttachStateChangeListener() {

        @Override
        public void onViewAttachedToWindow(View v) {
            SideBySideLineView lineView = (SideBySideLineView)v;
            Assert.isTrue(mAttachedViews.add(lineView));
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            SideBySideLineView lineView = (SideBySideLineView)v;
            Assert.isTrue(mAttachedViews.remove(lineView));
        }
    };

    private final HorizontalScrollListener mHorizontalScrollListener = new HorizontalScrollListener() {

        @Override
        public void onHorizontalScroll(int newX, int oldX) {
            for (SideBySideLineView lineView : mAttachedViews) {
                lineView.setPseudoScrollX(newX);
            }
        }
    };

    public CollapsedSideBySideLineAdapter(@Nonnull String filename,
            @Nonnull List<CollapsedOrLine> items,
            @Nonnull ItemWidths itemWidthInfo,
            @Nonnull HorizontalScrollController scrollController) {
        mFilename = filename;
        mItems = items;
        mItemWidthInfo = itemWidthInfo;
        mScrollController = scrollController;
        mScrollController.registerHorizontalScrollListener(mHorizontalScrollListener);
    }

    @Override
    public int getCount() {
        return 1 + mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == 0) {
            return getFileTitleView(convertView, parent);
        }

        CollapsedOrLine item = (CollapsedOrLine)getItem(position);
        if (item.isCollapsed()) {
            return getCollapsedView(item.getCollapsed(), convertView, parent);
        } else {
            return getLineView(item.getLine(), convertView, parent);
        }
    }

    private View getFileTitleView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.file_title_line_item, parent, false);
        }

        final TextView fileTitle = (TextView)convertView.findViewById(R.id.file_title);
        fileTitle.setText(mFilename);

        return convertView;
    }

    private View getLineView(SideBySideLine line, View convertView, ViewGroup parent) {
        final SideBySideLineView view;
        if (convertView == null) {
            view = new SideBySideLineView(parent.getContext());
            view.setItemWidths(mItemWidthInfo);
            view.addOnAttachStateChangeListener(mRowAttachStateListener);
        } else {
            view = (SideBySideLineView)convertView;
        }

        view.setLine(line);

        view.setPseudoScrollX(mScrollController.getHorizontalScrollPosition());
        return view;
    }

    private View getCollapsedView(Collapsed collapsed, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.collapsed_line_item, parent, false);
        }

        final Resources resources = parent.getContext().getResources();

        final TextView dummyText = (TextView)convertView.findViewById(R.id.collapsed_line_count);
        dummyText.setText(resources.getQuantityString(
                R.plurals.collapsed_lines, collapsed.size(), collapsed.size()));

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            // File title row
            return 2;
        }
        CollapsedOrLine item = (CollapsedOrLine)getItem(position);
        if (item.isCollapsed()) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Immutable
    public static class SingleFileDiffData {

        private final List<CollapsedOrLine> mItems;
        private final ItemWidths mItemWidthInfo;

        public SingleFileDiffData(List<CollapsedOrLine> items, ItemWidths itemWidthInfo) {
            mItems = items;
            mItemWidthInfo = itemWidthInfo;
        }

        public List<CollapsedOrLine> getItems() {
            return mItems;
        }

        public ItemWidths getitemWidths() {
            return mItemWidthInfo;
        }
    }

    /**
     * A collapsed section of the diff.
     */
    public interface Collapsed {

        /**
         * Number of lines collapsed.
         */
        public int size();
    }

    /**
     * A collapsed set of {@link SideBySideLine}s, to be represented as a single
     * small row in the diff view.
     */
    public static class CollapsedLines implements Collapsed {

        private final List<SideBySideLine> mLines;

        public CollapsedLines(List<SideBySideLine> collapsedLines) {
            mLines = collapsedLines;
        }

        @Override
        public int size() {
            return mLines.size();
        }
    }

    /**
     * Collapsed section of the diff with unknown actual line contents.
     */
    public static class CollapsedUnknown implements Collapsed {

        private final int mNumLines;

        public CollapsedUnknown(int numLines) {
            mNumLines = numLines;
        }

        @Override
        public int size() {
            return mNumLines;
        }
    }

    public static class CollapsedOrLine {

        private final Collapsed mCollapsed;
        private final SideBySideLine mLine;

        private CollapsedOrLine(@CheckForNull Collapsed collapsed,
                @CheckForNull SideBySideLine line) {
            Assert.isTrue((collapsed == null) ^ (line == null));
            mCollapsed = collapsed;
            mLine = line;
        }

        public static CollapsedOrLine of(Collapsed collapsed) {
            return new CollapsedOrLine(collapsed, null);
        }

        public static CollapsedOrLine of(SideBySideLine line) {
            return new CollapsedOrLine(null, line);
        }

        public boolean isCollapsed() {
            return mCollapsed != null;
        }

        public Collapsed getCollapsed() {
            return mCollapsed;
        }

        public SideBySideLine getLine() {
            return mLine;
        }
    }
}

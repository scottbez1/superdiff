package com.scottbezek.superdiff.list;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.scottbezek.util.Assert;

/**
 * A fairly naive multiplexing adapter, made up of any number of
 * {@link CollapsedSideBySideLineAdapter}s in order to display multiple diffs in
 * a single ListView.
 */
public class MultiFileDiffAdapter extends BaseAdapter {

    private final List<CollapsedSideBySideLineAdapter> mAdapters;

    public MultiFileDiffAdapter(List<CollapsedSideBySideLineAdapter> adapters) {
        mAdapters = adapters;
    }

    @Override
    public int getCount() {
        int sum = 0;
        for (CollapsedSideBySideLineAdapter adapter : mAdapters) {
            sum += adapter.getCount();
        }
        return sum;
    }

    @Override
    public Object getItem(int position) {
        for (CollapsedSideBySideLineAdapter adapter : mAdapters) {
            if (position >= adapter.getCount()) {
                position -= adapter.getCount();
            } else {
                return adapter.getItem(position);
            }
        }
        throw Assert.fail("invalid position");
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        for (CollapsedSideBySideLineAdapter adapter : mAdapters) {
            if (position >= adapter.getCount()) {
                position -= adapter.getCount();
            } else {
                return adapter.getView(position, convertView, parent);
            }
        }
        throw Assert.fail("invalid position");
    }

    @Override
    public int getViewTypeCount() {
        int sum = 0;
        for (CollapsedSideBySideLineAdapter adapter : mAdapters) {
            sum += adapter.getViewTypeCount();
        }
        return sum;
    }

    @Override
    public int getItemViewType(int position) {
        /*
         * Give every sub-adapter it's own "namespace" of view type ids, even
         * though they're a homogeneous type.
         *
         * In theory it would be preferable to have them all share the view
         * types (and thus share recycled views with one another), but then it
         * would no longer make sense for them to each individually define the
         * getView methods, and the concept of View "ownership" by the adapter
         * would need to be adjusted accordingly. It's unclear what that would
         * look like, and doesn't seem necessary for now.
         */
        int itemViewTypeOffset = 0;
        for (CollapsedSideBySideLineAdapter adapter : mAdapters) {
            if (position >= adapter.getCount()) {
                position -= adapter.getCount();
                itemViewTypeOffset += adapter.getViewTypeCount();
            } else {
                return itemViewTypeOffset + adapter.getItemViewType(position);
            }
        }
        throw Assert.fail("invalid position");
    }

}

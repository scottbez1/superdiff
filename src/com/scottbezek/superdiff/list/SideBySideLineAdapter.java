package com.scottbezek.superdiff.list;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.scottbezek.superdiff.unified.SideBySideLine;

public class SideBySideLineAdapter extends BaseAdapter {

    private final List<SideBySideLine> mLines;

    public SideBySideLineAdapter(List<SideBySideLine> lines) {
        mLines = lines;
    }

    @Override
    public int getCount() {
        return mLines.size();
    }

    @Override
    public Object getItem(int position) {
        return mLines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final SideBySideLineView view;
        if (convertView == null) {
            view = new SideBySideLineView(parent.getContext());
        } else {
            view = (SideBySideLineView)convertView;
        }

        final SideBySideLine line = mLines.get(position);
        view.setLine(line);
        return view;
    }

}
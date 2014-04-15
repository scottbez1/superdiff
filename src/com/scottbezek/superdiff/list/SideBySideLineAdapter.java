package com.scottbezek.superdiff.list;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.scottbezek.superdiff.unified.SideBySideLine;

public class SideBySideLineAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<SideBySideLine> mLines;

    public SideBySideLineAdapter(Context context, List<SideBySideLine> lines) {
        mContext = context;
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
            view = new SideBySideLineView(mContext);
        } else {
            view = (SideBySideLineView)convertView;
        }

        final SideBySideLine line = mLines.get(position);
        view.setLine(line);
        return view;
    }

}

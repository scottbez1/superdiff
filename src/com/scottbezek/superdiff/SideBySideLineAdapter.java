package com.scottbezek.superdiff;

import java.util.List;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.scottbezek.superdiff.unified.SideBySideLine;
import com.scottbezek.util.Assert;

public class SideBySideLineAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;
    private final List<SideBySideLine> mLines;

    private final int mNormalBackgroundColor;
    private final int mEmptyBackgroundColor;
    private final int mRemovedBackgroundColor;
    private final int mAddedBackgroundColor;

    public SideBySideLineAdapter(LayoutInflater layoutInflater, List<SideBySideLine> lines) {
        mLayoutInflater = layoutInflater;
        mLines = lines;

        Resources resources = layoutInflater.getContext().getResources();

        mNormalBackgroundColor = resources.getColor(R.color.diff_line_normal_background);
        mEmptyBackgroundColor = resources.getColor(R.color.diff_line_empty_background);
        mRemovedBackgroundColor = resources.getColor(R.color.diff_line_removed_background);
        mAddedBackgroundColor = resources.getColor(R.color.diff_line_added_background);
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
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.side_by_side_line_item, parent, false);
        }
        bindView(convertView, position);
        return convertView;
    }

    private void bindView(View view, int position) {
        final SideBySideLine line = mLines.get(position);

        final TextView leftLineNumber = (TextView)view.findViewById(R.id.left_line_number);
        final TextView leftContents = (TextView)view.findViewById(R.id.left_line_contents);

        if (line.hasLeft()) {
            leftLineNumber.setText(String.valueOf(line.getLeftLineNumber()));
            leftContents.setText(line.getLeftLine());
        } else {
            leftLineNumber.setText("");
            leftContents.setText("");
        }

        final TextView rightLineNumber = (TextView)view.findViewById(R.id.right_line_number);
        final TextView rightContents = (TextView)view.findViewById(R.id.right_line_contents);

        if (line.hasRight()) {
            rightLineNumber.setText(String.valueOf(line.getRightLineNumber()));
            rightContents.setText(line.getRightLine());
        } else {
            rightLineNumber.setText("");
            rightContents.setText("");
        }

        final int leftBackgroundColor;
        final int rightBackgroundColor;
        if (line.hasLeft() && line.hasRight()) {
            if (line.getLeftLine().equals(line.getRightLine())) {
                leftBackgroundColor = mNormalBackgroundColor;
                rightBackgroundColor = mNormalBackgroundColor;
            } else {
                leftBackgroundColor = mRemovedBackgroundColor;
                rightBackgroundColor = mAddedBackgroundColor;
            }
        } else if (line.hasLeft() ^ line.hasRight()){
            if (line.hasLeft()) {
                leftBackgroundColor = mRemovedBackgroundColor;
                rightBackgroundColor = mEmptyBackgroundColor;
            } else {
                leftBackgroundColor = mEmptyBackgroundColor;
                rightBackgroundColor = mAddedBackgroundColor;
            }
        } else {
            throw Assert.fail("diff line has neither left nor right");
        }

        leftContents.setBackgroundColor(leftBackgroundColor);
        rightContents.setBackgroundColor(rightBackgroundColor);
    }

}

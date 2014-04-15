package com.scottbezek.superdiff.list;

import android.content.Context;
import android.content.res.Resources;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scottbezek.superdiff.R;
import com.scottbezek.superdiff.unified.SideBySideLine;
import com.scottbezek.util.Assert;

public class SideBySideLineView extends LinearLayout {

    private final int mNormalBackgroundColor;
    private final int mEmptyBackgroundColor;
    private final int mRemovedBackgroundColor;
    private final int mAddedBackgroundColor;

    public SideBySideLineView(Context context) {
        super(context);
        inflate();

        final Resources resources = context.getResources();
        mNormalBackgroundColor = resources.getColor(R.color.diff_line_normal_background);
        mEmptyBackgroundColor = resources.getColor(R.color.diff_line_empty_background);
        mRemovedBackgroundColor = resources.getColor(R.color.diff_line_removed_background);
        mAddedBackgroundColor = resources.getColor(R.color.diff_line_added_background);
    }

    private void inflate() {
        setOrientation(LinearLayout.HORIZONTAL);
        inflate(getContext(), R.layout.side_by_side_line_item, this);
    }

    public void setLine(SideBySideLine line) {
        final TextView leftLineNumber = (TextView)findViewById(R.id.left_line_number);
        final TextView leftContents = (TextView)findViewById(R.id.left_line_contents);

        if (line.hasLeft()) {
            leftLineNumber.setText(String.valueOf(line.getLeftLineNumber()));
            leftContents.setText(line.getLeftLine());
        } else {
            leftLineNumber.setText("");
            leftContents.setText("");
        }

        final TextView rightLineNumber = (TextView)findViewById(R.id.right_line_number);
        final TextView rightContents = (TextView)findViewById(R.id.right_line_contents);

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

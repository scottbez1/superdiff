package com.scottbezek.superdiff.list;

import com.scottbezek.difflib.unified.SideBySideLine;
import com.scottbezek.superdiff.R;
import com.scottbezek.util.Assert;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.annotation.concurrent.Immutable;

public class SideBySideLineView extends LinearLayout {

    private final int mNormalBackgroundColor;
    private final int mEmptyBackgroundColor;
    private final int mRemovedBackgroundColor;
    private final int mAddedBackgroundColor;
    private final int mRemovedCharactersBackgroundColor;
    private final int mAddedCharactersBackgroundColor;

    private final View mLeftContainer;
    private final TextView mLeftLineNumber;
    private final TextView mLeftContents;
    private final View mRightContainer;
    private final TextView mRightLineNumber;
    private final TextView mRightContents;

    public SideBySideLineView(Context context) {
        super(context);

        setOrientation(LinearLayout.HORIZONTAL);
        inflate(getContext(), R.layout.side_by_side_line_item, this);

        mLeftContainer = findViewById(R.id.left_container);
        mLeftLineNumber = (TextView)findViewById(R.id.left_line_number);
        mLeftContents = (TextView)findViewById(R.id.left_line_contents);
        mRightContainer = findViewById(R.id.right_container);
        mRightLineNumber = (TextView)findViewById(R.id.right_line_number);
        mRightContents = (TextView)findViewById(R.id.right_line_contents);

        final Resources resources = context.getResources();
        mNormalBackgroundColor = resources.getColor(R.color.diff_line_normal_background);
        mEmptyBackgroundColor = resources.getColor(R.color.diff_line_empty_background);
        mRemovedBackgroundColor = resources.getColor(R.color.diff_line_removed_background);
        mAddedBackgroundColor = resources.getColor(R.color.diff_line_added_background);

        mRemovedCharactersBackgroundColor = resources.getColor(R.color.diff_chars_removed_background);
        mAddedCharactersBackgroundColor = resources.getColor(R.color.diff_chars_added_background);
    }

    public void setLine(SideBySideLine line) {
        CharSequence leftLine = line.getLeftLine();
        CharSequence rightLine = line.getRightLine();

        final int leftBackgroundColor;
        final int rightBackgroundColor;
        if (leftLine != null && rightLine != null) {
            if (leftLine.equals(rightLine)) {
                leftBackgroundColor = mNormalBackgroundColor;
                rightBackgroundColor = mNormalBackgroundColor;
            } else {
                leftBackgroundColor = mRemovedBackgroundColor;
                rightBackgroundColor = mAddedBackgroundColor;
            }
        } else if ((leftLine == null) ^ (rightLine == null)){
            if (leftLine != null) {
                leftBackgroundColor = mRemovedCharactersBackgroundColor;
                rightBackgroundColor = mEmptyBackgroundColor;
            } else {
                leftBackgroundColor = mEmptyBackgroundColor;
                rightBackgroundColor = mAddedCharactersBackgroundColor;
            }
        } else {
            throw Assert.fail("diff line has neither left nor right");
        }
        mLeftContents.setBackgroundColor(leftBackgroundColor);
        mRightContents.setBackgroundColor(rightBackgroundColor);

        if (leftLine != null) {
            mLeftLineNumber.setText(String.valueOf(line.getLeftLineNumber()));
            mLeftContents.setText(leftLine);
        } else {
            mLeftLineNumber.setText("");
            mLeftContents.setText("");
        }

        if (rightLine != null) {
            mRightLineNumber.setText(String.valueOf(line.getRightLineNumber()));
            mRightContents.setText(rightLine);
        } else {
            mRightLineNumber.setText("");
            mRightContents.setText("");
        }
    }

    public void setItemWidths(ItemWidths widths) {
        android.view.ViewGroup.LayoutParams lp;

        lp = mLeftLineNumber.getLayoutParams();
        lp.width = widths.getLineNumberWidthPx();
        mLeftLineNumber.setLayoutParams(lp);

        lp = mRightLineNumber.getLayoutParams();
        lp.width = widths.getLineNumberWidthPx();
        mRightLineNumber.setLayoutParams(lp);


        lp = mLeftContents.getLayoutParams();
        lp.width = widths.getLineContentsWidthPx();
        mLeftContents.setLayoutParams(lp);

        lp = mRightContents.getLayoutParams();
        lp.width = widths.getLineContentsWidthPx();
        mRightContents.setLayoutParams(lp);
    }

    public void setPseudoScrollX(int scrollX) {
        mLeftContainer.setScrollX(scrollX);
        mRightContainer.setScrollX(scrollX);
    }

    @Immutable
    public static class ItemWidths {
        private final int mLineNumberWidthPx;
        private final int mLineContentsWidthPx;

        public ItemWidths(int lineNumberWidthPx, int lineContentsWidthPx) {
            mLineNumberWidthPx = lineNumberWidthPx;
            mLineContentsWidthPx = lineContentsWidthPx;
        }

        public int getLineNumberWidthPx() {
            return mLineNumberWidthPx;
        }

        public int getLineContentsWidthPx() {
            return mLineContentsWidthPx;
        }
    }
}

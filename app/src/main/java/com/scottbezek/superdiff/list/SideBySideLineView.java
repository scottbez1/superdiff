package com.scottbezek.superdiff.list;

import javax.annotation.concurrent.Immutable;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scottbezek.superdiff.R;
import com.scottbezek.difflib.compute.DiffCatalog;
import com.scottbezek.difflib.compute.LcsDiff;
import com.scottbezek.difflib.compute.Region;
import com.scottbezek.difflib.unified.SideBySideLine;
import com.scottbezek.util.Assert;

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
        String leftLine = line.getLeftLine();
        String rightLine = line.getRightLine();

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
                leftBackgroundColor = mRemovedBackgroundColor;
                rightBackgroundColor = mEmptyBackgroundColor;
            } else {
                leftBackgroundColor = mEmptyBackgroundColor;
                rightBackgroundColor = mAddedBackgroundColor;
            }
        } else {
            throw Assert.fail("diff line has neither left nor right");
        }
        mLeftContents.setBackgroundColor(leftBackgroundColor);
        mRightContents.setBackgroundColor(rightBackgroundColor);

        Spannable leftSpan = null;
        Spannable rightSpan = null;
        if (leftLine != null) {
            leftSpan = new SpannableString(leftLine);
        }
        if (rightLine != null) {
            rightSpan = new SpannableString(rightLine);
        }

        if (leftLine != null && rightLine != null) {
            LcsDiff diffCalc = new LcsDiff();
            DiffCatalog diff = diffCalc.computeDiff(leftLine, rightLine);
            for (Region r : diff.getLeftUniqueRegions()) {
                leftSpan.setSpan(new BackgroundColorSpan(mRemovedCharactersBackgroundColor), r.getStart(), r.getStart() + r.getLength(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for (Region r : diff.getRightUniqueRegions()) {
                rightSpan.setSpan(new BackgroundColorSpan(mAddedCharactersBackgroundColor), r.getStart(), r.getStart() + r.getLength(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (leftLine != null) {
            mLeftLineNumber.setText(String.valueOf(line.getLeftLineNumber()));
            mLeftContents.setText(leftSpan);
        } else {
            mLeftLineNumber.setText("");
            mLeftContents.setText("");
        }

        if (rightLine != null) {
            mRightLineNumber.setText(String.valueOf(line.getRightLineNumber()));
            mRightContents.setText(rightSpan);
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

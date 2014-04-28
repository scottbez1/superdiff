package com.scottbezek.difflib.unified;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents an "absolute" line in the side-by-side diff.
 */
@Immutable
public class SideBySideLine {

    private final int mLeftLineNumber;
    private final String mLeftLine;
    private final int mRightLineNumber;
    private final String mRightLine;

    public SideBySideLine(int leftLineNumber, @CheckForNull String leftLine,
            int rightLineNumber, @CheckForNull String rightLine) {
        mLeftLineNumber = leftLineNumber;
        mLeftLine = leftLine;
        mRightLineNumber = rightLineNumber;
        mRightLine = rightLine;
    }

    public int getLeftLineNumber() {
        return mLeftLineNumber;
    }

    @CheckForNull
    public String getLeftLine() {
        return mLeftLine;
    }

    public int getRightLineNumber() {
        return mRightLineNumber;
    }

    @CheckForNull
    public String getRightLine() {
        return mRightLine;
    }

    @Override
    public String toString() {
        return mLeftLineNumber + ":" + mLeftLine + "\t" + mRightLineNumber + ":" + mRightLine;
    }
}

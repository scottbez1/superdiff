package com.scottbezek.difflib.unified;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Represents an "absolute" line in the side-by-side diff.
 */
@Immutable
public class SideBySideLine {

    private final int mLeftLineNumber;
    private final CharSequence mLeftLine;
    private final int mRightLineNumber;
    private final CharSequence mRightLine;

    public SideBySideLine(int leftLineNumber, @CheckForNull CharSequence leftLine,
            int rightLineNumber, @CheckForNull CharSequence rightLine) {
        mLeftLineNumber = leftLineNumber;
        mLeftLine = leftLine;
        mRightLineNumber = rightLineNumber;
        mRightLine = rightLine;
    }

    public int getLeftLineNumber() {
        return mLeftLineNumber;
    }

    @CheckForNull
    public CharSequence getLeftLine() {
        return mLeftLine;
    }

    public int getRightLineNumber() {
        return mRightLineNumber;
    }

    @CheckForNull
    public CharSequence getRightLine() {
        return mRightLine;
    }

    @Override
    public String toString() {
        return mLeftLineNumber + ":" + mLeftLine + "\t" + mRightLineNumber + ":" + mRightLine;
    }
}

package com.scottbezek.superdiff.diffcompute;

public class Region {

    private final int mStart;
    private final int mLength;

    public Region(int start, int length) {
        mStart = start;
        mLength = length;
    }

    public int getStart() {
        return mStart;
    }

    public int getLength() {
        return mLength;
    }
}

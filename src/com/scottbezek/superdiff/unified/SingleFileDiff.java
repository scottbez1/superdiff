package com.scottbezek.superdiff.unified;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleFileDiff {

    private final String mLeftFilename;
    private final String mRightFilename;
    private final List<Chunk> mChunks;

    public SingleFileDiff(String leftFilename, String rightFilename, List<Chunk> chunks) {
        mLeftFilename = leftFilename;
        mRightFilename = rightFilename;
        mChunks = Collections.unmodifiableList(new ArrayList<Chunk>(chunks));
    }

    public List<Chunk> getChunks() {
        return mChunks;
    }

    public static class Builder {

        private String mLeftFilename = null;
        private String mRightFilename = null;

        private final List<Chunk> mChunks = new ArrayList<Chunk>();

        public Builder setLeftFilename(String filename) {
            if (mLeftFilename != null) {
                throw new IllegalStateException("Can't set filename again");
            }
            mLeftFilename = filename;
            return this;
        }

        public Builder setRightFilename(String filename) {
            if (mRightFilename != null) {
                throw new IllegalStateException("Can't set filename again");
            }
            mRightFilename = filename;
            return this;
        }

        public Builder addChunk(Chunk chunk) {
            // XXX assert that the difference in this chunk's start lines is equal to the cumulative chunk length difference so far

            if (mLeftFilename == null || mRightFilename == null) {
                throw new IllegalStateException("Must set both filenames before adding chunks");
            }
            mChunks.add(chunk);
            return this;
        }

        public SingleFileDiff build() {
            if (mLeftFilename == null || mRightFilename == null) {
                throw new IllegalStateException("Missing filename");
            }
            return new SingleFileDiff(mLeftFilename, mRightFilename, mChunks);
        }
    }
}

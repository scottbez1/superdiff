package com.scottbezek.superdiff.unified;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.scottbezek.superdiff.unified.Parser.DiffParseException;

public class SingleFileDiff {

    @SuppressWarnings("unused") // TODO(sbezek): these should maybe be moved elsewhere?
    private final String mLeftFilename, mRightFilename;

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

        public Builder setLeftFilename(String filename) throws DiffParseException {
            if (mLeftFilename != null) {
                throw new DiffParseException("Can't set filename again");
            }
            mLeftFilename = filename;
            return this;
        }

        public Builder setRightFilename(String filename) throws DiffParseException {
            if (mRightFilename != null) {
                throw new DiffParseException("Can't set filename again");
            }
            mRightFilename = filename;
            return this;
        }

        public Builder addChunk(Chunk chunk) throws DiffParseException {
            // XXX assert that the difference in this chunk's start lines is equal to the cumulative chunk length difference so far

            if (mLeftFilename == null || mRightFilename == null) {
                throw new DiffParseException("Must set both filenames before adding chunks");
            }
            mChunks.add(chunk);
            return this;
        }

        public SingleFileDiff build() throws DiffParseException {
            if (mLeftFilename == null || mRightFilename == null) {
                throw new DiffParseException("Missing filename");
            }
            return new SingleFileDiff(mLeftFilename, mRightFilename, mChunks);
        }
    }
}

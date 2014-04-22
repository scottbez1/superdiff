package com.scottbezek.superdiff.unified;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.scottbezek.superdiff.unified.Chunk.Block.Delta;
import com.scottbezek.superdiff.unified.Chunk.Block.Unchanged;
import com.scottbezek.superdiff.unified.Parser.DiffParseException;
import com.scottbezek.util.Assert;

@Immutable
public class Chunk implements IForwardApplicable {

    private final int mLeftStartLine;
    private final List<Block> mBlocks;

    private Chunk(int leftStartLine, List<Block> blocks) {
        mLeftStartLine = leftStartLine;
        mBlocks = blocks;
    }

    public int getLeftStartLine() {
        return mLeftStartLine;
    }

    @Override
    public List<SideBySideLine> applyForward(ILineReader leftFile) {
        List<SideBySideLine> output = new ArrayList<SideBySideLine>();
        for (Block block : mBlocks) {
            output.addAll(block.applyForward(leftFile));
        }
        return output;
    }

    public static class Builder {

        private final int mLeftStartLine;
        private final int mLeftLength;
        private final int mRightStartLine;
        private final int mRightLength;

        private final List<Block> mBlocks = new ArrayList<Block>();

        private Delta.Builder mCurrentDeltaBuilder;
        private Unchanged.Builder mCurrentUnchangedBuilder;

        private int mLeftLinesProcessed = 0;
        private int mRightLinesProcessed = 0;

        public Builder(int leftStartLine, int leftLength, int rightStartLine, int rightLength) {
            mLeftStartLine = leftStartLine;
            mLeftLength = leftLength;
            mRightStartLine = rightStartLine;
            mRightLength = rightLength;
        }

        public boolean isComplete() {
            return mLeftLinesProcessed == mLeftLength && mRightLinesProcessed == mRightLength;
        }

        private void assertSize() throws DiffParseException {
            if (mLeftLinesProcessed > mLeftLength) {
                throw new DiffParseException("More left lines than expected!");
            }
            if (mRightLinesProcessed > mRightLength) {
                throw new DiffParseException("More right lines than expected!");
            }
        }

        private int getCurrentLeftLine() {
            return mLeftStartLine + mLeftLinesProcessed;
        }

        private int getCurrentRightLine() {
            return mRightStartLine + mRightLinesProcessed;
        }

        private void finishDeltaBlock() {
            if (mCurrentDeltaBuilder != null) {
                mBlocks.add(mCurrentDeltaBuilder.build());
                mCurrentDeltaBuilder = null;
            }
        }

        @SuppressWarnings("null")
        @Nonnull
        private Unchanged.Builder prepareUnchangedBuilder() {
            // Finish the delta block if we were building one
            finishDeltaBlock();

            if (mCurrentUnchangedBuilder == null) {
                mCurrentUnchangedBuilder = new Unchanged.Builder(getCurrentLeftLine(), getCurrentRightLine());
            }
            return mCurrentUnchangedBuilder;
        }

        public void appendLineUnchanged(String line) throws DiffParseException {
            prepareUnchangedBuilder().appendLine(line);
            mLeftLinesProcessed++;
            mRightLinesProcessed++;
            assertSize();
        }

        private void finishUnchangedBlock() {
            if (mCurrentUnchangedBuilder != null) {
                mBlocks.add(mCurrentUnchangedBuilder.build());
                mCurrentUnchangedBuilder = null;
            }
        }

        @SuppressWarnings("null")
        @Nonnull
        private Delta.Builder prepareDeltaBuilder() {
            // Finish the unchanged block if we were building one
            finishUnchangedBlock();

            if (mCurrentDeltaBuilder == null) {
                mCurrentDeltaBuilder = new Delta.Builder(getCurrentLeftLine(), getCurrentRightLine());
            }
            return mCurrentDeltaBuilder;
        }

        public void appendLineLeftRemoved(String line) throws DiffParseException {
            prepareDeltaBuilder().appendRemovedLine(line);
            mLeftLinesProcessed++;
            assertSize();
        }

        public void appendLineRightAdded(String line) throws DiffParseException {
            prepareDeltaBuilder().appendAddedLine(line);
            mRightLinesProcessed++;
            assertSize();
        }

        public Chunk build() throws DiffParseException {
            Assert.isFalse(mCurrentDeltaBuilder != null && mCurrentUnchangedBuilder != null);
            finishDeltaBlock();
            finishUnchangedBlock();

            if (!isComplete()) {
                throw new DiffParseException(
                        "Chunk isn't complete. Expected " + mLeftLength
                                + " changed left lines but got "
                                + mLeftLinesProcessed + ". Expected "
                                + mRightLength
                                + " changed right lines but got "
                                + mRightLinesProcessed);
            }
            return new Chunk(mLeftStartLine, mBlocks);
        }
    }

    public interface Block extends IForwardApplicable {

        @Immutable
        public static class Unchanged implements Block {

            private final int mLeftStartLine;
            private final int mRightStartLine;
            private final List<String> mLines;

            public Unchanged(int leftStartLine, int rightStartLine, List<String> lines) {
                mLeftStartLine = leftStartLine;
                mRightStartLine = rightStartLine;
                mLines = lines;
            }

            @Override
            public List<SideBySideLine> applyForward(ILineReader leftFile) {
                int leftLine = mLeftStartLine;
                int rightLine = mRightStartLine;
                List<SideBySideLine> output = new ArrayList<SideBySideLine>();
                for (String line : mLines) {
                    String consumedLine = leftFile.consumeLine();
                    if (!line.equals(consumedLine)) {
                        throw new IllegalStateException("Expected:\n" + line + "\nBut got:\n" + consumedLine);
                    }
                    output.add(new SideBySideLine(leftLine, line, rightLine, line));
                    leftLine++;
                    rightLine++;
                }
                return output;
            }

            public static class Builder {

                private final int mLeftStartLine;
                private final int mRightStartLine;
                private final List<String> mLines = new ArrayList<String>();

                public Builder(int leftStartLine, int rightStartLine) {
                    mLeftStartLine = leftStartLine;
                    mRightStartLine = rightStartLine;
                }

                public void appendLine(String line) {
                    mLines.add(line);
                }

                public Unchanged build() {
                    return new Unchanged(mLeftStartLine, mRightStartLine, mLines);
                }
            }
        }

        @Immutable
        public static class Delta implements Block {

            private final int mLeftStartLine;
            private final int mRightStartLine;
            private final List<String> mRemovedLines;
            private final List<String> mAddedLines;

            public Delta(int leftStartLine, int rightStartLine,
                    List<String> removedLines, List<String> addedLines) {
                mLeftStartLine = leftStartLine;
                mRightStartLine = rightStartLine;
                mRemovedLines = removedLines;
                mAddedLines = addedLines;
            }

            public static class Builder {

                private final int mLeftStartLine;
                private final int mRightStartLine;
                private final List<String> mRemovedLines = new ArrayList<String>();
                private final List<String> mAddedLines = new ArrayList<String>();

                public Builder(int leftStartLine, int rightStartLine) {
                    mLeftStartLine = leftStartLine;
                    mRightStartLine = rightStartLine;
                }

                public void appendRemovedLine(String removedLine) {
                    mRemovedLines.add(removedLine);
                }

                public void appendAddedLine(String addedLine) {
                    mAddedLines.add(addedLine);
                }

                public Delta build() {
                    return new Delta(mLeftStartLine, mRightStartLine,
                            mRemovedLines, mAddedLines);
                }
            }

            @Override
            public List<SideBySideLine> applyForward(ILineReader leftFile) {
                int outLines = Math.max(mRemovedLines.size(), mAddedLines.size());

                int leftLine = mLeftStartLine;
                int rightLine = mRightStartLine;
                List<SideBySideLine> output = new ArrayList<SideBySideLine>();

                for (int i = 0; i < outLines; i++) {
                    String expectedRemovedLine = null;
                    String addedLine = null;
                    if (i < mRemovedLines.size()) {
                        expectedRemovedLine = mRemovedLines.get(i);
                        String consumedLine = leftFile.consumeLine();
                        if (!expectedRemovedLine.equals(consumedLine)) {
                            throw new IllegalStateException("Expected:\n"
                                    + expectedRemovedLine + "\nBut got:\n"
                                    + consumedLine);
                        }
                    }
                    if (i < mAddedLines.size()) {
                        addedLine = mAddedLines.get(i);
                    }
                    SideBySideLine line = new SideBySideLine(leftLine, expectedRemovedLine, rightLine, addedLine);
                    output.add(line);

                    if (line.getLeftLine() != null) {
                        leftLine++;
                    }
                    if (line.getRightLine() != null) {
                        rightLine++;
                    }
                }
                return output;
            }

        }
    }
}

package com.scottbezek.superdiff.unified;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.scottbezek.superdiff.unified.Chunk.Block.Delta;
import com.scottbezek.superdiff.unified.Chunk.Block.Unchanged;
import com.scottbezek.util.Assert;

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

        private void assertSize() {
            if (mLeftLinesProcessed > mLeftLength) {
                throw new IllegalStateException("More left lines than expected!");
            }
            if (mRightLinesProcessed > mRightLength) {
                throw new IllegalStateException("More right lines than expected!");
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

        @Nonnull
        private Unchanged.Builder prepareUnchangedBuilder() {
            // Finish the delta block if we were building one
            finishDeltaBlock();

            if (mCurrentUnchangedBuilder == null) {
                mCurrentUnchangedBuilder = new Unchanged.Builder(getCurrentLeftLine(), getCurrentRightLine());
            }
            return mCurrentUnchangedBuilder;
        }

        public void appendLineUnchanged(String line) {
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

        @Nonnull
        private Delta.Builder prepareDeltaBuilder() {
            // Finish the unchanged block if we were building one
            finishUnchangedBlock();

            if (mCurrentDeltaBuilder == null) {
                mCurrentDeltaBuilder = new Delta.Builder(getCurrentLeftLine(), getCurrentRightLine());
            }
            return mCurrentDeltaBuilder;
        }

        public void appendLineLeftRemoved(String line) {
            prepareDeltaBuilder().appendRemovedLine(line);
            mLeftLinesProcessed++;
            assertSize();
        }

        public void appendLineRightAdded(String line) {
            prepareDeltaBuilder().appendAddedLine(line);
            mRightLinesProcessed++;
            assertSize();
        }

        public Chunk build() {
            Assert.isFalse(mCurrentDeltaBuilder != null && mCurrentUnchangedBuilder != null);
            finishDeltaBlock();
            finishUnchangedBlock();

            if (!isComplete()) {
                throw new IllegalStateException(
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

                    if (line.hasLeft()) {
                        leftLine++;
                    }
                    if (line.hasRight()) {
                        rightLine++;
                    }
                }
                return output;
            }

        }
    }

    public interface LineDelta {

        /**
         * Apply the delta forward (i.e. modify the older base version to generate the new version).
         *
         * @param leftFile
         * @param rightFile
         * @return number of left lines consumed
         */
        int applyForward(ILineReader leftFile, ILineWriter rightFile);

        public static class Unchanged implements LineDelta {

            private final String mLine;

            public Unchanged(String line) {
                mLine = line;
            }

            @Override
            public int applyForward(ILineReader leftFile, ILineWriter rightFile) {
                String consumedLine = leftFile.consumeLine();
                if (!mLine.equals(consumedLine)) {
                    throw new IllegalStateException("Expected:\n" + mLine + "\nBut got:\n" + consumedLine);
                }
                rightFile.outputLine(mLine);
                return 1;
            }
        }

        public static class Added implements LineDelta {

            private final String mLine;

            public Added(String line) {
                mLine = line;
            }

            @Override
            public int applyForward(ILineReader leftFile, ILineWriter rightFile) {
                rightFile.outputLine(mLine);
                return 0;
            }
        }

        public static class Removed implements LineDelta {

            private final String mLine;

            public Removed(String line) {
                mLine = line;
            }

            @Override
            public int applyForward(ILineReader leftFile, ILineWriter rightFile) {
                String consumedLine = leftFile.consumeLine();
                if (!mLine.equals(consumedLine)) {
                    throw new IllegalStateException("Expected:\n" + mLine + "\nBut got:\n" + consumedLine);
                }
                return 1;
            }
        }
    }


//
//
//    public static class TransformationPiece implements ChunkPiece {
//
//        private final int mLeftStartLine;
//        private final List<String> mLeftRemovedLines;
//        private final List<String> mRightAddedLines;
//
//        public TransformationPiece(int leftStartLine, List<String> leftRemovedLines, List<String> rightAddedLines) {
//            mLeftStartLine = leftStartLine;
//            mLeftRemovedLines = new ArrayList<String>(leftRemovedLines);
//            mRightAddedLines = new ArrayList<String>(rightAddedLines);
//        }
//
//        @Override
//        public void applyForward(LineReader leftFile, LineWriter rightFile) {
//            for (String expectedLeftRemoval : mLeftRemovedLines) {
//                String consumedLine = leftFile.consumeLine();
//                if (!expectedLeftRemoval.equals(consumedLine)) {
//                    throw new IllegalStateException("Expected:\n" + expectedLeftRemoval + "\nBut got:\n" + consumedLine);
//                }
//            }
//            for (String addedLine : mRightAddedLines) {
//                rightFile.outputLine(addedLine);
//            }
//        }
//
//        @Override
//        public int getLeftStartLine() {
//            return mLeftStartLine;
//        }
//
//    }
//
//    // TODO: might be better to make UnchangedPiece handle multiple-line unchanged sections?
//    public static class UnchangedPiece implements ChunkPiece {
//
//        private final int mLeftStartLine;
//        private final String mUnchangedLine;
//
//        public UnchangedPiece(int leftStartLine, String unchangedLine) {
//            mLeftStartLine = leftStartLine;
//            mUnchangedLine = unchangedLine;
//        }
//
//        @Override
//        public void applyForward(LineReader leftFile, LineWriter rightFile) {
//            // for (String line : mUnchangedLines) {
//            String consumedLine = leftFile.consumeLine();
//            if (!mUnchangedLine.equals(consumedLine)) {
//                throw new IllegalStateException("Expected:\n" + mUnchangedLine
//                        + "\nBut got:\n" + consumedLine);
//            }
//            rightFile.outputLine(mUnchangedLine);
//            // }
//        }
//
//        @Override
//        public int getLeftStartLine() {
//            return mLeftStartLine;
//        }
//    }

}

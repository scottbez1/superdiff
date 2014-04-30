package com.scottbezek.difflib.compute;

import com.scottbezek.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Diff algorithm that computes an edit string of minimal Levenshtein cost which transforms one
 * sequence to another. Cost of insert/delete/replace operations can be adjusted.
 */
public class LevenshteinDiff<Element> {

    private final List<Element> mFirst;

    private final List<Element> mSecond;

    private float mInsertCost = 1f;

    private float mReplaceCost = 1f;

    private float mDeleteCost = 1f;

    private float[][] mCostTable = null;

    private Edit[][] mEditTypeTable = null;

    /**
     * Construct a LevenshteinDiff to compute the edit string (i.e. series of insertions,
     * deletions, replacements, and no-ops to transform one string into another).
     * <p/>
     * Cost parameters of operations may optionally be adjusted via {@link #setInsertCost(float)},
     * {@link #setDeleteCost(float)}, {@link #setReplaceCost(float)} before calling {@link
     * #compute()}.
     */
    public LevenshteinDiff(List<Element> first, List<Element> second) {
        mFirst = first;
        mSecond = second;
    }

    /**
     * Set the cost of an insertion. Should be called before {@link #compute()}.
     * @return <code>this</code>, for chaining.
     */
    public LevenshteinDiff<Element> setInsertCost(float cost) {
        mInsertCost = cost;
        return this;
    }

    /**
     * Set the cost of a replacement. Should be called before {@link #compute()}.
     * @return <code>this</code>, for chaining.
     */
    public LevenshteinDiff<Element> setReplaceCost(float cost) {
        mReplaceCost = cost;
        return this;
    }

    /**
     * Set the cost of a deletion. Should be called before {@link #compute()}.
     * @return <code>this</code>, for chaining.
     */
    public LevenshteinDiff<Element> setDeleteCost(float cost) {
        mDeleteCost = cost;
        return this;
    }

    /**
     * Computes the diff in O(NM) time and space.
     * @return <code>this</code>, for chaining.
     */
    public LevenshteinDiff<Element> compute() {
        mCostTable = new float[mFirst.size() + 1][];
        mEditTypeTable = new Edit[mFirst.size() + 1][];

        /*
         * Initialize the upper row and left column, which correspond to initial deletions/insertions.
         */
        for (int i = 0; i <= mFirst.size(); i++) {
            mCostTable[i] = new float[mSecond.size() + 1];
            mEditTypeTable[i] = new Edit[mSecond.size() + 1];

            mCostTable[i][0] = i * mDeleteCost;
            mEditTypeTable[i][0] = Edit.DELETE;
        }
        for (int j = 0; j <= mSecond.size(); j++) {
            mCostTable[0][j] = j * mInsertCost;
            mEditTypeTable[0][j] = Edit.INSERT;
        }

        for (int i = 1; i <= mFirst.size(); i++) {
            for (int j = 1; j <= mSecond.size(); j++) {
                final float deleteCost = mCostTable[i - 1][j] + mDeleteCost;
                final float insertCost = mCostTable[i][j - 1] + mInsertCost;
                final float replaceCost;
                final Edit replaceType;
                if (mFirst.get(i - 1).equals(mSecond.get(j - 1))) {
                    replaceCost = mCostTable[i - 1][j - 1] + 0f;
                    replaceType = Edit.SAME;
                } else {
                    replaceCost = mCostTable[i - 1][j - 1] + mReplaceCost;
                    replaceType = Edit.REPLACE;
                }

                final float opCost;
                final Edit opType;

                // Prefer replace if costs are tied
                if (replaceCost <= deleteCost && replaceCost <= insertCost) {
                    opCost = replaceCost;
                    opType = replaceType;
                } else if (insertCost <= deleteCost) {
                    opCost = insertCost;
                    opType = Edit.INSERT;
                } else {
                    opCost = deleteCost;
                    opType = Edit.DELETE;
                }

                mCostTable[i][j] = opCost;
                mEditTypeTable[i][j] = opType;
            }
        }
        return this;
    }

    /**
     * Retrieve the edit string for this diff. The diff must have already been computed by calling
     * {@link #compute()}.
     */
    public List<Edit> getEditString() {
        if (mCostTable == null) {
            throw new IllegalStateException("Must compute the diff first");
        }

        List<Edit> output = new ArrayList<Edit>();
        int i = mFirst.size();
        int j = mSecond.size();
        while (i > 0 || j > 0) {
            Edit editType = mEditTypeTable[i][j];
            output.add(editType);
            switch (editType) {
                case INSERT:
                    j--;
                    break;
                case DELETE:
                    i--;
                    break;
                case REPLACE: // intentional fall-through
                case SAME:
                    i--;
                    j--;
                    break;
            }
        }

        // Reverse the edit string since it was built by back-tracking through the table
        Collections.reverse(output);
        return output;
    }

    /**
     * Returns a printable representation of the computed cost table, for debugging.
     */
    String getDebugCostTable() {
        if (mCostTable == null) {
            throw new IllegalStateException("Must compute the diff first");
        }
        int colWidth = 4;
        StringBuilder sb = new StringBuilder();
        for (int j = -1; j <= mSecond.size(); j++) {
            for (int i = -1; i <= mFirst.size(); i++) {
                if (i < 1 && j < 1) {
                    // empty item
                    sb.append(String.format("%1$" + colWidth + "s", ""));
                } else if (i == -1) {
                    String rowHeader = mSecond.get(j-1).toString();
                    sb.append(String.format("%1$-" + colWidth + "s", rowHeader));
                } else if (j == -1) {
                    String columnHeader = mFirst.get(i-1).toString();
                    sb.append(String.format("%1$-" + colWidth + "s", columnHeader));
                } else {
                    sb.append(String.format("%1$." + colWidth + "f", mCostTable[i][j]).substring(0, colWidth));
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Takes 2 lists of elements and returns 2 new lists with the shared prefix and/or
     * suffix removed. The input lists are not modified.
     */
    static <Element> ElementPair<Element> getTrimmedElements(List<Element> first,
            List<Element> second) {
        final ListIterator<Element> firstIterator = first.listIterator();
        final ListIterator<Element> secondIterator = second.listIterator();

        // Move both iterators forward until the elements don't match or the end
        // of either string is reached.
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            if (!firstIterator.next().equals(secondIterator.next())) {
                firstIterator.previous();
                secondIterator.previous();
                break;
            }
        }

        final ListIterator<Element> firstBackwardIterator = first.listIterator(first.size());
        final ListIterator<Element> secondBackwardIterator = second.listIterator(second.size());
        while (firstBackwardIterator.hasPrevious()
                && secondBackwardIterator.hasPrevious()
                && firstBackwardIterator.previousIndex() >= firstIterator.nextIndex()
                && secondBackwardIterator.previousIndex() >= secondIterator.nextIndex()) {
            if (!firstBackwardIterator.previous().equals(secondBackwardIterator.previous())) {
                firstBackwardIterator.next();
                secondBackwardIterator.next();
                break;
            }
        }

        final List<Element> firstTrimmed = new ArrayList<Element>();
        while (firstIterator.hasNext()
                && firstIterator.nextIndex() <= firstBackwardIterator.previousIndex()) {
            firstTrimmed.add(firstIterator.next());
        }

        final List<Element> secondTrimmed = new ArrayList<Element>();
        while (secondIterator.hasNext()
                && secondIterator.nextIndex() <= secondBackwardIterator.previousIndex()) {
            secondTrimmed.add(secondIterator.next());
        }

        // Same number of elements should have been trimmed from each
        Assert.isTrue(
                (first.size() - firstTrimmed.size()) == (second.size() - secondTrimmed.size()));
        return new ElementPair<Element>(firstTrimmed, secondTrimmed);
    }

    static class ElementPair<Element> {

        private final List<Element> mFirst;

        private final List<Element> mSecond;

        public ElementPair(List<Element> first, List<Element> second) {
            mFirst = first;
            mSecond = second;
        }

        public List<Element> getFirst() {
            return mFirst;
        }

        public List<Element> getSecond() {
            return mSecond;
        }
    }

}

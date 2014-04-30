package com.scottbezek.difflib.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                    replaceType = Edit.UNCHANGED;
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
                case REPLACE:
                    i--;
                    j--;
                    break;
                case UNCHANGED:
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
}

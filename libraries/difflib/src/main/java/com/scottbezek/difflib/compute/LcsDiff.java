package com.scottbezek.difflib.compute;

import com.scottbezek.difflib.UnicodeUtil;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class LcsDiff {

    private final Locale mLocale;

    public LcsDiff() {
        // TODO(sbezek): choose something better than default?
        mLocale = Locale.getDefault();
    }

    public DiffCatalog computeDiff(String leftString, String rightString) {
        final List<String> left = UnicodeUtil.splitNaturalCharacters(leftString, mLocale);
        final List<String> right = UnicodeUtil.splitNaturalCharacters(rightString, mLocale);

        int[][] lcsLengthTable = computeLcsLengthTable(left, right);

        DiffCatalog output = new DiffCatalog();
        buildDiff(output, lcsLengthTable, left, right, left.size(), right.size(), leftString.length(), rightString.length());
        return output;
    }

    private static int[][] computeLcsLengthTable(List<String> left, List<String> right) {
        final int [][] lcsLength = new int[left.size() + 1][];
        for (int i = 0; i < left.size() + 1; i++) {
            lcsLength[i] = new int[right.size() + 1];
        }

        for (int i = 1; i < left.size() + 1; i++) {
            for (int j = 1; j < right.size() + 1; j++) {
                if (left.get(i-1).equals(right.get(j-1))) {
                    lcsLength[i][j] = lcsLength[i-1][j-1] + 1;
                } else {
                    lcsLength[i][j] = Math.max(
                            lcsLength[i][j-1],
                            lcsLength[i-1][j]);
                }
            }
        }
        return lcsLength;
    }

    private static void buildDiff(DiffCatalog diffOutput, int[][] lcsLengthTable, List<String> left, List<String> right, int i, int j, int absoluteCharsI, int absoluteCharsJ) {
        if (i > 0 && j > 0 && left.get(i-1).equals(right.get(j-1))) {
            String item = left.get(i-1);
            buildDiff(diffOutput, lcsLengthTable, left, right, i-1, j-1, absoluteCharsI - item.length(), absoluteCharsJ - item.length());
        } else if (j > 0 && (i == 0 || lcsLengthTable[i][j-1] >= lcsLengthTable[i-1][j])) {
            String rightItem = right.get(j-1);
            buildDiff(diffOutput, lcsLengthTable, left, right, i, j-1, absoluteCharsI, absoluteCharsJ - rightItem.length());
            diffOutput.addRightUniqueRegion(new Region(absoluteCharsJ - rightItem.length(), rightItem.length()));
        } else if (i > 0 && (j == 0 || lcsLengthTable[i][j-1] < lcsLengthTable[i-1][j])) {
            String leftItem = left.get(i-1);
            buildDiff(diffOutput, lcsLengthTable, left, right, i-1, j, absoluteCharsI - leftItem.length(), absoluteCharsJ);
            diffOutput.addLeftUniqueRegion(new Region(absoluteCharsI - leftItem.length(), leftItem.length()));
        }
    }
}

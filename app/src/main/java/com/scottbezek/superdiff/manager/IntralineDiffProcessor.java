package com.scottbezek.superdiff.manager;

import com.scottbezek.difflib.UnicodeUtil;
import com.scottbezek.difflib.compute.DiffComputeUtil;
import com.scottbezek.difflib.compute.Edit;
import com.scottbezek.difflib.compute.LevenshteinDiff;
import com.scottbezek.difflib.unified.SideBySideLine;
import com.scottbezek.util.Assert;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Computes intraline diffs, and attaches the appropriate spans to the CharSequences in a {@link
 * com.scottbezek.difflib.unified.SideBySideLine}.
 */
public class IntralineDiffProcessor {

    private final Locale mLocale;
    private final int mRemovedCharactersBackgroundColor;
    private final int mAddedCharactersBackgroundColor;

    public IntralineDiffProcessor(Locale locale, int removedCharactersBackgroundColor,
            int addedCharactersBackgroundColor) {
        mLocale = locale;
        mRemovedCharactersBackgroundColor = removedCharactersBackgroundColor;
        mAddedCharactersBackgroundColor = addedCharactersBackgroundColor;
    }

    public SideBySideLine computeIntralineDiff(SideBySideLine line) {
        CharSequence leftLine = line.getLeftLine();
        CharSequence rightLine = line.getRightLine();
        if (leftLine != null && rightLine != null && !leftLine.equals(rightLine)) {
            final Spannable leftSpan = new SpannableString(leftLine);
            final Spannable rightSpan = new SpannableString(rightLine);

            final List<String> leftElements = UnicodeUtil.splitNaturalCharacters(leftLine.toString(), mLocale);
            final List<String> rightElements = UnicodeUtil.splitNaturalCharacters(rightLine.toString(), mLocale);
            final List<Edit> editString = new LevenshteinDiff<String>(leftElements, rightElements)
                    .setReplaceCost(2f)
                    .compute()
                    .getEditString();
            DiffComputeUtil.removeSmallUnchangedRegions(editString);

            final Iterator<String> leftIterator = leftElements.iterator();
            final Iterator<String> rightIterator = rightElements.iterator();
            int leftCharIndex = 0;
            int rightCharIndex = 0;
            for (Edit edit : editString) {
                if (edit == Edit.DELETE) {
                    final String leftElement = leftIterator.next();
                    leftSpan.setSpan(new BackgroundColorSpan(mRemovedCharactersBackgroundColor),
                            leftCharIndex, leftCharIndex + leftElement.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    leftCharIndex += leftElement.length();
                } else if (edit == Edit.INSERT) {
                    final String rightElement = rightIterator.next();
                    rightSpan.setSpan(new BackgroundColorSpan(mAddedCharactersBackgroundColor),
                            rightCharIndex, rightCharIndex + rightElement.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    rightCharIndex += rightElement.length();
                } else if (edit == Edit.REPLACE) {
                    final String leftElement = leftIterator.next();
                    leftSpan.setSpan(new BackgroundColorSpan(mRemovedCharactersBackgroundColor),
                            leftCharIndex, leftCharIndex + leftElement.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    leftCharIndex += leftElement.length();

                    final String rightElement = rightIterator.next();
                    rightSpan.setSpan(new BackgroundColorSpan(mAddedCharactersBackgroundColor),
                            rightCharIndex, rightCharIndex + rightElement.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    rightCharIndex += rightElement.length();
                } else if (edit == Edit.UNCHANGED) {
                    final String leftElement = leftIterator.next();
                    leftCharIndex += leftElement.length();
                    final String rightElement = rightIterator.next();
                    rightCharIndex += rightElement.length();
                } else {
                    throw Assert.fail("Unknown edit type: " + edit);
                }
            }
            return new SideBySideLine(line.getLeftLineNumber(), leftSpan, line.getRightLineNumber(), rightSpan);
        } else {
            return line;
        }
    }
}

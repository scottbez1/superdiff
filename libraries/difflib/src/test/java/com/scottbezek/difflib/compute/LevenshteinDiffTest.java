package com.scottbezek.difflib.compute;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LevenshteinDiffTest {

    @Test
    public void testSimpleDiff() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                splitChars("kitten"),
                splitChars("sitting"))
                .compute();

        List<Edit> expected = Arrays.asList(
                Edit.REPLACE,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.REPLACE,
                Edit.UNCHANGED,
                Edit.INSERT
                );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testReplaceMatchedPrefersReplace() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                splitChars("kitten"),
                splitChars("sitting"))
                .setDeleteCost(1f)
                .setInsertCost(1f)
                .setReplaceCost(2f) // same as a delete+insert
                .compute();

        List<Edit> expected = Arrays.asList(
                Edit.REPLACE,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.REPLACE,
                Edit.UNCHANGED,
                Edit.INSERT
        );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testExpensiveReplace() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                splitChars("kitten"),
                splitChars("sitting"))
                .setDeleteCost(1f)
                .setInsertCost(1f)
                .setReplaceCost(2.001f) // replace is slightly more expensive than a delete + insert
                .compute();

        List<Edit> expected = Arrays.asList(
                Edit.DELETE,
                Edit.INSERT,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.DELETE,
                Edit.INSERT,
                Edit.UNCHANGED,
                Edit.INSERT
        );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testBothEmpty() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                Collections.<Character>emptyList(),
                Collections.<Character>emptyList())
                .compute();

        List<Edit> expected = Collections.emptyList();

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testFromEmpty() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                Collections.<Character>emptyList(),
                splitChars("foobar"))
                .compute();

        List<Edit> expected = Arrays.asList(
                Edit.INSERT,
                Edit.INSERT,
                Edit.INSERT,
                Edit.INSERT,
                Edit.INSERT,
                Edit.INSERT
        );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testToEmpty() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                splitChars("foobar"),
                Collections.<Character>emptyList()
                )
                .compute();

        List<Edit> expected = Arrays.asList(
                Edit.DELETE,
                Edit.DELETE,
                Edit.DELETE,
                Edit.DELETE,
                Edit.DELETE,
                Edit.DELETE
        );

        assertEquals(expected, c.getEditString());
    }

    private static List<Character> splitChars(String s) {
        List<Character> chars = new ArrayList<Character>();
        for (char c : s.toCharArray()) {
            chars.add(c);
        }
        return chars;
    }
}

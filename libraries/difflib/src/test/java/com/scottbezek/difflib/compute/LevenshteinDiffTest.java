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

        List<LevenshteinDiff.EditType> expected = Arrays.asList(
                LevenshteinDiff.EditType.REPLACE,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.REPLACE,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.INSERT
                );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testReplaceMatchedPrefersReplace() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                splitChars("kitten"),
                splitChars("sitting"))
                .setReplaceCost(2f) // same as a delete+insert
                .compute();

        List<LevenshteinDiff.EditType> expected = Arrays.asList(
                LevenshteinDiff.EditType.REPLACE,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.REPLACE,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.INSERT
        );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testExpensiveReplace() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                splitChars("kitten"),
                splitChars("sitting"))
                .setReplaceCost(2.001f) // replace is slightly more expensive than a delete + insert
                .compute();

        List<LevenshteinDiff.EditType> expected = Arrays.asList(
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.SAME,
                LevenshteinDiff.EditType.INSERT
        );

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testBothEmpty() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                Collections.<Character>emptyList(),
                Collections.<Character>emptyList())
                .compute();

        List<LevenshteinDiff.EditType> expected = Collections.emptyList();

        assertEquals(expected, c.getEditString());
    }

    @Test
    public void testFromEmpty() {
        LevenshteinDiff<Character> c = new LevenshteinDiff<Character>(
                Collections.<Character>emptyList(),
                splitChars("foobar"))
                .compute();

        List<LevenshteinDiff.EditType> expected = Arrays.asList(
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.INSERT,
                LevenshteinDiff.EditType.INSERT
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

        List<LevenshteinDiff.EditType> expected = Arrays.asList(
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.DELETE,
                LevenshteinDiff.EditType.DELETE
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

    @Test
    public void testGetTrimmedElementsSimple() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("foo", "bar", "some", "other", "words", "sandwich");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("ice", "cream"), result.getFirst());
        assertEquals(Arrays.asList("some", "other", "words"), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsEmpty() {
        List<String> first = Collections.emptyList();
        List<String> second = Collections.emptyList();
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Collections.emptyList(), result.getFirst());
        assertEquals(Collections.emptyList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsFirstOnly() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList();
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("foo", "bar", "ice", "cream", "sandwich"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsSecondOnly() {
        List<String> first = Arrays.asList();
        List<String> second = Arrays.asList("foo", "bar", "some", "other", "words", "sandwich");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList(), result.getFirst());
        assertEquals(Arrays.asList("foo", "bar", "some", "other", "words", "sandwich"), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsBothSingle() {
        List<String> first = Arrays.asList("foo");
        List<String> second = Arrays.asList("foo");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList(), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsPrefix() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("foo", "bar");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("ice", "cream", "sandwich"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsSuffix() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("cream", "sandwich");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("foo", "bar", "ice"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsDisjoint() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("oops", "woah");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("foo", "bar", "ice", "cream", "sandwich"), result.getFirst());
        assertEquals(Arrays.asList("oops", "woah"), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsPrefersPrefix() {
        List<String> first = Arrays.asList("foo", "bar", "foo", "bar", "testing", "foo", "bar");
        List<String> second = Arrays.asList("foo", "bar");
        LevenshteinDiff.ElementPair result = LevenshteinDiff.getTrimmedElements(first, second);

        // "foo" "bar" could be removed as the prefix or suffix of the first list - the prefix should be preferred
        assertEquals(Arrays.asList("foo", "bar", "testing", "foo", "bar"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }
}

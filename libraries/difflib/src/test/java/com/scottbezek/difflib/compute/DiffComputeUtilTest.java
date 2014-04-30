package com.scottbezek.difflib.compute;

import com.scottbezek.difflib.compute.DiffComputeUtil.ElementListPair;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DiffComputeUtilTest {

    @Test
    public void testRemoveSmallUnchangedRegions_Empty() throws Exception {
        List<Edit> test = Arrays.asList();
        List<Edit> expected = Arrays.asList();

        DiffComputeUtil.removeSmallUnchangedRegions(test);
        assertEquals(expected, test);
    }

    @Test
    public void testRemoveSmallUnchangedRegions_IgnoreUnchangedPrefix() throws Exception {
        List<Edit> test = Arrays.asList(Edit.UNCHANGED, Edit.DELETE);
        List<Edit> expected = Arrays.asList(Edit.UNCHANGED, Edit.DELETE);

        DiffComputeUtil.removeSmallUnchangedRegions(test);
        assertEquals(expected, test);
    }

    @Test
    public void testRemoveSmallUnchangedRegions_Basic() throws Exception {
        List<Edit> test = Arrays.asList(
                Edit.INSERT,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.REPLACE);
        List<Edit> expected = Arrays.asList(
                Edit.INSERT,
                Edit.REPLACE,
                Edit.REPLACE,
                Edit.REPLACE,
                Edit.REPLACE);

        DiffComputeUtil.removeSmallUnchangedRegions(test);
        assertEquals(expected, test);
    }

    @Test
    public void testRemoveSmallUnchangedRegions_IgnoreUnchangedSuffix() throws Exception {
        List<Edit> test = Arrays.asList(Edit.INSERT, Edit.UNCHANGED);
        List<Edit> expected = Arrays.asList(Edit.INSERT, Edit.UNCHANGED);

        DiffComputeUtil.removeSmallUnchangedRegions(test);
        assertEquals(expected, test);
    }

    @Test
    public void testRemoveSmallUnchangedRegions_IgnoreLongUnchanged() throws Exception {
        List<Edit> test = Arrays.asList(
                Edit.INSERT,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.REPLACE);
        List<Edit> expected = Arrays.asList(
                Edit.INSERT,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.REPLACE);

        DiffComputeUtil.removeSmallUnchangedRegions(test);
        assertEquals(expected, test);
    }

    @Test
    public void testRemoveSmallUnchangedRegions_MultipleRegions() throws Exception {
        List<Edit> test = Arrays.asList(
                Edit.INSERT,
                Edit.UNCHANGED,
                Edit.DELETE,
                Edit.UNCHANGED,
                Edit.UNCHANGED,
                Edit.REPLACE);
        List<Edit> expected = Arrays.asList(
                Edit.INSERT,
                Edit.REPLACE,
                Edit.DELETE,
                Edit.REPLACE,
                Edit.REPLACE,
                Edit.REPLACE);

        DiffComputeUtil.removeSmallUnchangedRegions(test);
        assertEquals(expected, test);
    }

    @Test
    public void testGetTrimmedElementsSimple() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("foo", "bar", "some", "other", "words", "sandwich");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("ice", "cream"), result.getFirst());
        assertEquals(Arrays.asList("some", "other", "words"), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsEmpty() {
        List<String> first = Collections.emptyList();
        List<String> second = Collections.emptyList();
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Collections.emptyList(), result.getFirst());
        assertEquals(Collections.emptyList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsFirstOnly() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList();
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("foo", "bar", "ice", "cream", "sandwich"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsSecondOnly() {
        List<String> first = Arrays.asList();
        List<String> second = Arrays.asList("foo", "bar", "some", "other", "words", "sandwich");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList(), result.getFirst());
        assertEquals(Arrays.asList("foo", "bar", "some", "other", "words", "sandwich"), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsBothSingle() {
        List<String> first = Arrays.asList("foo");
        List<String> second = Arrays.asList("foo");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList(), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsPrefix() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("foo", "bar");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("ice", "cream", "sandwich"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsSuffix() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("cream", "sandwich");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("foo", "bar", "ice"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsDisjoint() {
        List<String> first = Arrays.asList("foo", "bar", "ice", "cream", "sandwich");
        List<String> second = Arrays.asList("oops", "woah");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);
        assertEquals(Arrays.asList("foo", "bar", "ice", "cream", "sandwich"), result.getFirst());
        assertEquals(Arrays.asList("oops", "woah"), result.getSecond());
    }

    @Test
    public void testGetTrimmedElementsPrefersPrefix() {
        List<String> first = Arrays.asList("foo", "bar", "foo", "bar", "testing", "foo", "bar");
        List<String> second = Arrays.asList("foo", "bar");
        ElementListPair<String> result = DiffComputeUtil.getTrimmedElements(first, second);

        // "foo" "bar" could be removed as the prefix or suffix of the first list - the prefix should be preferred
        assertEquals(Arrays.asList("foo", "bar", "testing", "foo", "bar"), result.getFirst());
        assertEquals(Arrays.asList(), result.getSecond());
    }
}

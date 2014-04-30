package com.scottbezek.difflib;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class UnicodeUtilTest {

    @Test
    public void testSplitNaturalCharactersEmpty() throws Exception {
        List<String> expected = Collections.emptyList();
        assertEquals(expected, UnicodeUtil.splitNaturalCharacters("", Locale.US));
    }

    @Test
    public void testSplitNaturalCharactersSingle() throws Exception {
        List<String> expected = Arrays.asList("a");
        assertEquals(expected, UnicodeUtil.splitNaturalCharacters("a", Locale.US));
    }

    @Test
    public void testSplitNaturalCharactersSimple() throws Exception {
        List<String> expected = Arrays.asList("a", "b", "c");
        assertEquals(expected, UnicodeUtil.splitNaturalCharacters("abc", Locale.US));
    }

    @Test
    public void testSplitNaturalCharactersAccent() throws Exception {
        List<String> expected = Arrays.asList("a", "a\u0301", "c");
        assertEquals(expected, UnicodeUtil.splitNaturalCharacters("aa\u0301c", Locale.US));
    }
}

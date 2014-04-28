package com.scottbezek.difflib;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Various utilities for dealing with unicode strings.
 */
public class UnicodeUtil {

    private UnicodeUtil() {}

    /**
     * Splits a String into natural characters - i.e. how a user might expect to
     * navigate a cursor through that text.
     * <p>
     * For instance, an accented character is a single "natural character," but
     * might be stored as a base character plus diacritical mark. A naive
     * byte/codepoint-based split would not act naturally in that case.
     * <p>
     * For a simple ASCII String, this will be no different than splitting the
     * String into a list of bytes.
     *
     * @param input
     *            The String to split.
     * @param locale
     *            The locale to use when determining character boundaries. The
     *            expected character boundaries may differ depending on the
     *            user's locale.
     *
     * @return A List of "natural characters" (where each natural character is
     *         stored as a non-null String of length >= 1).
     * @see BreakIterator
     * @see BreakIterator#getCharacterInstance()
     */
    public static List<String> splitNaturalCharacters(String input, Locale locale) {
        BreakIterator breakIterator = BreakIterator.getCharacterInstance(locale);
        List<String> result = new ArrayList<String>();

        breakIterator.setText(input);
        int start = breakIterator.first();
        for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
            result.add(input.substring(start, end));
        }
        return result;
    }
}

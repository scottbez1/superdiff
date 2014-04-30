package com.scottbezek.difflib.compute;

import com.scottbezek.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Various utilities for computing diffs.
 */
public class DiffComputeUtil {

    private DiffComputeUtil() {}

    /**
     * Converts short regions of {@link Edit#UNCHANGED} elements surrounded by edits into {@link
     * Edit#REPLACE}s. This can make diffs more readable, since short (1, 2, or 3 character)
     * unchanged regions are often false positives rather than useful info.
     * <p/>
     * For example, consider "View has requested"->"Special constant" which has an edit string of
     * "RRURRDURURRRDDUURRI"
     * <pre>
     *     RR-RRD-R-RRRDD--RR       RR-RR-R-RRR--RRI
     *     View has requested   |   Special constant
     * </pre>
     * The single- and two- character unchanged regions are meaningless and clutter the rendering
     * since these two strings are unrelated. After running the edit string through this function,
     * the new edit string is "RRRRRDRRRRRRDDRRRRI" which will generally be rendered as a single
     * contiguous highlighted region.
     *
     * @param editString Original edit string, to be modified in place.
     */
    public static void removeSmallUnchangedRegions(List<Edit> editString) {
        boolean seenChange = false;
        int contiguousUnchanged = 0;
        for (int i = 0; i < editString.size(); i++) {
            Edit edit = editString.get(i);
            if (edit == Edit.UNCHANGED) {
                if (seenChange) {
                    contiguousUnchanged++;
                }
            } else {
                if (0 < contiguousUnchanged && contiguousUnchanged < 4) {
                    for (int j = 0; j < contiguousUnchanged; j++) {
                        editString.set(i - j - 1, Edit.REPLACE);
                    }
                }
                seenChange = true;
                contiguousUnchanged = 0;
            }
        }
    }

    /**
     * Takes 2 lists of elements and returns 2 new lists with the shared prefix and/or
     * suffix removed. The input lists are not modified.
     */
    public static <Element> ElementListPair<Element> getTrimmedElements(List<Element> first,
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
        return new ElementListPair<Element>(firstTrimmed, secondTrimmed);
    }

    public static class ElementListPair<Element> {

        private final List<Element> mFirst;

        private final List<Element> mSecond;

        public ElementListPair(List<Element> first, List<Element> second) {
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

package com.scottbezek.difflib.compute;

/**
 * A single operation that is part of the series of operations that transforms one
 * sequence into another.
 */
public enum Edit {
    /** Element was inserted into the second sequence */
    INSERT,

    /** An element in the first sequence was replaced by an element in the second sequence */
    REPLACE,

    /** Element was deleted from the first sequence */
    DELETE,

    /** Element from the first sequence matches element from the second sequence */
    UNCHANGED,
}

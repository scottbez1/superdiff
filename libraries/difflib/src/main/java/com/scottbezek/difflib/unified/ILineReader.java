package com.scottbezek.difflib.unified;

public interface ILineReader {

    /**
     * Read a full line, or throw if one isn't available.
     *
     * TODO(sbezek): specify whether or not the line contains a trailing newline (probably yes?)
     * @return The line.
     * @throws IllegalStateException If a line isn't available.
     */
    String consumeLine();
}
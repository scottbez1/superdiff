package com.scottbezek.difflib.unified;

import java.util.List;

public interface IForwardApplicable {

    /**
     * Apply a diff chunk forward, using the {@link ILineReader} to read lines of
     * the base (left) file.
     *
     * @param baseFile
     * @return
     */
    List<SideBySideLine> applyForward(ILineReader baseFile);
}

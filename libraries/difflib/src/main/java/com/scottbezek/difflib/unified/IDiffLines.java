package com.scottbezek.difflib.unified;

import java.util.List;

public interface IDiffLines {

    /**
     * Get the plain lines of the diff. If verification against a base file is
     * desired, use {@link IForwardApplicable#applyForward(ILineReader)}
     * instead.
     */
    List<SideBySideLine> getLines();
}

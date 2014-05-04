package com.scottbezek.superdiff.manager;

import com.scottbezek.difflib.unified.Chunk;
import com.scottbezek.difflib.unified.Parser;
import com.scottbezek.difflib.unified.Parser.DiffParseException;
import com.scottbezek.difflib.unified.SideBySideLine;
import com.scottbezek.difflib.unified.SingleFileDiff;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedUnknown;
import com.scottbezek.superdiff.manager.DiffManager.DiffFailed;
import com.scottbezek.superdiff.manager.DiffManager.DiffLoadResult;
import com.scottbezek.superdiff.manager.DiffManager.DiffStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by scott on 5/2/14.
 */
public class DiffLoadTask implements Runnable {

    private final StateStream<DiffStatus> mOutput;
    private final InputStream mInput;

    public DiffLoadTask(InputStream input, StateStream output) {
        mInput = input;
        mOutput = output;
    }

    @Override
    public void run() {
        try {
            final Map<String, List<CollapsedOrLine>> diffByFilename =
                    getCollapsedDiffs(new Scanner(mInput));
            mOutput.update(new DiffLoadResult(diffByFilename));
        } catch (DiffParseException e) {
            mOutput.update(new DiffFailed(e));
        } finally {
            try {
                mInput.close();
            } catch (IOException e) {}
        }
    }


    private static Map<String, List<CollapsedOrLine>> getCollapsedDiffs(
            Scanner unifiedDiffFileContents) throws DiffParseException {
        final Map<String, List<CollapsedOrLine>> collapsedDiffByFilename =
                new HashMap<String, List<CollapsedOrLine>>();

        final List<SingleFileDiff> fileDiffs = new Parser(System.out)
                .parse(unifiedDiffFileContents);
        for (SingleFileDiff d : fileDiffs) {
            List<CollapsedOrLine> items = new ArrayList<CollapsedOrLine>();

            int curLeftLine = 1;
            for (Chunk chunk : d.getChunks()) {
                final int leftStartLine = chunk.getLeftStartLine();
                if (leftStartLine > curLeftLine) {
                    items.add(CollapsedOrLine.of(new CollapsedUnknown(leftStartLine - curLeftLine)));
                }
                for (SideBySideLine line : chunk.getLines()) {
                    items.add(CollapsedOrLine.of(line));
                    if (line.getLeftLine() != null) {
                        curLeftLine++;
                    }
                }
            }
            collapsedDiffByFilename.put(d.getDisplayFileName(), items);
        }

        return collapsedDiffByFilename;
    }
}

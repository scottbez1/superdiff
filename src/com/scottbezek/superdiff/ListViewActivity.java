package com.scottbezek.superdiff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnLayoutChangeListener;

import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.Collapsed;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;
import com.scottbezek.superdiff.list.HorizontalScrollObservingListView;
import com.scottbezek.superdiff.list.SideBySideLineView.ItemWidths;
import com.scottbezek.superdiff.unified.Chunk;
import com.scottbezek.superdiff.unified.ILineReader;
import com.scottbezek.superdiff.unified.Parser;
import com.scottbezek.superdiff.unified.Parser.DiffParseException;
import com.scottbezek.superdiff.unified.SideBySideLine;
import com.scottbezek.superdiff.unified.SingleFileDiff;
import com.scottbezek.util.Assert;
import com.scottbezek.util.StopWatch;

public class ListViewActivity extends Activity {

    private static final String TAG = ListViewActivity.class.getName();

    private HorizontalScrollObservingListView mListView;
    private ItemWidths mItemWidthInfo;
    private final OnLayoutChangeListener mListviewLayoutChangeListener = new OnLayoutChangeListener() {

        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int contentWidth = mItemWidthInfo.getLineContentsWidthPx();
            int scrollContainerWidth = (right - left)/2 - mItemWidthInfo.getLineNumberWidthPx();
            mListView.setHorizontalScrollRange(Math.max(0, contentWidth - scrollContainerWidth));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (HorizontalScrollObservingListView)
                findViewById(R.id.content_view);

        // TODO(sbezek): move to a loader
        List<CollapsedOrLine> diff = getDiff(getResources());

        StopWatch itemWidthTimer = StopWatch.start("calculate_item_widths");
        mItemWidthInfo = calculateItemWidths(getResources(), diff);
        itemWidthTimer.stopAndLog();

        mListView.setAdapter(new CollapsedSideBySideLineAdapter(diff, mItemWidthInfo, mListView));

        // If the listview changes dimensions (including the initial layout), we
        // need to recalculate the horizontal scroll range.
        mListView.addOnLayoutChangeListener(mListviewLayoutChangeListener);
    }

    private static List<CollapsedOrLine> getDiff(Resources resources) {
        StopWatch readTimer = StopWatch.start("read_original_file");
        final String[] lines = DummyContent.readLines(resources, R.raw.sample_view_before);
        readTimer.stopAndLog();
        Log.d(TAG, "Read " + lines.length + " lines");

        Parser parser = new Parser(System.out);
        SingleFileDiff d;
        try {
            d = parser.parse(DummyContent.getScanner(resources, R.raw.sample_view_diff));
        } catch (DiffParseException e) {
            // TODO(sbezek): handle this reasonably once diff parsing is factored out of here
            throw new RuntimeException(e);
        }


        StopWatch applyTimer = StopWatch.start("apply_diff");

        List<CollapsedOrLine> items = new ArrayList<CollapsedOrLine>();
        List<SideBySideLine> currentCollapse = null;

        ILineReader fooReader = new ArrayReader(lines);
        Iterator<Chunk> chunkIter = d.getChunks().iterator();
        Chunk nextChunk = chunkIter.hasNext() ? chunkIter.next() : null;

        /*
         * Generate the collapsed diff. Simple collapse policy, based on
         * collapsed parts of the original diff.
         *
         * (yes, it's pointless to have already generated the full output if
         * we're just going to collapse the code based on the unified diff, but
         * eventually we'll need that to do better/adjustable collapsing)
         */
        int leftLine = 1;
        int rightLine = 1;
        while (leftLine <= lines.length) {
            if (nextChunk == null || leftLine < nextChunk.getLeftStartLine()) {
                // No relevant chunk for this line, so just output the current line
                String line = fooReader.consumeLine();
                final SideBySideLine diffLine = new SideBySideLine(leftLine, line, rightLine, line);
                leftLine++;
                rightLine++;

                if (currentCollapse == null) {
                    currentCollapse = new ArrayList<SideBySideLine>();
                }
                currentCollapse.add(diffLine);
            } else {
                if (currentCollapse != null) {
                    items.add(CollapsedOrLine.of(new Collapsed(currentCollapse)));
                    currentCollapse = null;
                }

                for (SideBySideLine line : nextChunk.applyForward(fooReader)) {
                    items.add(CollapsedOrLine.of(line));
                    if (line.getLeftLine() != null) {
                        leftLine++;
                    }
                    if (line.getRightLine() != null) {
                        rightLine++;
                    }
                }
                nextChunk = chunkIter.hasNext() ? chunkIter.next() : null;
            }
        }

        if (currentCollapse != null) {
            items.add(CollapsedOrLine.of(new Collapsed(currentCollapse)));
            currentCollapse = null;
        }

        applyTimer.stopAndLog();

        return items;
    }

    private static ItemWidths calculateItemWidths(Resources resources, List<CollapsedOrLine> diff) {
        Paint p = new Paint();
        p.setTextSize(resources.getDimension(R.dimen.code_text_size));
        p.setTypeface(Typeface.MONOSPACE);

        // Because we're using a monospace font, cheat by finding widest line (#
        // of characters) and just measuring that once.
        // TODO(sbezek): XXX what about unicode? this is almost certainly completely flawed, though it might be possible to use a BreakIterator.getCharacterInstance()?
        int widestLineNumberChars = 2;
        int widestContentsChars = 20;
        for (CollapsedOrLine item : diff) {
            if (item.isCollapsed()) {
                continue;
            } else {
                SideBySideLine line = item.getLine();
                final String leftLine = line.getLeftLine();
                if (leftLine != null) {
                    widestLineNumberChars = Math.max(widestLineNumberChars,
                            String.valueOf(line.getLeftLineNumber()).length());
                    widestContentsChars = Math.max(widestContentsChars, leftLine.length());
                }
                final String rightLine = line.getRightLine();
                if (rightLine != null) {
                    widestLineNumberChars = Math.max(widestLineNumberChars,
                            String.valueOf(line.getRightLineNumber()).length());
                    widestContentsChars = Math.max(widestContentsChars, rightLine.length());
                }
            }
        }

        return new ItemWidths(
                getWidthOfNCharacters(p, widestLineNumberChars),
                getWidthOfNCharacters(p, widestContentsChars));
    }

    /**
     * Only valid for monospaced fonts.
     */
    private static int getWidthOfNCharacters(Paint p, int numChars) {
        // Only is valid for monospaced fonts
        Assert.isTrue(p.getTypeface() == Typeface.MONOSPACE);

        StringBuilder dummy = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            dummy.append("A");
        }
        return (int)p.measureText(dummy.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private static class ArrayReader implements ILineReader {

        private final String[] mLines;
        private int mCurrentLine = 0;

        public ArrayReader(String[] lines) {
            mLines = lines;
        }

        @Override
        public String consumeLine() {
            return mLines[mCurrentLine++];
        }
    }
}

package com.scottbezek.superdiff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;

import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.Collapsed;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;
import com.scottbezek.superdiff.list.SideBySideLineView.ItemWidths;
import com.scottbezek.superdiff.unified.Chunk;
import com.scottbezek.superdiff.unified.ILineReader;
import com.scottbezek.superdiff.unified.Parser;
import com.scottbezek.superdiff.unified.SideBySideLine;
import com.scottbezek.superdiff.unified.SingleFileDiff;
import com.scottbezek.util.Assert;

public class ListViewActivity extends Activity {

    private static final String TAG = ListViewActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.content_view);

        List<CollapsedOrLine> diff = getDiff();
        ItemWidths itemWidthInfo = calculateItemWidths(diff);

        LayoutParams lp = listView.getLayoutParams();
        // TODO(sbezek): this is kind of lame and ignores things like margins, padding, etc.
        lp.width = itemWidthInfo.getLineContentsWidthPx() * 2 + itemWidthInfo.getLineNumberWidthPx() * 2;
        listView.setLayoutParams(lp);

        listView.setAdapter(new CollapsedSideBySideLineAdapter(diff, itemWidthInfo));
    }

    private List<CollapsedOrLine> getDiff() {
        long startRead = SystemClock.elapsedRealtime();
        final String[] lines = DummyContent.readLines(getResources(), R.raw.sample_view_before);
        Log.d(TAG, "Read " + lines.length + "lines in " + (SystemClock.elapsedRealtime() - startRead) + "ms");

        Parser parser = new Parser(System.out);
        SingleFileDiff d = parser.parse(DummyContent.getScanner(getResources(), R.raw.sample_view_diff));


        long startApply = SystemClock.elapsedRealtime();

        List<CollapsedOrLine> items = new ArrayList<CollapsedOrLine>();
        List<SideBySideLine> currentCollapse = null;

        ILineReader fooReader = new ArrayReader(lines);
        Iterator<Chunk> chunkIter = d.getChunks().iterator();
        Chunk nextChunk = chunkIter.hasNext() ? chunkIter.next() : null;

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

        Log.d(TAG, "Applied diff in " + (SystemClock.elapsedRealtime() - startApply) + "ms");

        return items;
    }

    private ItemWidths calculateItemWidths(List<CollapsedOrLine> diff) {
        Paint p = new Paint();
        p.setTextSize(getResources().getDimension(R.dimen.code_text_size));
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

    private int getWidthOfNCharacters(Paint p, int numChars) {
        // Only is valid for monospaced fonts
        Assert.isTrue(p.getTypeface() == Typeface.MONOSPACE);

        StringBuilder dummy = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            dummy.append("A");
        }
        return (int)p.measureText(dummy.toString());
    }

//  for (Chunk c : d.getAllChunks()) {
//      fooReader.moveToLine(c.getLeftStartLine()-1);
//      c.applyForward(fooReader, fooWriter);
//  }

//
//
//  int widest = 0;
//  String widestText = "";
//  for (String line : lines) {
//      if (line.length() > widest) {
//          widest = line.length();
//          widestText = line;
//      }
//  }
//  // TODO also account for other things in the item (e.g line number)
//  LayoutParams lp = listView.getLayoutParams();
//  lp.width = (int)p.measureText(widestText);
//  listView.setLayoutParams(lp);

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

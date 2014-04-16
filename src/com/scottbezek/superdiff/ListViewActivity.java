package com.scottbezek.superdiff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;

import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.Collapsed;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;
import com.scottbezek.superdiff.unified.Chunk;
import com.scottbezek.superdiff.unified.ILineReader;
import com.scottbezek.superdiff.unified.Parser;
import com.scottbezek.superdiff.unified.SideBySideLine;
import com.scottbezek.superdiff.unified.SingleFileDiff;

public class ListViewActivity extends Activity {

    private static final String TAG = ListViewActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.content_view);
        LayoutParams lp = listView.getLayoutParams();
        lp.width = 1000;
        listView.setLayoutParams(lp);
        listView.setAdapter(new CollapsedSideBySideLineAdapter(getDiff()));
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
                    if (line.hasLeft()) {
                        leftLine++;
                    }
                    if (line.hasRight()) {
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

//  for (Chunk c : d.getAllChunks()) {
//      fooReader.moveToLine(c.getLeftStartLine()-1);
//      c.applyForward(fooReader, fooWriter);
//  }

//
//  Paint p = new Paint();
//  p.setTextSize(getResources().getDimension(R.dimen.code_text_size));
//  p.setTypeface(Typeface.MONOSPACE);
//
//  // Because we're using a monospace font, cheat by finding widest (characters) line and just measuring that once.
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

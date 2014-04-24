package com.scottbezek.superdiff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnLayoutChangeListener;

import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedUnknown;
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

    public static final String EXTRA_SAMPLE = "EXTRA_SAMPLE";

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
        final List<CollapsedOrLine> diff;
        final Intent intent = getIntent();
        final Uri dataUri = intent.getData();
        if (dataUri == null && !intent.hasExtra(EXTRA_SAMPLE)) {
            finish();
            return;
        } else {
            final InputStream diffInput;
            if (dataUri != null) {
                try{
                    diffInput = getContentResolver().openInputStream(dataUri);
                } catch (FileNotFoundException e) {
                    // TODO(sbezek): handle load failure reasonably once this is in a loader
                    throw new RuntimeException(e);
                }
            } else {
                String sampleName = intent.getStringExtra(EXTRA_SAMPLE);
                if (sampleName.contains("..")) {
                    finish();
                    return;
                }
                try {
                    diffInput = getResources().getAssets().open("samples/" + sampleName);
                } catch (IOException e) {
                    // TODO(sbezek): handle load failure reasonably once this is in a loader
                    throw new RuntimeException(e);
                }
            }
            try {
                diff = getCollapsedDiff(new Scanner(diffInput));
            } catch (DiffParseException e) {
                // TODO(sbezek): handle load failure reasonably once this is in a loader
                throw new RuntimeException(e);
            }
        }

        StopWatch itemWidthTimer = StopWatch.start("calculate_item_widths");
        mItemWidthInfo = calculateItemWidths(getResources(), diff);
        itemWidthTimer.stopAndLog();

        mListView.setAdapter(new CollapsedSideBySideLineAdapter(diff, mItemWidthInfo, mListView));

        // If the listview changes dimensions (including the initial layout), we
        // need to recalculate the horizontal scroll range.
        mListView.addOnLayoutChangeListener(mListviewLayoutChangeListener);
    }

    private static List<CollapsedOrLine> getCollapsedDiff(
            Scanner unifiedDiffFileContents) throws DiffParseException {
        Parser parser = new Parser(System.out);
        SingleFileDiff d = parser.parse(unifiedDiffFileContents);

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

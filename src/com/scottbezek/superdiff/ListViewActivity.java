package com.scottbezek.superdiff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.scottbezek.superdiff.list.MultiFileDiffAdapter;
import com.scottbezek.superdiff.list.SideBySideLineView.ItemWidths;
import com.scottbezek.superdiff.unified.Chunk;
import com.scottbezek.superdiff.unified.Parser;
import com.scottbezek.superdiff.unified.Parser.DiffParseException;
import com.scottbezek.superdiff.unified.SideBySideLine;
import com.scottbezek.superdiff.unified.SingleFileDiff;
import com.scottbezek.util.Assert;
import com.scottbezek.util.StopWatch;

public class ListViewActivity extends Activity {

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
        final Map<String, List<CollapsedOrLine>> diffByFilename;
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
                diffByFilename = getCollapsedDiffs(new Scanner(diffInput));
            } catch (DiffParseException e) {
                // TODO(sbezek): handle load failure reasonably once this is in a loader
                throw new RuntimeException(e);
            }
        }

        StopWatch itemWidthTimer = StopWatch.start("calculate_item_widths");
        // For now we only support a single horizontally-scrollable container
        // (in the future it might be nice to allow each file to be scrolled
        // separately), so just take the max widths of all file diffs
        int widestLineNumberWidth = 0;
        int widestLineContentsWidth = 0;
        for (List<CollapsedOrLine> fileDiff : diffByFilename.values()) {
            ItemWidths curItemWidths = calculateItemWidths(getResources(), fileDiff);
            if (curItemWidths.getLineNumberWidthPx() > widestLineNumberWidth) {
                widestLineNumberWidth = curItemWidths.getLineNumberWidthPx();
            }
            if (curItemWidths.getLineContentsWidthPx() > widestLineContentsWidth) {
                widestLineContentsWidth = curItemWidths.getLineContentsWidthPx();
            }
        }
        mItemWidthInfo = new ItemWidths(widestLineNumberWidth, widestLineContentsWidth);
        itemWidthTimer.stopAndLog();

        List<CollapsedSideBySideLineAdapter> adapters = new ArrayList<CollapsedSideBySideLineAdapter>();
        for (Entry<String, List<CollapsedOrLine>> entry : diffByFilename.entrySet()) {
            adapters.add(new CollapsedSideBySideLineAdapter(entry.getKey(),
                    entry.getValue(), mItemWidthInfo, mListView));
        }
        mListView.setAdapter(new MultiFileDiffAdapter(adapters));

        // If the listview changes dimensions (including the initial layout), we
        // need to recalculate the horizontal scroll range.
        mListView.addOnLayoutChangeListener(mListviewLayoutChangeListener);
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
}

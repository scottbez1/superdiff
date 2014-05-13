package com.scottbezek.superdiff;

import com.scottbezek.difflib.unified.SideBySideLine;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;
import com.scottbezek.superdiff.list.HorizontalScrollObservingListView;
import com.scottbezek.superdiff.list.MultiFileDiffAdapter;
import com.scottbezek.superdiff.list.SideBySideLineView.ItemWidths;
import com.scottbezek.superdiff.manager.DiffManager;
import com.scottbezek.superdiff.manager.DiffManager.DiffFailed;
import com.scottbezek.superdiff.manager.DiffManager.DiffLoadResult;
import com.scottbezek.superdiff.manager.DiffManager.DiffLoading;
import com.scottbezek.superdiff.manager.DiffManager.DiffStatus;
import com.scottbezek.superdiff.manager.IntralineDiffProcessor;
import com.scottbezek.superdiff.manager.StateStream;
import com.scottbezek.superdiff.manager.StateStream.Listener;
import com.scottbezek.util.Assert;
import com.scottbezek.util.StopWatch;

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
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ListViewActivity extends Activity {

    public static final String EXTRA_SAMPLE = "EXTRA_SAMPLE";

    private DiffManager mDiffManager;
    private ProgressBar mProgress;
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

    private StateStream<DiffStatus> mResultStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDiffManager = DiffManager.getInstance();

        mListView = (HorizontalScrollObservingListView)
                findViewById(R.id.content_view);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        // If the listview changes dimensions (including the initial layout), we
        // need to recalculate the horizontal scroll range.
        mListView.addOnLayoutChangeListener(mListviewLayoutChangeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initiateLoad();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResultStream.unsubscribe(mDiffListener);
    }

    private void initiateLoad() {
        final Intent intent = getIntent();
        final Uri dataUri = intent.getData();
        if (dataUri == null && !intent.hasExtra(EXTRA_SAMPLE)) {
            finish();
            return;
        } else {
            final IntralineDiffProcessor intralineDiffProcessor
                    = intralineDiffProcessorFromResources(getResources());
            if (dataUri != null) {
                mResultStream = mDiffManager
                        .loadContentUri(getContentResolver(), dataUri, intralineDiffProcessor);
            } else {
                String sampleName = intent.getStringExtra(EXTRA_SAMPLE);
                if (sampleName.contains("..")) {
                    finish();
                    return;
                }
                mResultStream = mDiffManager
                        .loadSample(getAssets(), sampleName, intralineDiffProcessor);
            }
            mResultStream.subscribeInvoke(mDiffListener);
        }
    }

    private static IntralineDiffProcessor intralineDiffProcessorFromResources(Resources resources) {
        int addedCharactersBackgroundColor = resources
                .getColor(R.color.diff_chars_added_background);
        int removedCharactersBackgroundColor = resources
                .getColor(R.color.diff_chars_removed_background);
        return new IntralineDiffProcessor(resources.getConfiguration().locale,
                removedCharactersBackgroundColor, addedCharactersBackgroundColor);
    }

    private final Listener mDiffListener = new Listener<DiffStatus>() {
        @Override
        public void onStateChanged(DiffStatus state) {
            if (state instanceof DiffLoading) {
                mListView.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);
            } else if (state instanceof DiffFailed) {
                mProgress.setVisibility(View.GONE);
                DiffFailed failure = (DiffFailed)state;
                Toast.makeText(ListViewActivity.this, failure.getCause().getMessage(), Toast.LENGTH_LONG).show();
            } else if (state instanceof DiffLoadResult) {
                Map<String, List<CollapsedOrLine>> diffByFilename = ((DiffLoadResult)state).getDiffByFilename();

                // TODO(sbezek): move this to a background thread; make the result contain the 'Spanned' results
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

                mListView.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.GONE);
            }
        }
    };

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
                final CharSequence leftLine = line.getLeftLine();
                if (leftLine != null) {
                    widestLineNumberChars = Math.max(widestLineNumberChars,
                            String.valueOf(line.getLeftLineNumber()).length());
                    widestContentsChars = Math.max(widestContentsChars, leftLine.length());
                }
                final CharSequence rightLine = line.getRightLine();
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

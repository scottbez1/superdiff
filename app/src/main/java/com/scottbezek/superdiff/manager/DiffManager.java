package com.scottbezek.superdiff.manager;

import com.scottbezek.difflib.unified.Parser.DiffParseException;
import com.scottbezek.superdiff.list.CollapsedSideBySideLineAdapter.CollapsedOrLine;

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

/**
 * Manages parsing/computation of diffs.
 */
public class DiffManager {

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    private static final Object sInstanceLock = new Object();

    private static DiffManager sInstance;

    @Nonnull
    public static DiffManager getInstance() {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new DiffManager();
            }
            return sInstance;
        }
    }

    private DiffManager() {
    }

    /**
     * Marker interface for classes that represent the status of a diff.
     *
     * @see com.scottbezek.superdiff.manager.DiffManager.DiffLoading
     * @see com.scottbezek.superdiff.manager.DiffManager.DiffLoadResult
     * @see com.scottbezek.superdiff.manager.DiffManager.DiffFailed
     */
    public interface DiffStatus {

    }

    /**
     * Status indicating that the diff is loading.
     */
    public static class DiffLoading implements DiffStatus {

    }

    /**
     * Status indicating that the diff failed to load.
     */
    public static class DiffFailed implements DiffStatus {

        private final IOException mIoException;

        private final DiffParseException mDiffParseException;

        public DiffFailed(IOException cause) {
            mIoException = cause;
            mDiffParseException = null;
        }

        public DiffFailed(DiffParseException cause) {
            mIoException = null;
            mDiffParseException = cause;
        }

        public void rethrow() throws IOException, DiffParseException {
            if (mIoException != null) {
                throw mIoException;
            } else if (mDiffParseException != null) {
                throw mDiffParseException;
            }
        }

        @Nonnull
        public Exception getCause() {
            if (mIoException != null) {
                return mIoException;
            } else if (mDiffParseException != null) {
                return mDiffParseException;
            } else {
                throw new RuntimeException();
            }
        }
    }

    /**
     * Result of successful diff loading. Contains the result diff data (see {@link
     * #getDiffByFilename()}).
     */
    public static class DiffLoadResult implements DiffStatus {

        private final Map<String, List<CollapsedOrLine>> mDiffByFilename;

        public DiffLoadResult(Map<String, List<CollapsedOrLine>> diffByFilename) {
            mDiffByFilename = diffByFilename;
        }

        public Map<String, List<CollapsedOrLine>> getDiffByFilename() {
            return mDiffByFilename;
        }
    }

    /**
     * Load one of the sample diffs from the apk assets.
     *
     * @param assets     {@link android.content.res.AssetManager} for retrieving the sample
     *                   contents.
     * @param sampleName Name of the sample unified diff file to load. Must not contain '..'
     * @return A {@link com.scottbezek.superdiff.manager.StateStream} which will be updated as the
     * diff is loaded.
     */
    public StateStream<DiffStatus> loadSample(AssetManager assets, String sampleName) {
        StateStream<DiffStatus> state = new StateStream<DiffStatus>(new DiffLoading());
        // TODO(sbezek): do some caching?

        mExecutor.execute(new SampleLoader(assets, sampleName, state));
        return state;
    }

    /**
     * Load a unified diff file from a content provider.
     *
     * @param contentResolver For retrieving the data contents.
     * @param dataUri         Identifier for the content to load.
     * @return A {@link com.scottbezek.superdiff.manager.StateStream} which will be updated as the
     * diff is loaded.
     */
    public StateStream<DiffStatus> loadContentUri(ContentResolver contentResolver, Uri dataUri) {
        StateStream<DiffStatus> state = new StateStream<DiffStatus>(new DiffLoading());
        // TODO(sbezek): do some caching?

        mExecutor.execute(new ContentLoader(contentResolver, dataUri, state));
        return state;
    }

    /**
     * Loads a sample diff.
     */
    public static class SampleLoader implements Runnable {

        private final AssetManager mAssets;

        private final String mSampleName;

        private final StateStream<DiffStatus> mOutput;

        public SampleLoader(AssetManager assets, String sampleName,
                StateStream<DiffStatus> output) {
            if (sampleName.contains("..")) {
                throw new IllegalStateException("Path cannot contain '..'");
            }
            mAssets = assets;
            mSampleName = sampleName;
            mOutput = output;
        }

        @Override
        public void run() {
            InputStream diffInput;
            try {
                diffInput = mAssets.open("samples/" + mSampleName);
            } catch (IOException e) {
                mOutput.update(new DiffFailed(e));
                return;
            }
            DiffLoadTask loadTask = new DiffLoadTask(diffInput, mOutput);
            loadTask.run();
        }
    }


    /**
     * Loads a diff from a {@link android.content.ContentResolver}.
     */
    public static class ContentLoader implements Runnable {

        private final ContentResolver mContentResolver;

        private final Uri mDataUri;

        private final StateStream<DiffStatus> mOutput;

        public ContentLoader(ContentResolver contentResolver, Uri dataUri,
                StateStream<DiffStatus> output) {
            mContentResolver = contentResolver;
            mDataUri = dataUri;
            mOutput = output;
        }

        @Override
        public void run() {
            InputStream diffInput;
            try {
                diffInput = mContentResolver.openInputStream(mDataUri);
            } catch (FileNotFoundException e) {
                mOutput.update(new DiffFailed(e));
                return;
            }
            DiffLoadTask loadTask = new DiffLoadTask(diffInput, mOutput);
            loadTask.run();
        }
    }
}

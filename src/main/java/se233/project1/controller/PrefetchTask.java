package se233.project1.controller;

import javafx.concurrent.Task;
import se233.project1.model.ImageItem;
import se233.project1.model.VectorSettings;

import java.util.List;

/**
 * Traces the settings one click away from the current ones in the background and caches them,
 * so switching detail level, colors or background removal shows the result instantly.
 */
public class PrefetchTask extends Task<Void> {
    private final ImageItem item;
    private final List<VectorSettings> variants;
    private final Vectorizer vectorizer;
    private boolean stopped = false;

    public PrefetchTask(ImageItem item, List<VectorSettings> variants, Vectorizer vectorizer) {
        this.item = item;
        this.variants = variants;
        this.vectorizer = vectorizer;
    }

    /** Asks the task to stop before its next trace (called when the user does something else). */
    public synchronized void stopPrefetch() {
        stopped = true;
    }

    private synchronized boolean isStopped() {
        return stopped;
    }

    @Override
    protected Void call() {
        for (VectorSettings settings : variants) {
            if (isStopped()) {
                return null;
            }
            if (item.getCachedPreview(settings) != null) {
                continue;
            }
            try {
                item.cachePreview(vectorizer.vectorize(item.getPixels(), item.getWidth(), item.getHeight(),
                        item.getAnalysis(), settings, Vectorizer.PREVIEW_MAX_SIDE));
            } catch (Exception e) {
                // a failed prefetch is not a problem: the preview is traced again when it is needed
                return null;
            }
        }
        return null;
    }
}

package se233.project1.controller;

import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.project1.model.ImageItem;
import se233.project1.model.VectorSettings;

import java.util.List;

/**
 * Traces the settings one click away from the current ones in the background and caches them,
 * so switching detail level, colors or background removal shows the result instantly.
 */
public class PrefetchTask extends Task<Void> {
    private static final Logger logger = LogManager.getLogger(PrefetchTask.class);
    private final ImageItem item;
    private final List<VectorSettings> variants;
    private final Vectorizer vectorizer;

    public PrefetchTask(ImageItem item, List<VectorSettings> variants, Vectorizer vectorizer) {
        this.item = item;
        this.variants = variants;
        this.vectorizer = vectorizer;
    }

    @Override
    protected Void call() {
        for (VectorSettings settings : variants) {
            if (isCancelled()) {
                return null;
            }
            if (item.getCachedPreview(settings) != null) {
                continue;
            }
            try {
                item.cachePreview(vectorizer.vectorize(item.getPixels(), item.getWidth(), item.getHeight(),
                        item.getAnalysis(), settings, Vectorizer.PREVIEW_MAX_SIDE));
            } catch (InterruptedException e) {
                return null; // cancelled
            } catch (Exception e) {
                logger.debug("Prefetch failed: {}", e.getMessage());
            }
        }
        return null;
    }
}

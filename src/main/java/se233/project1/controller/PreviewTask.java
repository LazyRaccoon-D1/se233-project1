package se233.project1.controller;

import javafx.concurrent.Task;
import se233.project1.model.ImageItem;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

/** Traces the preview of one image for the settings the user just picked, on a background thread. */
public class PreviewTask extends Task<VectorResult> {
    private final ImageItem item;
    private final VectorSettings settings;
    private final Vectorizer vectorizer;

    public PreviewTask(ImageItem item, VectorSettings settings, Vectorizer vectorizer) {
        this.item = item;
        this.settings = settings;
        this.vectorizer = vectorizer;
    }

    @Override
    protected VectorResult call() throws Exception {
        VectorResult result = vectorizer.vectorize(item.getPixels(), item.getWidth(), item.getHeight(),
                item.getAnalysis(), settings, Vectorizer.PREVIEW_MAX_SIDE);
        item.cachePreview(result);
        return result;
    }
}

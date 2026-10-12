package se233.project1.controller;

import javafx.concurrent.Task;
import se233.project1.exception.PotraceNotFoundException;
import se233.project1.model.ImageItem;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

/**
 * Traces the preview of one image for the settings the user just picked, on a background thread.
 * Errors are caught here; the result is null and getErrorMessage() explains what went wrong.
 */
public class PreviewTask extends Task<VectorResult> {
    private final ImageItem item;
    private final VectorSettings settings;
    private final Vectorizer vectorizer;
    private String errorMessage;
    private boolean potraceMissing;

    public PreviewTask(ImageItem item, VectorSettings settings, Vectorizer vectorizer) {
        this.item = item;
        this.settings = settings;
        this.vectorizer = vectorizer;
    }

    @Override
    protected VectorResult call() {
        try {
            VectorResult result = vectorizer.vectorize(item.getPixels(), item.getWidth(), item.getHeight(),
                    item.getAnalysis(), settings, Vectorizer.PREVIEW_MAX_SIDE);
            item.cachePreview(result);
            return result;
        } catch (PotraceNotFoundException e) {
            potraceMissing = true;
            errorMessage = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        }
        return null;
    }

    public ImageItem getItem() { return item; }
    public VectorSettings getSettings() { return settings; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isPotraceMissing() { return potraceMissing; }
}

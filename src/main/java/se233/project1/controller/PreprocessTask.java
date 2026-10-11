package se233.project1.controller;

import javafx.application.Platform;
import javafx.scene.image.Image;
import se233.project1.model.ColorAnalysis;
import se233.project1.model.ImageItem;
import se233.project1.model.ItemStatus;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

import java.util.concurrent.Callable;

/**
 * Preprocessing of one dropped image, run on a worker thread:
 * load the image, detect its colors, configure Medium detail + 2 colors, then trace the preview.
 */
public class PreprocessTask implements Callable<ImageItem> {
    private final ImageItem item;
    private final Vectorizer vectorizer;

    public PreprocessTask(ImageItem item, Vectorizer vectorizer) {
        this.item = item;
        this.vectorizer = vectorizer;
    }

    @Override
    public ImageItem call() throws Exception {
        Platform.runLater(() -> item.setStatus(ItemStatus.PROCESSING));

        Image image = ImageLoader.load(item.getSource());
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = ImageLoader.readPixels(image);
        ColorAnalysis analysis = ColorAnalyzer.analyze(pixels);
        VectorSettings settings = VectorSettings.defaults(analysis);
        item.setPixels(pixels, width, height);
        item.setAnalysis(analysis);
        item.setSettings(settings);
        Platform.runLater(() -> item.setFxImage(image));

        VectorResult result = vectorizer.vectorize(pixels, width, height, analysis, settings,
                Vectorizer.PREVIEW_MAX_SIDE);
        item.cachePreview(result);
        Platform.runLater(() -> {
            item.setMessage(analysis.getDetectedColors() + " colors detected");
            item.setStatus(ItemStatus.READY);
        });
        return item;
    }
}

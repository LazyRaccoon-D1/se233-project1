package se233.project1.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * One image dropped into the application together with its settings and cached previews.
 * Worker threads and the JavaFX thread both use this object, so the shared data is accessed through
 * synchronized methods. The JavaFX properties are only changed on the JavaFX Application Thread.
 */
public class ImageItem {
    private final File source;
    private final String displayName;

    private int[] pixels;
    private int width;
    private int height;
    private ColorAnalysis analysis;
    private VectorSettings settings;
    private final Map<String, VectorResult> previewCache = new HashMap<>();

    private final ObjectProperty<Image> fxImage = new SimpleObjectProperty<>();
    private final ObjectProperty<ItemStatus> status = new SimpleObjectProperty<>(ItemStatus.QUEUED);
    private final StringProperty message = new SimpleStringProperty("");

    public ImageItem(File source) {
        this.source = source;
        this.displayName = source.getName();
    }

    public File getSource() { return source; }
    public String getDisplayName() { return displayName; }

    public synchronized void setPixels(int[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public synchronized int[] getPixels() { return pixels; }
    public synchronized int getWidth() { return width; }
    public synchronized int getHeight() { return height; }

    public synchronized ColorAnalysis getAnalysis() { return analysis; }
    public synchronized void setAnalysis(ColorAnalysis analysis) { this.analysis = analysis; }

    public synchronized VectorSettings getSettings() { return settings; }
    public synchronized void setSettings(VectorSettings settings) { this.settings = settings; }

    public synchronized boolean isLoaded() {
        return pixels != null && analysis != null && settings != null;
    }

    public synchronized VectorResult getCachedPreview(VectorSettings s) {
        return previewCache.get(s.getKey());
    }

    public synchronized void cachePreview(VectorResult result) {
        previewCache.put(result.getSettings().getKey(), result);
    }

    public ObjectProperty<Image> fxImageProperty() { return fxImage; }
    public Image getFxImage() { return fxImage.get(); }
    public void setFxImage(Image image) { fxImage.set(image); }

    public ObjectProperty<ItemStatus> statusProperty() { return status; }
    public ItemStatus getStatus() { return status.get(); }
    public void setStatus(ItemStatus s) { status.set(s); }

    public StringProperty messageProperty() { return message; }
    public String getMessage() { return message.get(); }
    public void setMessage(String m) { message.set(m); }

    public String getBaseName() {
        int dot = displayName.lastIndexOf('.');
        if (dot > 0) {
            return displayName.substring(0, dot);
        }
        return displayName;
    }
}

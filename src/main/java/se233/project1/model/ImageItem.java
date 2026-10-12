package se233.project1.model;

import javafx.scene.image.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One image dropped into the application together with its settings and cached previews.
 * Worker threads and the JavaFX thread both use this object, so the shared data is accessed through
 * synchronized methods. The status, message and display image are only changed on the JavaFX
 * Application Thread (through Platform.runLater); every change runs the registered listeners.
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

    private Image fxImage;
    private ItemStatus status = ItemStatus.QUEUED;
    private String message = "";
    private final List<Runnable> listeners = new ArrayList<>();

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

    /** Registers code to run whenever the status, message or display image changes. */
    public void addChangeListener(Runnable listener) { listeners.add(listener); }
    public void removeChangeListener(Runnable listener) { listeners.remove(listener); }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    public Image getFxImage() { return fxImage; }

    public void setFxImage(Image image) {
        fxImage = image;
        notifyListeners();
    }

    public ItemStatus getStatus() { return status; }

    public void setStatus(ItemStatus s) {
        status = s;
        notifyListeners();
    }

    public String getMessage() { return message; }

    public void setMessage(String m) {
        message = m;
        notifyListeners();
    }

    public String getBaseName() {
        int dot = displayName.lastIndexOf('.');
        if (dot > 0) {
            return displayName.substring(0, dot);
        }
        return displayName;
    }
}

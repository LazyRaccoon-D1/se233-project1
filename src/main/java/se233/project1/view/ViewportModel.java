package se233.project1.view;

import javafx.geometry.Bounds;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Zoom and pan state shared by the original and the vectorized preview, so both always show the same
 * region of the image.
 */
public class ViewportModel {
    public static final double MIN_ZOOM = 0.02;
    public static final double MAX_ZOOM = 40;
    public static final double ZOOM_STEP = 1.25;
    private static final double FIT_MARGIN = 0.94;

    private double zoom = 1;
    private double offsetX = 0;
    private double offsetY = 0;
    private final List<Runnable> listeners = new ArrayList<>();
    private double contentWidth = 1, contentHeight = 1;
    private double viewWidth, viewHeight;
    private boolean fitMode = true;

    public double getZoom() { return zoom; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }

    /** Registers code to run whenever the zoom or the pan offset changes (both preview panes do this). */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public void setContentSize(double width, double height) {
        contentWidth = Math.max(1, width);
        contentHeight = Math.max(1, height);
        zoomToFit();
    }

    public void setViewSize(double width, double height) {
        viewWidth = width;
        viewHeight = height;
        if (fitMode) {
            zoomToFit();
        }
    }

    public void zoomToFit() {
        fitMode = true;
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        zoom = clamp(Math.min(viewWidth / contentWidth, viewHeight / contentHeight) * FIT_MARGIN);
        offsetX = (viewWidth - contentWidth * zoom) / 2;
        offsetY = (viewHeight - contentHeight * zoom) / 2;
        notifyListeners();
    }

    public void zoomIn() { zoomAt(ZOOM_STEP, viewWidth / 2, viewHeight / 2); }

    public void zoomOut() { zoomAt(1 / ZOOM_STEP, viewWidth / 2, viewHeight / 2); }

    /** Zooms by {@code factor} keeping the point (pivotX, pivotY) of the view in place. */
    public void zoomAt(double factor, double pivotX, double pivotY) {
        double z = clamp(zoom * factor);
        if (z == zoom) {
            return;
        }
        fitMode = false;
        double ratio = z / zoom;
        offsetX = pivotX - (pivotX - offsetX) * ratio;
        offsetY = pivotY - (pivotY - offsetY) * ratio;
        zoom = z;
        notifyListeners();
    }

    public void panBy(double dx, double dy) {
        fitMode = false;
        offsetX = offsetX + dx;
        offsetY = offsetY + dy;
        notifyListeners();
    }

    private static double clamp(double z) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
    }

    /**
     * Places a node so that its local point (x, y) appears at (offsetX + scaleX * x, offsetY + scaleY * y).
     * setScaleX/Y scale a node around the centre of its bounds, so the layout position compensates for that.
     */
    public static void place(Node node, double scaleX, double scaleY, double offsetX, double offsetY) {
        Bounds bounds = node.getLayoutBounds();
        double centerX = bounds.getMinX() + bounds.getWidth() / 2;
        double centerY = bounds.getMinY() + bounds.getHeight() / 2;
        node.setScaleX(scaleX);
        node.setScaleY(scaleY);
        node.setLayoutX(offsetX + centerX * (scaleX - 1));
        node.setLayoutY(offsetY + centerY * (scaleY - 1));
    }
}

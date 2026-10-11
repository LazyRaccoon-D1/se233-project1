package se233.project1.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;

/**
 * Zoom and pan state shared by the original and the vectorized preview, so both always show the same
 * region of the image.
 */
public class ViewportModel {
    public static final double MIN_ZOOM = 0.02;
    public static final double MAX_ZOOM = 40;
    public static final double ZOOM_STEP = 1.25;
    private static final double FIT_MARGIN = 0.94;

    private final DoubleProperty zoom = new SimpleDoubleProperty(1);
    private final DoubleProperty offsetX = new SimpleDoubleProperty(0);
    private final DoubleProperty offsetY = new SimpleDoubleProperty(0);
    private double contentWidth = 1, contentHeight = 1;
    private double viewWidth, viewHeight;
    private boolean fitMode = true;

    public DoubleProperty zoomProperty() { return zoom; }
    public DoubleProperty offsetXProperty() { return offsetX; }
    public DoubleProperty offsetYProperty() { return offsetY; }
    public double getZoom() { return zoom.get(); }

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
        double z = clamp(Math.min(viewWidth / contentWidth, viewHeight / contentHeight) * FIT_MARGIN);
        zoom.set(z);
        offsetX.set((viewWidth - contentWidth * z) / 2);
        offsetY.set((viewHeight - contentHeight * z) / 2);
    }

    public void zoomIn() { zoomAt(ZOOM_STEP, viewWidth / 2, viewHeight / 2); }

    public void zoomOut() { zoomAt(1 / ZOOM_STEP, viewWidth / 2, viewHeight / 2); }

    /** Zooms by {@code factor} keeping the point (pivotX, pivotY) of the view in place. */
    public void zoomAt(double factor, double pivotX, double pivotY) {
        double old = zoom.get();
        double z = clamp(old * factor);
        if (z == old) {
            return;
        }
        fitMode = false;
        double ratio = z / old;
        offsetX.set(pivotX - (pivotX - offsetX.get()) * ratio);
        offsetY.set(pivotY - (pivotY - offsetY.get()) * ratio);
        zoom.set(z);
    }

    public void panBy(double dx, double dy) {
        fitMode = false;
        offsetX.set(offsetX.get() + dx);
        offsetY.set(offsetY.get() + dy);
    }

    private static double clamp(double z) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
    }

    /**
     * Places a node so that its local point (x, y) appears at (offsetX + scaleX * x, offsetY + scaleY * y).
     * setScaleX/Y scale a node around the centre of its bounds, so the translation compensates for that.
     */
    public static void place(Node node, double scaleX, double scaleY, double offsetX, double offsetY) {
        Bounds bounds = node.getLayoutBounds();
        double centerX = bounds.getMinX() + bounds.getWidth() / 2;
        double centerY = bounds.getMinY() + bounds.getHeight() / 2;
        node.setScaleX(scaleX);
        node.setScaleY(scaleY);
        node.setTranslateX(offsetX + centerX * (scaleX - 1));
        node.setTranslateY(offsetY + centerY * (scaleY - 1));
    }
}

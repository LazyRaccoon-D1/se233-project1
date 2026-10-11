package se233.project1.view;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import se233.project1.model.VectorResult;

/** Builds the JavaFX nodes shown in the preview panes, both drawn in original image pixels. */
public class VectorNodeFactory {

    /**
     * Draws the traced layers with SVGPath nodes. Potrace coordinates are mapped to image pixels with the
     * same transform the SVG file uses: x' = scaleX * (tx + sx * x), y' = scaleY * (ty + sy * y).
     */
    public static Node vectorNode(VectorResult result) {
        Group layers = new Group();
        for (VectorResult.Layer layer : result.getLayers()) {
            SVGPath path = new SVGPath();
            path.setContent(layer.getPathData());
            int rgb = layer.getRgb();
            path.setFill(Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
            layers.getChildren().add(path);
        }
        if (!layers.getChildren().isEmpty()) {
            ViewportModel.place(layers,
                    result.getScaleX() * result.getPotraceSx(), result.getScaleY() * result.getPotraceSy(),
                    result.getScaleX() * result.getPotraceTx(), result.getScaleY() * result.getPotraceTy());
        }
        return new Group(layers);
    }

    /** The original image; pixels stay sharp when zoomed in so the difference to the vector is visible. */
    public static Node rasterNode(Image image, ViewportModel model) {
        ImageView view = new ImageView(image);
        view.smoothProperty().bind(model.zoomProperty().lessThan(2));
        return view;
    }
}

package se233.project1.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * One side of the side-by-side preview: a titled viewport (drag to pan, double-click to fit) over a
 * checkerboard so transparent areas are visible, with a caption and a loading / error overlay.
 */
public class PreviewPane extends VBox {
    private final ViewportModel model;
    private final Label caption = new Label();
    private final Pane holder = new Pane();
    private final VBox overlay = new VBox(10);
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label overlayText = new Label();
    private double lastX, lastY;

    public PreviewPane(String titleText, ViewportModel model, boolean drivesViewSize) {
        this.model = model;
        getStyleClass().add("preview-pane");
        Label title = new Label(titleText);
        title.getStyleClass().add("pane-title");
        title.setMaxWidth(Double.MAX_VALUE);
        caption.getStyleClass().add("pane-caption");
        caption.setMaxWidth(Double.MAX_VALUE);

        Pane viewport = new Pane();
        viewport.getStyleClass().add("preview-viewport");
        viewport.setMinSize(0, 0);
        viewport.setPrefSize(100, 100);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
        viewport.getChildren().add(holder);

        overlay.getStyleClass().add("preview-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMouseTransparent(true);
        spinner.setPrefSize(42, 42);
        overlayText.getStyleClass().add("overlay-text");
        overlayText.setWrapText(true);
        overlay.getChildren().addAll(spinner, overlayText);
        overlay.setVisible(false);

        StackPane stack = new StackPane(viewport, overlay);
        stack.setMinSize(0, 0);
        VBox.setVgrow(stack, Priority.ALWAYS);
        getChildren().addAll(title, stack, caption);

        model.addChangeListener(() -> updatePlacement());
        if (drivesViewSize) {
            viewport.widthProperty().addListener((obs, o, n) -> model.setViewSize(n.doubleValue(), viewport.getHeight()));
            viewport.heightProperty().addListener((obs, o, n) -> model.setViewSize(viewport.getWidth(), n.doubleValue()));
        }

        stack.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
        });
        stack.setOnMouseDragged(e -> {
            model.panBy(e.getX() - lastX, e.getY() - lastY);
            lastX = e.getX();
            lastY = e.getY();
        });
        stack.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                model.zoomToFit();
            }
        });
    }

    /** Shows a node drawn in image pixel coordinates (0,0 to width,height). */
    public void setContent(Node node, double width, double height) {
        holder.setMinSize(width, height);
        holder.setPrefSize(width, height);
        holder.setMaxSize(width, height);
        holder.getChildren().clear();
        if (node != null) {
            holder.getChildren().add(node);
        }
        holder.autosize();
        updatePlacement();
    }

    public void clearContent() {
        holder.getChildren().clear();
    }

    private void updatePlacement() {
        ViewportModel.place(holder, model.getZoom(), model.getZoom(), model.getOffsetX(), model.getOffsetY());
    }

    public void setCaption(String text) {
        caption.setText(text);
    }

    public void showLoading(String text) {
        spinner.setVisible(true);
        spinner.setManaged(true);
        overlayText.setText(text);
        overlay.getStyleClass().remove("error");
        overlay.setVisible(true);
    }

    public void showError(String text) {
        spinner.setVisible(false);
        spinner.setManaged(false);
        overlayText.setText(text);
        if (!overlay.getStyleClass().contains("error")) {
            overlay.getStyleClass().add("error");
        }
        overlay.setVisible(true);
    }

    public void hideOverlay() {
        overlay.setVisible(false);
    }
}

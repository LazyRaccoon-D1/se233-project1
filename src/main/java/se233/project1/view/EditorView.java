package se233.project1.view;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Main editor: the zoom panel on top, original and vectorized previews side by side in the centre,
 * settings on the right, batch queue and progress at the bottom.
 */
public class EditorView extends BorderPane {
    private final ViewportModel viewport = new ViewportModel();
    private final Button zoomInButton = new Button("+  Zoom In");
    private final Button zoomOutButton = new Button("−  Zoom Out");
    private final Button zoomFitButton = new Button("⤢  Zoom to Fit");
    private final PreviewPane originalPane = new PreviewPane("Original Image", viewport, true);
    private final PreviewPane resultPane = new PreviewPane("Vectorized Result", viewport, false);
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private final QueueStrip queueStrip = new QueueStrip();
    private final StatusBar statusBar = new StatusBar();

    public EditorView() {
        getStyleClass().add("editor");

        // the top panel contains only the three zoom controls
        zoomInButton.setOnAction(e -> viewport.zoomIn());
        zoomOutButton.setOnAction(e -> viewport.zoomOut());
        zoomFitButton.setOnAction(e -> viewport.zoomToFit());
        HBox topPanel = new HBox(6, zoomInButton, zoomOutButton, zoomFitButton);
        topPanel.getStyleClass().add("top-panel");
        setTop(topPanel);

        originalPane.setMinWidth(0);
        originalPane.setPrefWidth(0);
        originalPane.setMaxWidth(Double.MAX_VALUE);
        resultPane.setMinWidth(0);
        resultPane.setPrefWidth(0);
        resultPane.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(originalPane, Priority.ALWAYS);
        HBox.setHgrow(resultPane, Priority.ALWAYS);
        HBox previews = new HBox(8, originalPane, resultPane);
        previews.getStyleClass().add("previews");
        setCenter(previews);
        setRight(settingsPanel);
        setBottom(new VBox(queueStrip, statusBar));
    }

    public ViewportModel getViewport() { return viewport; }
    public PreviewPane getOriginalPane() { return originalPane; }
    public PreviewPane getResultPane() { return resultPane; }
    public SettingsPanel getSettingsPanel() { return settingsPanel; }
    public QueueStrip getQueueStrip() { return queueStrip; }
    public StatusBar getStatusBar() { return statusBar; }
}

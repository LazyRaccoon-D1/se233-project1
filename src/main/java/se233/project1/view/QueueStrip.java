package se233.project1.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import se233.project1.model.ImageItem;
import se233.project1.model.ItemStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Batch queue along the bottom of the editor (the additional feature): a thumbnail per image with its live
 * processing status. Click a thumbnail to open it, remove one image, or clear the batch. Dropping more
 * files onto the editor adds them to the batch.
 */
public class QueueStrip extends HBox {
    private static final double THUMB = 58;

    private final HBox tiles = new HBox(8);
    private final Button clearButton = new Button("Clear all");
    private final Label summary = new Label();
    private final List<VBox> tileNodes = new ArrayList<>();
    private Consumer<Integer> onSelect = i -> {
    };
    private Consumer<ImageItem> onRemove = i -> {
    };

    public QueueStrip() {
        getStyleClass().add("queue-strip");
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);

        tiles.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(tiles);
        scroll.getStyleClass().add("queue-scroll");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToHeight(true);
        scroll.setMinHeight(THUMB + 40);
        scroll.setPrefHeight(THUMB + 40);
        HBox.setHgrow(scroll, Priority.ALWAYS);

        Label hint = new Label("Drop more files here\nto add them");
        hint.getStyleClass().add("hint");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        summary.getStyleClass().add("hint");
        VBox actions = new VBox(6, summary, hint, clearButton);
        actions.setAlignment(Pos.CENTER);
        actions.setPrefWidth(130);
        getChildren().addAll(scroll, actions);
    }

    public void setOnSelect(Consumer<Integer> onSelect) { this.onSelect = onSelect; }
    public void setOnRemove(Consumer<ImageItem> onRemove) { this.onRemove = onRemove; }
    public Button getClearButton() { return clearButton; }

    /** Rebuilds the thumbnails for the current list of images. */
    public void refresh(List<ImageItem> items, int currentIndex) {
        tiles.getChildren().clear();
        tileNodes.clear();
        for (int i = 0; i < items.size(); i++) {
            VBox tile = tile(items.get(i), i);
            tileNodes.add(tile);
            tiles.getChildren().add(tile);
        }
        summary.setText(items.size() + (items.size() == 1 ? " image" : " images"));
        select(currentIndex);
    }

    public void select(int currentIndex) {
        for (int i = 0; i < tileNodes.size(); i++) {
            tileNodes.get(i).getStyleClass().remove("selected");
            if (i == currentIndex) {
                tileNodes.get(i).getStyleClass().add("selected");
            }
        }
    }

    /** Disables queue editing while a batch job runs. */
    public void setEditingDisabled(boolean disabled) {
        clearButton.setDisable(disabled);
        for (VBox tile : tileNodes) {
            tile.lookupAll(".remove-button").forEach(b -> b.setDisable(disabled));
        }
    }

    private VBox tile(ImageItem item, int index) {
        ImageView thumb = new ImageView();
        thumb.setFitWidth(THUMB);
        thumb.setFitHeight(THUMB);
        thumb.setPreserveRatio(true);
        thumb.imageProperty().bind(item.fxImageProperty());

        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(26, 26);
        busy.setMouseTransparent(true);
        Label badge = new Label();
        badge.getStyleClass().add("status-badge");
        badge.setMouseTransparent(true);
        StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);

        Button remove = new Button("✕");
        remove.getStyleClass().add("remove-button");
        remove.setOnAction(e -> onRemove.accept(item));
        remove.setVisible(false);
        StackPane.setAlignment(remove, Pos.TOP_RIGHT);

        StackPane frame = new StackPane(thumb, busy, badge, remove);
        frame.getStyleClass().add("thumb-frame");
        frame.setMinSize(THUMB + 8, THUMB + 8);
        frame.setPrefSize(THUMB + 8, THUMB + 8);
        frame.setMaxSize(THUMB + 8, THUMB + 8);

        Label name = new Label(item.getDisplayName());
        name.getStyleClass().add("thumb-name");
        name.setMaxWidth(THUMB + 8);
        Label status = new Label();
        status.getStyleClass().add("thumb-status");
        status.setMaxWidth(THUMB + 8);

        VBox tile = new VBox(2, frame, name, status);
        tile.getStyleClass().add("thumb-tile");
        tile.setAlignment(Pos.CENTER);
        tile.setOnMouseClicked(e -> onSelect.accept(index));
        tile.setOnMouseEntered(e -> remove.setVisible(true));
        tile.setOnMouseExited(e -> remove.setVisible(false));

        Runnable update = () -> {
            ItemStatus s = item.getStatus();
            boolean working = s == ItemStatus.PROCESSING || s == ItemStatus.EXPORTING;
            busy.setVisible(working);
            thumb.setOpacity(working || s == ItemStatus.QUEUED ? 0.45 : 1);
            badge.getStyleClass().removeAll("exported", "failed");
            if (s == ItemStatus.EXPORTED) {
                badge.setText("✓");
                badge.getStyleClass().add("exported");
                badge.setVisible(true);
            } else if (s == ItemStatus.FAILED) {
                badge.setText("!");
                badge.getStyleClass().add("failed");
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
            }
            status.setText(s.getLabel());
        };
        item.statusProperty().addListener((o, oldValue, newValue) -> update.run());
        update.run();
        return tile;
    }
}

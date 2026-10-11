package se233.project1.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Start screen: a large drop target for images or a ZIP archive. */
public class DropZoneView extends StackPane {
    private final VBox card;
    private final Label warning = new Label();

    public DropZoneView() {
        getStyleClass().add("drop-zone-root");
        Label icon = new Label("⬇");
        icon.getStyleClass().add("drop-icon");
        Label title = new Label("Drag file(s) here");
        title.getStyleClass().add("drop-title");
        Label hint = new Label("A JPG or PNG image, several images at once, or a single ZIP archive of images");
        hint.getStyleClass().add("drop-hint");
        warning.getStyleClass().add("warning-banner");
        warning.setWrapText(true);
        warning.setMaxWidth(520);
        warning.setVisible(false);
        warning.setManaged(false);

        card = new VBox(14, icon, title, hint, warning);
        card.getStyleClass().add("drop-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(640, 400);
        getChildren().add(card);
    }

    public void setDragHover(boolean hover) {
        card.getStyleClass().remove("hover");
        if (hover) {
            card.getStyleClass().add("hover");
        }
    }

    public void showWarning(String text) {
        warning.setText(text);
        warning.setVisible(true);
        warning.setManaged(true);
    }
}

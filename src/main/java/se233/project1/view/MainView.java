package se233.project1.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/** Root node switching between the drop zone and the editor, with a hint shown while dragging files. */
public class MainView extends StackPane {
    private final DropZoneView dropZone = new DropZoneView();
    private final EditorView editor = new EditorView();
    private final Label dropHint = new Label("Drop to add images to the batch");

    public MainView() {
        getStyleClass().add("main-view");
        dropHint.getStyleClass().add("drop-hint-overlay");
        dropHint.setMouseTransparent(true);
        dropHint.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        dropHint.setAlignment(Pos.CENTER);
        dropHint.setVisible(false);
        getChildren().addAll(dropZone, dropHint);
    }

    public DropZoneView getDropZone() { return dropZone; }
    public EditorView getEditor() { return editor; }

    public boolean isEditorShown() { return getChildren().contains(editor); }

    public void showEditor() {
        if (!isEditorShown()) getChildren().setAll(editor, dropHint);
    }

    public void showDropZone() {
        if (isEditorShown()) getChildren().setAll(dropZone, dropHint);
    }

    public void setDragHover(boolean hover) {
        if (isEditorShown()) dropHint.setVisible(hover);
        else dropZone.setDragHover(hover);
    }
}

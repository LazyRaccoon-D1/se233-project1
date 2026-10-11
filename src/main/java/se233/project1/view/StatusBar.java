package se233.project1.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Progress bar and status text for batch jobs (preprocessing and export). */
public class StatusBar extends HBox {
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label message = new Label("Ready");
    private final Label threads = new Label();
    private final Button cancelButton = new Button("Cancel");

    public StatusBar() {
        getStyleClass().add("status-bar");
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        progressBar.setPrefWidth(260);
        message.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(message, Priority.ALWAYS);
        threads.getStyleClass().add("thread-info");
        cancelButton.getStyleClass().add("small-button");
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        getChildren().addAll(progressBar, message, threads, cancelButton);
        setIdle("Ready");
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public Label getMessage() { return message; }
    public Button getCancelButton() { return cancelButton; }

    public void setThreadInfo(String text) { threads.setText(text); }

    public void setRunning(boolean running) {
        cancelButton.setVisible(running);
        cancelButton.setManaged(running);
        progressBar.setVisible(true);
    }

    public void setIdle(String text) {
        message.textProperty().unbind();
        progressBar.progressProperty().unbind();
        message.setText(text);
        threads.setText("");
        setRunning(false);
    }
}

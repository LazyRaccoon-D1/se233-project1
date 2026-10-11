package se233.project1.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/** Informative message boxes shown to the user. */
public class Dialogs {

    public static void error(Window owner, String header, String content) {
        show(owner, Alert.AlertType.ERROR, header, content);
    }

    public static void warning(Window owner, String header, String content) {
        show(owner, Alert.AlertType.WARNING, header, content);
    }

    public static void info(Window owner, String header, String content) {
        show(owner, Alert.AlertType.INFORMATION, header, content);
    }

    /** A message followed by a list of details, one per line (e.g. the files that failed). */
    public static void withDetails(Window owner, Alert.AlertType type, String header, String content,
                                   List<String> details) {
        String text = content;
        for (String line : details) {
            text = text + "\n  - " + line;
        }
        show(owner, type, header, text);
    }

    /** Asks a yes/no question; returns true when the user clicks OK. */
    public static boolean confirm(Window owner, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Confirm");
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Window owner, Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : type == Alert.AlertType.WARNING ? "Warning" : "Information");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setMinWidth(460);
        alert.showAndWait();
    }
}

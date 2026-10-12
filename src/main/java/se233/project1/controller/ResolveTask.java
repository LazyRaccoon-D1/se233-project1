package se233.project1.controller;

import javafx.concurrent.Task;

import java.io.File;
import java.util.List;

/**
 * Checks the dropped files and extracts a ZIP archive on a background thread.
 * The result is null when the files are not accepted; getErrorMessage() then says why.
 */
public class ResolveTask extends Task<InputResolver.Resolution> {
    private final List<File> files;
    private String errorMessage;

    public ResolveTask(List<File> files) {
        this.files = files;
    }

    @Override
    protected InputResolver.Resolution call() {
        try {
            return InputResolver.resolve(files);
        } catch (Exception e) {
            System.err.println("Rejected dropped files: " + e.getMessage());
            errorMessage = e.getMessage();
            return null;
        }
    }

    public String getErrorMessage() { return errorMessage; }
}

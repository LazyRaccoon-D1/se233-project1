package se233.project1.exception;

import java.io.IOException;

/** Thrown when the dropped files are not a supported combination (images, or a single ZIP archive). */
public class InvalidInputException extends IOException {
    public InvalidInputException(String message) {
        super(message);
    }
}

package se233.project1.exception;

import java.io.IOException;

/** Thrown when an image file cannot be decoded. */
public class CorruptedImageException extends IOException {
    public CorruptedImageException(String message) {
        super(message);
    }
}

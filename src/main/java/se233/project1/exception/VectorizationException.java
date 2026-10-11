package se233.project1.exception;

import java.io.IOException;

/** Thrown when tracing an image fails (Potrace error or unreadable output). */
public class VectorizationException extends IOException {
    public VectorizationException(String message) {
        super(message);
    }
}

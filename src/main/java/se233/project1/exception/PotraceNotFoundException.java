package se233.project1.exception;

import java.io.IOException;

/** Thrown when the Potrace command-line tool cannot be found. */
public class PotraceNotFoundException extends IOException {
    public PotraceNotFoundException() {
        super("The Potrace command-line tool could not be found.\n\n"
                + "Install it and restart the application:\n"
                + "  - macOS: brew install potrace\n"
                + "  - Linux: sudo apt install potrace\n"
                + "  - Windows: download potrace from https://potrace.sourceforge.net and put potrace.exe "
                + "on your PATH or in a \"tools\" folder next to the project.\n\n"
                + "You can also point the app to it with the POTRACE_PATH environment variable.");
    }
}

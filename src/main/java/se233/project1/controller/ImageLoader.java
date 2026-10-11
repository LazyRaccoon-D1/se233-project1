package se233.project1.controller;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import se233.project1.exception.CorruptedImageException;

import java.io.File;

/** Decodes JPG/PNG files with JavaFX and reads their pixels. */
public class ImageLoader {
    /** Very large images are scaled down on load to keep memory use reasonable. */
    static final int MAX_SIDE = 4000;

    public static Image load(File file) throws CorruptedImageException {
        if (!file.canRead()) {
            throw new CorruptedImageException("\"" + file.getName() + "\" cannot be read (missing file or no permission).");
        }
        String url = file.toURI().toString();
        Image image = new Image(url);
        checkLoaded(image, file);
        if (image.getWidth() > MAX_SIDE || image.getHeight() > MAX_SIDE) {
            image = new Image(url, MAX_SIDE, MAX_SIDE, true, true);
            checkLoaded(image, file);
        }
        return image;
    }

    private static void checkLoaded(Image image, File file) throws CorruptedImageException {
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            String reason = image.getException() == null ? "" : " (" + image.getException().getMessage() + ")";
            throw new CorruptedImageException("\"" + file.getName() + "\" is corrupted or not a valid JPG/PNG image" + reason + ".");
        }
    }

    /** Reads all pixels as ARGB integers, row by row. */
    public static int[] readPixels(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = reader.getArgb(x, y);
            }
        }
        return pixels;
    }
}

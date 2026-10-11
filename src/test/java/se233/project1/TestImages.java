package se233.project1;

import javafx.application.Platform;

import java.io.File;

/** Test fixtures: synthetic pixel arrays and the files in src/test/resources. */
public class TestImages {
    public static final String IMAGES = "src/test/resources/images/";
    public static final String INPUTS = "src/test/resources/inputs/";

    public static File image(String name) {
        return new File(IMAGES + name);
    }

    public static File input(String name) {
        return new File(INPUTS + name);
    }

    /** Starts the JavaFX runtime once (needed to decode images with javafx.scene.image.Image). */
    public static void initJfxRuntime() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
            // already started by another test class
        }
    }

    /** Vertical stripes of the given colors, each with a slightly noisy copy to imitate JPEG noise. */
    public static int[] stripes(int[] colors, int width, int height) {
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = colors[x * colors.length / width];
                int noise = (x + y) % 3 - 1;
                int r = clamp(((color >> 16) & 0xFF) + noise);
                int g = clamp(((color >> 8) & 0xFF) + noise);
                int b = clamp((color & 0xFF) + noise);
                pixels[y * width + x] = 0xFF000000 | r << 16 | g << 8 | b;
            }
        }
        return pixels;
    }

    /** A white square with a filled orange rectangle and a blue rectangle inside it (3 colors). */
    public static int[] logo(int size) {
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = 0xFFFFFFFF;
                if (x > size / 5 && x < size * 3 / 5 && y > size / 5 && y < size * 3 / 5) {
                    color = 0xFFFF6F00;
                }
                if (x > size * 2 / 3 && x < size * 9 / 10 && y > size * 2 / 3 && y < size * 9 / 10) {
                    color = 0xFF1E3A8A;
                }
                pixels[y * size + x] = color;
            }
        }
        return pixels;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

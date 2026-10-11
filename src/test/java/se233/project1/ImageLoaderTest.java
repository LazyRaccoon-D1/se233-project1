package se233.project1;

import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se233.project1.controller.ColorAnalyzer;
import se233.project1.controller.ImageLoader;
import se233.project1.exception.CorruptedImageException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImageLoaderTest {
    @BeforeAll
    public static void initJfxRuntime() {
        TestImages.initJfxRuntime();
    }

    @Test
    public void load_givenPng_thenImageWithPixels() throws Exception {
        Image image = ImageLoader.load(TestImages.image("logo-3colors.png"));
        assertEquals(500, (int) image.getWidth());
        assertEquals(500 * 500, ImageLoader.readPixels(image).length);
    }

    @Test
    public void load_givenJpg_thenJpegNoiseStillGivesThreeColors() throws Exception {
        Image image = ImageLoader.load(TestImages.image("logo-3colors.jpg"));
        assertEquals(3, ColorAnalyzer.analyze(ImageLoader.readPixels(image)).getDetectedColors());
    }

    @Test
    public void load_givenTransparentPng_thenTransparencyDetected() throws Exception {
        Image image = ImageLoader.load(TestImages.image("heart-transparent.png"));
        assertTrue(ColorAnalyzer.analyze(ImageLoader.readPixels(image)).hasTransparency());
    }

    @Test
    public void load_givenCorruptedFile_thenCorruptedImageException() {
        assertThrows(CorruptedImageException.class, () -> ImageLoader.load(TestImages.image("corrupted.png")));
    }

    @Test
    public void load_givenMissingFile_thenCorruptedImageException() {
        assertThrows(CorruptedImageException.class, () -> ImageLoader.load(TestImages.image("missing.png")));
    }
}

package se233.project1;

import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se233.project1.controller.ColorAnalyzer;
import se233.project1.controller.ImageLoader;
import se233.project1.controller.PotraceRunner;
import se233.project1.controller.Vectorizer;
import se233.project1.model.ColorAnalysis;
import se233.project1.model.ColorMode;
import se233.project1.model.DetailLevel;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end tests with the real Potrace tool (it must be installed). */
public class VectorizerTest {
    private static final File OUTPUT = new File("target/test-svg");
    private final Vectorizer vectorizer = new Vectorizer(new PotraceRunner());

    @BeforeAll
    public static void setup() {
        TestImages.initJfxRuntime();
        OUTPUT.mkdirs();
    }

    private VectorResult trace(String imageName, VectorSettings settings, String outName) throws Exception {
        Image image = ImageLoader.load(TestImages.image(imageName));
        int[] pixels = ImageLoader.readPixels(image);
        ColorAnalysis analysis = ColorAnalyzer.analyze(pixels);
        VectorSettings used = settings == null ? VectorSettings.defaults(analysis) : settings.adaptTo(analysis);
        VectorResult result = vectorizer.vectorize(pixels, (int) image.getWidth(), (int) image.getHeight(), analysis,
                used, Vectorizer.EXPORT_MAX_SIDE);
        try (FileOutputStream out = new FileOutputStream(new File(OUTPUT, outName))) {
            out.write(result.toSvg().getBytes());
        }
        return result;
    }

    private static boolean isWhitish(int rgb) {
        return ((rgb >> 16) & 0xFF) > 230 && ((rgb >> 8) & 0xFF) > 230 && (rgb & 0xFF) > 230;
    }

    @Test
    public void vectorize_givenPng_thenValidSvgWithTwoLayers() throws Exception {
        VectorResult result = trace("logo-3colors.png", null, "logo-png.svg");
        String svg = result.toSvg();
        assertTrue(svg.startsWith("<?xml"));
        assertTrue(svg.contains("<svg") && svg.trim().endsWith("</svg>"));
        assertEquals(500, result.getWidth());
        assertEquals(2, result.getLayers().size(), "default is 2 colors");
    }

    @Test
    public void vectorize_givenJpg_thenValidSvgWithTwoLayers() throws Exception {
        VectorResult result = trace("logo-3colors.jpg", null, "logo-jpg.svg");
        assertTrue(result.toSvg().contains("<path"));
        assertEquals(2, result.getLayers().size());
    }

    @Test
    public void vectorize_givenThreeCustomColors_thenThreeLayers() throws Exception {
        VectorSettings three = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 3, false);
        assertEquals(3, trace("logo-3colors.png", three, "logo-3colors.svg").getLayers().size());
    }

    @Test
    public void vectorize_givenRemoveBackground_thenWhiteBackgroundLayerRemoved() throws Exception {
        VectorSettings keep = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 3, false);
        VectorResult opaque = trace("logo-3colors.png", keep, "logo-opaque.svg");
        VectorResult transparent = trace("logo-3colors.png", keep.withRemoveBackground(true), "logo-transparent.svg");
        assertTrue(isWhitish(opaque.getLayers().get(0).getRgb()), "bottom layer is the white background");
        assertFalse(isWhitish(transparent.getLayers().get(0).getRgb()), "background removed");
        int last = transparent.getLayers().size() - 1;
        assertTrue(isWhitish(transparent.getLayers().get(last).getRgb()), "the white hole inside the ring is kept");
    }

    @Test
    public void vectorize_givenHighDetail_thenMorePathDataThanLowDetail() throws Exception {
        VectorSettings base = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 3, false);
        int low = trace("logo-3colors.png", base.withDetail(DetailLevel.LOW), "logo-low.svg").getSvgSizeBytes();
        int high = trace("logo-3colors.png", base.withDetail(DetailLevel.HIGH), "logo-high.svg").getSvgSizeBytes();
        assertTrue(high > low);
    }

    @Test
    public void vectorize_givenUnlimitedColors_thenAtLeastThreeLayers() throws Exception {
        VectorSettings unlimited = new VectorSettings(DetailLevel.MEDIUM, ColorMode.UNLIMITED, 2, false);
        assertTrue(trace("color-badge.png", unlimited, "badge-unlimited.svg").getLayers().size() >= 7);
    }
}

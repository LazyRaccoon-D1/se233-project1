package se233.project1;

import org.junit.jupiter.api.Test;
import se233.project1.controller.ColorAnalyzer;
import se233.project1.model.ColorAnalysis;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColorAnalyzerTest {
    @Test
    public void analyze_givenThreeColorLogo_thenThreeColorsDetected() {
        ColorAnalysis analysis = ColorAnalyzer.analyze(TestImages.logo(200));
        assertEquals(3, analysis.getDetectedColors());
        assertEquals(0xFFFFFF, analysis.getDominantColors().get(0), "white covers most of the image");
    }

    @Test
    public void analyze_givenNoisyStripes_thenNoiseIsNotCountedAsColors() {
        int[] colors = {0xE53935, 0x1E88E5};
        assertEquals(2, ColorAnalyzer.analyze(TestImages.stripes(colors, 200, 50)).getDetectedColors());
    }

    @Test
    public void analyze_givenSevenColors_thenCustomLimitCappedAtFive() {
        int[] colors = {0xE53935, 0x43A047, 0x1E88E5, 0xFDD835, 0x8E24AA, 0x000000, 0xFFFFFF};
        ColorAnalysis analysis = ColorAnalyzer.analyze(TestImages.stripes(colors, 700, 50));
        assertEquals(7, analysis.getDetectedColors());
        assertEquals(5, analysis.getCustomColorLimit());
        assertTrue(analysis.getPaletteColors().size() >= 7);
    }

    @Test
    public void analyze_givenFullyTransparentImage_thenNoColors() {
        ColorAnalysis analysis = ColorAnalyzer.analyze(new int[400]);
        assertEquals(0, analysis.getDetectedColors());
        assertTrue(analysis.hasTransparency());
        assertEquals(1, analysis.getCustomColorLimit());
    }

    @Test
    public void quantize_givenTwoSeeds_thenEveryPixelGetsOneOfTwoLabels() {
        int[] pixels = TestImages.logo(100);
        ColorAnalysis analysis = ColorAnalyzer.analyze(pixels);
        ColorAnalyzer.Quantization q = ColorAnalyzer.quantize(pixels, analysis.getDominantColors().subList(0, 2));
        assertEquals(2, q.getPalette().length);
        assertTrue(Arrays.stream(q.getLabels()).allMatch(label -> label == 0 || label == 1));
    }
}

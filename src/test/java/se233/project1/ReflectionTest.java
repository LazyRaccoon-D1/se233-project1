package se233.project1;

import org.junit.jupiter.api.Test;
import se233.project1.controller.BatchTask;
import se233.project1.controller.ColorAnalyzer;
import se233.project1.model.ColorAnalysis;
import se233.project1.model.ColorMode;
import se233.project1.model.DetailLevel;
import se233.project1.model.ImageItem;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests of private state and private methods using reflection (Chapter 5 technique). */
public class ReflectionTest {
    @Test
    public void constructor_givenParallelFalse_thenPrivateFlagIsFalse() throws Exception {
        BatchTask task = new BatchTask("Testing", new ArrayList<>(), new ArrayList<>(), false);
        assertFalse((Boolean) ReflectionHelper.getField(task, "parallel"));
    }

    @Test
    public void cachePreview_givenSameSettingsTwice_thenOneCacheEntry() throws Exception {
        ImageItem item = new ImageItem(new File("logo.png"));
        VectorSettings settings = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);
        VectorResult result = new VectorResult(10, 10, 10, 10, 0, 10, 0.1, -0.1, new ArrayList<>(), settings, 5);
        item.cachePreview(result);
        item.cachePreview(result);
        Map<?, ?> cache = (Map<?, ?>) ReflectionHelper.getField(item, "previewCache");
        assertEquals(1, cache.size());
    }

    @Test
    public void setField_givenSettings_thenItemIsLoaded() throws Exception {
        ImageItem item = new ImageItem(new File("logo.png"));
        assertFalse(item.isLoaded());
        ReflectionHelper.setField(item, "pixels", new int[4]);
        ReflectionHelper.setField(item, "analysis", new ColorAnalysis(1, new ArrayList<>(), new ArrayList<>(), false));
        ReflectionHelper.setField(item, "settings", new VectorSettings(DetailLevel.LOW, ColorMode.CUSTOM, 1, false));
        assertTrue(item.isLoaded());
    }

    @Test
    public void nearest_givenRedPixel_thenRedPaletteEntry() throws Exception {
        double[][] centers = {{0, 0, 0}, {255, 0, 0}, {0, 0, 255}};
        Object index = ReflectionHelper.invokeStaticMethod(ColorAnalyzer.class, "nearest",
                new Class<?>[]{double[][].class, int.class, int.class, int.class}, centers, 250, 10, 10);
        assertEquals(1, index);
    }
}

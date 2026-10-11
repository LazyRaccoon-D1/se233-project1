package se233.project1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se233.project1.controller.ColorAnalyzer;
import se233.project1.controller.PotraceRunner;
import se233.project1.controller.Vectorizer;
import se233.project1.model.ColorAnalysis;
import se233.project1.model.ColorMode;
import se233.project1.model.DetailLevel;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Tests the color layering of the Vectorizer in isolation: Potrace is replaced by a mock object. */
public class VectorizerMockTest {
    private PotraceRunner potrace;
    private Vectorizer vectorizer;
    private int[] pixels;
    private ColorAnalysis analysis;

    @BeforeEach
    public void setUp() throws Exception {
        potrace = Mockito.mock(PotraceRunner.class);
        Mockito.when(potrace.trace(any(byte[].class), any(DetailLevel.class)))
                .thenReturn(new PotraceRunner.Output("M0 0L10 0L10 10z", 0, 100, 0.1, -0.1));
        vectorizer = new Vectorizer(potrace);
        pixels = TestImages.logo(100);
        analysis = ColorAnalyzer.analyze(pixels);
    }

    @Test
    public void vectorize_givenThreeColors_thenPotraceCalledOncePerLayer() throws Exception {
        VectorSettings settings = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 3, false);
        VectorResult result = vectorizer.vectorize(pixels, 100, 100, analysis, settings, 1000);
        assertEquals(3, result.getLayers().size());
        verify(potrace, times(3)).trace(any(byte[].class), any(DetailLevel.class));
    }

    @Test
    public void vectorize_givenRemoveBackground_thenBackgroundLayerNotTraced() throws Exception {
        VectorSettings settings = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 3, true);
        VectorResult result = vectorizer.vectorize(pixels, 100, 100, analysis, settings, 1000);
        assertEquals(2, result.getLayers().size());
        verify(potrace, times(2)).trace(any(byte[].class), any(DetailLevel.class));
    }

    @Test
    public void vectorize_givenLowDetail_thenTracedAtHalfResolution() throws Exception {
        VectorSettings settings = new VectorSettings(DetailLevel.LOW, ColorMode.CUSTOM, 2, false);
        VectorResult result = vectorizer.vectorize(pixels, 100, 100, analysis, settings, 1000);
        assertEquals(2.0, result.getScaleX(), 0.001);
    }
}

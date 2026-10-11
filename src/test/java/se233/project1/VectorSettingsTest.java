package se233.project1;

import org.junit.jupiter.api.Test;
import se233.project1.model.ColorAnalysis;
import se233.project1.model.ColorMode;
import se233.project1.model.DetailLevel;
import se233.project1.model.VectorSettings;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VectorSettingsTest {
    private static ColorAnalysis colors(int n) {
        return new ColorAnalysis(n, new ArrayList<>(), new ArrayList<>(), false);
    }

    @Test
    public void defaults_givenManyColors_thenMediumDetailWithTwoCustomColors() {
        VectorSettings s = VectorSettings.defaults(colors(8));
        assertEquals(DetailLevel.MEDIUM, s.getDetail());
        assertEquals(ColorMode.CUSTOM, s.getColorMode());
        assertEquals(2, s.getCustomColors());
        assertFalse(s.isRemoveBackground());
    }

    @Test
    public void defaults_givenOneDetectedColor_thenOneCustomColor() {
        assertEquals(1, VectorSettings.defaults(colors(1)).getCustomColors());
    }

    @Test
    public void isParallelEligible_givenMediumAndCustom_thenTrue() {
        VectorSettings s = VectorSettings.defaults(colors(5));
        assertTrue(s.isParallelEligible());
        assertTrue(s.withCustomColors(5).withRemoveBackground(true).isParallelEligible());
    }

    @Test
    public void isParallelEligible_givenOtherDetailOrUnlimited_thenFalse() {
        VectorSettings s = VectorSettings.defaults(colors(5));
        assertFalse(s.withDetail(DetailLevel.LOW).isParallelEligible());
        assertFalse(s.withDetail(DetailLevel.HIGH).isParallelEligible());
        assertFalse(s.withColorMode(ColorMode.UNLIMITED).isParallelEligible());
    }

    @Test
    public void constructor_givenSixCustomColors_thenIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 6, false));
        assertEquals("Custom colors must be between 1 and 5", exception.getMessage());
    }

    @Test
    public void getCustomColorLimit_givenNineOrThreeColors_thenCappedAtFive() {
        assertEquals(5, colors(9).getCustomColorLimit());
        assertEquals(3, colors(3).getCustomColorLimit());
    }

    @Test
    public void adaptTo_givenImageWithThreeColors_thenCustomColorsClamped() {
        VectorSettings s = VectorSettings.defaults(colors(9)).withCustomColors(5);
        assertEquals(3, s.adaptTo(colors(3)).getCustomColors());
    }

    @Test
    public void getKey_givenEqualSettings_thenSameKey() {
        VectorSettings a = VectorSettings.defaults(colors(4));
        VectorSettings b = new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);
        assertTrue(a.sameAs(b));
        assertFalse(a.sameAs(b.withRemoveBackground(true)));
    }
}

package se233.project1.model;

/**
 * The vectorization settings of one image. Objects are immutable: every "with" method returns a new object.
 */
public class VectorSettings {
    public static final int MAX_CUSTOM_COLORS = 5;
    public static final int DEFAULT_CUSTOM_COLORS = 2;

    private final DetailLevel detail;
    private final ColorMode colorMode;
    private final int customColors;
    private final boolean removeBackground;

    public VectorSettings(DetailLevel detail, ColorMode colorMode, int customColors, boolean removeBackground) {
        if (customColors < 1 || customColors > MAX_CUSTOM_COLORS) {
            throw new IllegalArgumentException("Custom colors must be between 1 and " + MAX_CUSTOM_COLORS);
        }
        this.detail = detail;
        this.colorMode = colorMode;
        this.customColors = customColors;
        this.removeBackground = removeBackground;
    }

    /** Medium detail, custom colors (2, or fewer if the image has fewer colors), background kept. */
    public static VectorSettings defaults(ColorAnalysis analysis) {
        return new VectorSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM,
                Math.min(DEFAULT_CUSTOM_COLORS, analysis.getCustomColorLimit()), false);
    }

    public DetailLevel getDetail() { return detail; }
    public ColorMode getColorMode() { return colorMode; }
    public int getCustomColors() { return customColors; }
    public boolean isRemoveBackground() { return removeBackground; }

    /** Parallel processing is only allowed for Medium detail with Custom colors (project constraint). */
    public boolean isParallelEligible() {
        return detail == DetailLevel.MEDIUM && colorMode == ColorMode.CUSTOM;
    }

    /** A text key that is identical for identical settings; used to cache previews in a map. */
    public String getKey() {
        return detail + "-" + colorMode + "-" + customColors + "-" + removeBackground;
    }

    public boolean sameAs(VectorSettings other) {
        return other != null && getKey().equals(other.getKey());
    }

    public VectorSettings withDetail(DetailLevel d) { return new VectorSettings(d, colorMode, customColors, removeBackground); }
    public VectorSettings withColorMode(ColorMode m) { return new VectorSettings(detail, m, customColors, removeBackground); }
    public VectorSettings withCustomColors(int n) { return new VectorSettings(detail, colorMode, n, removeBackground); }
    public VectorSettings withRemoveBackground(boolean r) { return new VectorSettings(detail, colorMode, customColors, r); }

    /** Adapts these settings to another image, clamping the custom color count to that image's limit. */
    public VectorSettings adaptTo(ColorAnalysis analysis) {
        return withCustomColors(Math.min(customColors, analysis.getCustomColorLimit()));
    }

    public String describe() {
        String colors;
        if (colorMode == ColorMode.UNLIMITED) {
            colors = "Unlimited colors";
        } else if (customColors == 1) {
            colors = "1 color";
        } else {
            colors = customColors + " colors";
        }
        String text = detail.getLabel() + " detail · " + colors;
        if (removeBackground) {
            text = text + " · Transparent background";
        }
        return text;
    }

    @Override
    public String toString() {
        return describe();
    }
}

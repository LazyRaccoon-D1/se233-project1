package se233.project1.model;

import java.util.List;

/** The result of automatic color detection for one image. */
public class ColorAnalysis {
    private final int detectedColors;
    private final List<Integer> dominantColors;
    private final List<Integer> paletteColors;
    private final boolean transparency;

    /**
     * @param detectedColors number of visually distinct colors found in the image
     * @param dominantColors the distinct colors (RGB), most frequent first
     * @param paletteColors  a finer palette close to the original colors, used by "Unlimited" mode
     * @param transparency   whether the image contains transparent pixels
     */
    public ColorAnalysis(int detectedColors, List<Integer> dominantColors, List<Integer> paletteColors,
                         boolean transparency) {
        this.detectedColors = detectedColors;
        this.dominantColors = dominantColors;
        this.paletteColors = paletteColors;
        this.transparency = transparency;
    }

    public int getDetectedColors() { return detectedColors; }
    public List<Integer> getDominantColors() { return dominantColors; }
    public List<Integer> getPaletteColors() { return paletteColors; }
    public boolean hasTransparency() { return transparency; }

    /** The custom color selection is capped at 5, or fewer when the image has fewer colors. */
    public int getCustomColorLimit() {
        return Math.max(1, Math.min(detectedColors, VectorSettings.MAX_CUSTOM_COLORS));
    }
}

package se233.project1.model;

/**
 * Detail presets. Each level maps to a tracing resolution and to Potrace parameters:
 * turdsize (speckles smaller than this many pixels are dropped), alphamax (corner threshold,
 * higher = smoother curves) and opttolerance (curve optimisation tolerance).
 */
public enum DetailLevel {
    LOW("Low", 0.5, 40, 1.25, 1.0),
    MEDIUM("Medium", 1.0, 8, 1.0, 0.3),
    HIGH("High", 1.0, 1, 0.6, 0.05);

    private final String label;
    private final double traceScale;
    private final int turdSize;
    private final double alphaMax;
    private final double optTolerance;

    DetailLevel(String label, double traceScale, int turdSize, double alphaMax, double optTolerance) {
        this.label = label;
        this.traceScale = traceScale;
        this.turdSize = turdSize;
        this.alphaMax = alphaMax;
        this.optTolerance = optTolerance;
    }

    public String getLabel() { return label; }
    public double getTraceScale() { return traceScale; }
    public int getTurdSize() { return turdSize; }
    public double getAlphaMax() { return alphaMax; }
    public double getOptTolerance() { return optTolerance; }
}

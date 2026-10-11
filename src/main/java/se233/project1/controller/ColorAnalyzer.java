package se233.project1.controller;

import se233.project1.model.ColorAnalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Detects the distinct colors of an image and quantizes images to a reduced palette (k-means).
 */
public final class ColorAnalyzer {
    /** A color must cover at least this share of the image to count as a "detected" color. */
    static final double DETECT_MIN_SHARE = 0.005;
    /** Colors closer than this (RGB distance) are treated as the same color (JPEG noise, anti-aliasing). */
    static final double DETECT_MERGE_DISTANCE = 48;
    /** Finer settings used for the "Unlimited" palette, which tries to keep the original colors. */
    static final double PALETTE_MIN_SHARE = 0.0008;
    static final double PALETTE_MERGE_DISTANCE = 22;
    static final int PALETTE_MAX_COLORS = 24;

    private static final int MAX_ANALYSIS_SAMPLES = 400_000;
    private static final int MAX_KMEANS_SAMPLES = 60_000;
    private static final int KMEANS_ITERATIONS = 8;

    private ColorAnalyzer() {
    }

    /** Pixels covering a color: a running mean of the merged histogram bins. */
    private static final class Cluster {
        long r, g, b, count;

        Cluster(long r, long g, long b, long count) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.count = count;
        }

        double distanceTo(double cr, double cg, double cb) {
            double dr = r / (double) count - cr, dg = g / (double) count - cg, db = b / (double) count - cb;
            return Math.sqrt(dr * dr + dg * dg + db * db);
        }

        int rgb() {
            return (int) Math.round(r / (double) count) << 16
                    | (int) Math.round(g / (double) count) << 8
                    | (int) Math.round(b / (double) count);
        }
    }

    /** Detects the colors of an image given as ARGB pixels. */
    public static ColorAnalysis analyze(int[] argb) {
        int step = Math.max(1, argb.length / MAX_ANALYSIS_SAMPLES);

        // 5 bits per channel histogram, keeping the exact sums so each bin has a precise mean color
        long[] count = new long[1 << 15];
        long[] sumR = new long[1 << 15], sumG = new long[1 << 15], sumB = new long[1 << 15];
        long total = 0;
        boolean transparency = false;
        for (int i = 0; i < argb.length; i += step) {
            int p = argb[i];
            int a = p >>> 24;
            if (a < 255) transparency = true;
            if (a < 128) continue;
            int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
            int key = (r >> 3) << 10 | (g >> 3) << 5 | (b >> 3);
            count[key]++;
            sumR[key] += r;
            sumG[key] += g;
            sumB[key] += b;
            total++;
        }
        if (total == 0) {
            return new ColorAnalysis(0, new ArrayList<>(), new ArrayList<>(), true);
        }

        // Bins sorted by frequency, most frequent first (count in the high bits, key in the low 15 bits)
        int used = 0;
        long[] sorted = new long[1 << 15];
        for (int key = 0; key < count.length; key++) {
            if (count[key] > 0) sorted[used++] = count[key] << 15 | key;
        }
        long[] bins = Arrays.copyOf(sorted, used);
        Arrays.sort(bins);

        List<Integer> dominant = cluster(bins, count, sumR, sumG, sumB, total,
                DETECT_MIN_SHARE, DETECT_MERGE_DISTANCE, Integer.MAX_VALUE);
        List<Integer> palette = cluster(bins, count, sumR, sumG, sumB, total,
                PALETTE_MIN_SHARE, PALETTE_MERGE_DISTANCE, PALETTE_MAX_COLORS);
        return new ColorAnalysis(dominant.size(), dominant, palette, transparency);
    }

    /**
     * Greedy clustering of histogram bins: a frequent bin far from every existing cluster starts a new
     * color, anything else is merged into its nearest cluster.
     */
    private static List<Integer> cluster(long[] binsAscending, long[] count, long[] sumR, long[] sumG, long[] sumB,
                                         long total, double minShare, double mergeDistance, int maxColors) {
        List<Cluster> clusters = new ArrayList<>();
        double minCount = minShare * total;
        for (int i = binsAscending.length - 1; i >= 0; i--) {
            int key = (int) (binsAscending[i] & 0x7FFF);
            long c = count[key];
            double mr = sumR[key] / (double) c, mg = sumG[key] / (double) c, mb = sumB[key] / (double) c;
            Cluster nearest = null;
            double best = Double.MAX_VALUE;
            for (Cluster cl : clusters) {
                double d = cl.distanceTo(mr, mg, mb);
                if (d < best) {
                    best = d;
                    nearest = cl;
                }
            }
            boolean startNew = nearest == null
                    || (best > mergeDistance && c >= minCount && clusters.size() < maxColors);
            if (startNew) {
                clusters.add(new Cluster(sumR[key], sumG[key], sumB[key], c));
            } else {
                nearest.r += sumR[key];
                nearest.g += sumG[key];
                nearest.b += sumB[key];
                nearest.count += c;
            }
        }
        clusters.sort((c1, c2) -> Long.compare(c2.count, c1.count));
        List<Integer> colors = new ArrayList<>();
        for (Cluster cl : clusters) {
            colors.add(cl.rgb());
        }
        return colors;
    }

    /** Result of quantization: a palette index for every pixel (-1 = transparent) and the palette colors. */
    public static class Quantization {
        private final int[] labels;
        private final int[] palette;

        public Quantization(int[] labels, int[] palette) {
            this.labels = labels;
            this.palette = palette;
        }

        public int[] getLabels() { return labels; }
        public int[] getPalette() { return palette; }
    }

    /** Maps every pixel to one of {@code seeds.size()} colors, refining the seed colors with k-means. */
    public static Quantization quantize(int[] argb, List<Integer> seeds) {
        int k = seeds.size();
        int[] labels = new int[argb.length];
        if (k == 0) {
            Arrays.fill(labels, -1);
            return new Quantization(labels, new int[0]);
        }
        double[][] centers = new double[k][3];
        for (int i = 0; i < k; i++) {
            int c = seeds.get(i);
            centers[i][0] = (c >> 16) & 0xFF;
            centers[i][1] = (c >> 8) & 0xFF;
            centers[i][2] = c & 0xFF;
        }

        // k-means refinement on a subsample of the opaque pixels
        int step = Math.max(1, argb.length / MAX_KMEANS_SAMPLES);
        int[] samples = new int[argb.length / step + 1];
        int n = 0;
        for (int i = 0; i < argb.length; i += step) {
            if ((argb[i] >>> 24) >= 128) samples[n++] = argb[i];
        }
        for (int iter = 0; iter < KMEANS_ITERATIONS && n > 0; iter++) {
            double[][] sums = new double[k][3];
            long[] counts = new long[k];
            for (int s = 0; s < n; s++) {
                int p = samples[s];
                int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
                int best = nearest(centers, r, g, b);
                sums[best][0] += r;
                sums[best][1] += g;
                sums[best][2] += b;
                counts[best]++;
            }
            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    centers[i][0] = sums[i][0] / counts[i];
                    centers[i][1] = sums[i][1] / counts[i];
                    centers[i][2] = sums[i][2] / counts[i];
                }
            }
        }

        // 6 bits per channel lookup table makes labelling every pixel cheap
        byte[] lut = new byte[1 << 18];
        for (int key = 0; key < lut.length; key++) {
            int r = ((key >> 12) << 2) + 2, g = (((key >> 6) & 0x3F) << 2) + 2, b = ((key & 0x3F) << 2) + 2;
            lut[key] = (byte) nearest(centers, r, g, b);
        }
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            if ((p >>> 24) < 128) {
                labels[i] = -1;
            } else {
                int key = ((p >> 18) & 0x3F) << 12 | ((p >> 10) & 0x3F) << 6 | ((p >> 2) & 0x3F);
                labels[i] = lut[key];
            }
        }
        int[] palette = new int[k];
        for (int i = 0; i < k; i++) {
            palette[i] = clamp(centers[i][0]) << 16 | clamp(centers[i][1]) << 8 | clamp(centers[i][2]);
        }
        return new Quantization(labels, palette);
    }

    private static int nearest(double[][] centers, int r, int g, int b) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < centers.length; i++) {
            double dr = centers[i][0] - r, dg = centers[i][1] - g, db = centers[i][2] - b;
            double d = dr * dr + dg * dg + db * db;
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}

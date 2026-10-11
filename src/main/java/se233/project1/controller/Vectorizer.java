package se233.project1.controller;

import se233.project1.model.ColorAnalysis;
import se233.project1.model.ColorMode;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Color raster-to-vector conversion on top of Potrace (which only traces black-and-white bitmaps):
 * <ol>
 *     <li>quantize the image to the requested palette,</li>
 *     <li>trace one bitmap per color as stacked layers: layer i covers its own pixels and those of
 *     every layer above it, so neighbouring shapes overlap instead of leaving hairline gaps,</li>
 *     <li>combine the layers into a single SVG.</li>
 * </ol>
 */
public class Vectorizer {
    /** Longest side traced for on-screen previews (kept small so previews feel instant). */
    public static final int PREVIEW_MAX_SIDE = 1000;
    /** Longest side traced when exporting. */
    public static final int EXPORT_MAX_SIDE = 4000;

    private final PotraceRunner potrace;

    public Vectorizer(PotraceRunner potrace) {
        this.potrace = potrace;
    }

    /**
     * Vectorizes an image given as ARGB pixels.
     *
     * @param maxSide longest side of the bitmap that is traced (previews use a smaller size than exports)
     */
    public VectorResult vectorize(int[] argb, int width, int height, ColorAnalysis analysis, VectorSettings settings,
                                  int maxSide) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        double scale = Math.min(1.0, maxSide / (double) Math.max(width, height)) * settings.getDetail().getTraceScale();
        int tw = Math.max(1, (int) Math.round(width * scale));
        int th = Math.max(1, (int) Math.round(height * scale));
        int[] work = (tw == width && th == height) ? argb : resize(argb, width, height, tw, th);

        List<Integer> seeds;
        if (settings.getColorMode() == ColorMode.CUSTOM) {
            List<Integer> dominant = analysis.getDominantColors();
            seeds = dominant.subList(0, Math.min(settings.getCustomColors(), dominant.size()));
        } else {
            seeds = analysis.getPaletteColors();
        }
        ColorAnalyzer.Quantization quantization = ColorAnalyzer.quantize(work, seeds);
        int[] labels = quantization.getLabels();
        int colors = quantization.getPalette().length;

        int background = detectBackground(labels, tw, th, colors);
        if (settings.isRemoveBackground() && background >= 0) {
            removeConnectedBackground(labels, tw, th, background);
        }
        List<Integer> order = layerOrder(labels, colors, background, settings.isRemoveBackground());
        int[] rank = new int[colors];
        for (int i = 0; i < order.size(); i++) {
            rank[order.get(i)] = i;
        }

        List<VectorResult.Layer> layers = new ArrayList<>();
        PotraceRunner.Output transform = null;
        for (int position = 0; position < order.size(); position++) {
            byte[] pbm = toPbm(labels, rank, position, tw, th);
            if (pbm == null) {
                continue;
            }
            PotraceRunner.Output out = potrace.trace(pbm, settings.getDetail());
            transform = out;
            if (!out.getPathData().isBlank()) {
                layers.add(new VectorResult.Layer(quantization.getPalette()[order.get(position)], out.getPathData()));
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        if (transform == null) {
            // nothing to trace (e.g. a fully transparent image): an empty but valid SVG
            return new VectorResult(width, height, tw, th, 0, th, 0.1, -0.1, layers, settings, elapsed);
        }
        return new VectorResult(width, height, tw, th, transform.getTx(), transform.getTy(), transform.getSx(),
                transform.getSy(), layers, settings, elapsed);
    }

    /** The most common color on the image border, or -1 when the border is mostly transparent. */
    static int detectBackground(int[] labels, int w, int h, int colors) {
        if (colors == 0) return -1;
        int[] counts = new int[colors];
        int transparent = 0, total = 0;
        for (int x = 0; x < w; x++) {
            for (int y : new int[]{0, h - 1}) {
                int l = labels[y * w + x];
                total++;
                if (l < 0) transparent++;
                else counts[l]++;
            }
        }
        for (int y = 1; y < h - 1; y++) {
            for (int x : new int[]{0, w - 1}) {
                int l = labels[y * w + x];
                total++;
                if (l < 0) transparent++;
                else counts[l]++;
            }
        }
        if (transparent * 2 > total) return -1;
        int best = 0;
        for (int i = 1; i < colors; i++) if (counts[i] > counts[best]) best = i;
        return best;
    }

    /**
     * Flood-fills the background color from the image border and makes it transparent. Background-colored
     * areas enclosed by the artwork (e.g. the inside of an "O") are kept.
     */
    static void removeConnectedBackground(int[] labels, int w, int h, int background) {
        int[] queue = new int[w * h];
        int head = 0, tail = 0;
        for (int x = 0; x < w; x++) {
            tail = seed(labels, queue, tail, x, background);
            tail = seed(labels, queue, tail, (h - 1) * w + x, background);
        }
        for (int y = 0; y < h; y++) {
            tail = seed(labels, queue, tail, y * w, background);
            tail = seed(labels, queue, tail, y * w + w - 1, background);
        }
        while (head < tail) {
            int i = queue[head++];
            int x = i % w, y = i / w;
            if (x > 0) tail = seed(labels, queue, tail, i - 1, background);
            if (x < w - 1) tail = seed(labels, queue, tail, i + 1, background);
            if (y > 0) tail = seed(labels, queue, tail, i - w, background);
            if (y < h - 1) tail = seed(labels, queue, tail, i + w, background);
        }
    }

    private static int seed(int[] labels, int[] queue, int tail, int index, int background) {
        if (labels[index] == background) {
            labels[index] = -1;
            queue[tail++] = index;
        }
        return tail;
    }

    /**
     * Bottom-to-top drawing order: the background first, then colors by decreasing area so small details
     * end up on top. When the background was removed, the remaining (enclosed) background areas go on top
     * instead, so the outer silhouette is defined by the artwork colors and no background halo remains.
     */
    static List<Integer> layerOrder(int[] labels, int colors, int background, boolean backgroundRemoved) {
        long[] counts = new long[colors];
        for (int l : labels) if (l >= 0) counts[l]++;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < colors; i++) if (counts[i] > 0 && i != background) order.add(i);
        order.sort((a, b) -> Long.compare(counts[b], counts[a]));
        if (background >= 0 && counts[background] > 0) {
            if (backgroundRemoved) {
                order.add(background);
            } else {
                order.add(0, background);
            }
        }
        return order;
    }

    /** Binary PBM (P4) of the pixels belonging to layer {@code position} or above; null if empty. */
    static byte[] toPbm(int[] labels, int[] rank, int position, int w, int h) {
        byte[] header = ("P4\n" + w + " " + h + "\n").getBytes();
        int rowBytes = (w + 7) / 8;
        byte[] pbm = new byte[header.length + rowBytes * h];
        System.arraycopy(header, 0, pbm, 0, header.length);
        boolean any = false;
        for (int y = 0; y < h; y++) {
            int rowStart = header.length + y * rowBytes;
            for (int x = 0; x < w; x++) {
                int l = labels[y * w + x];
                if (l >= 0 && rank[l] >= position) {
                    pbm[rowStart + (x >> 3)] |= (byte) (0x80 >> (x & 7));
                    any = true;
                }
            }
        }
        return any ? pbm : null;
    }

    /** Downscales ARGB pixels by averaging each block of source pixels (keeps thin lines visible). */
    static int[] resize(int[] argb, int width, int height, int targetW, int targetH) {
        int[] result = new int[targetW * targetH];
        for (int ty = 0; ty < targetH; ty++) {
            int y0 = ty * height / targetH;
            int y1 = Math.max(y0 + 1, (ty + 1) * height / targetH);
            for (int tx = 0; tx < targetW; tx++) {
                int x0 = tx * width / targetW;
                int x1 = Math.max(x0 + 1, (tx + 1) * width / targetW);
                long a = 0, r = 0, g = 0, b = 0;
                int n = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int p = argb[y * width + x];
                        a += p >>> 24;
                        r += (p >> 16) & 0xFF;
                        g += (p >> 8) & 0xFF;
                        b += p & 0xFF;
                        n++;
                    }
                }
                result[ty * targetW + tx] = (int) (a / n) << 24 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            }
        }
        return result;
    }
}

package se233.project1.controller;

import se233.project1.exception.PotraceNotFoundException;
import se233.project1.exception.VectorizationException;
import se233.project1.model.DetailLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Java wrapper around the Potrace command-line tool. Each call traces one black-and-white bitmap (PBM)
 * in an external process and returns the SVG path data.
 */
public class PotraceRunner {
    private static File executable;

    /** The path data of one traced bitmap plus Potrace's coordinate transform. */
    public static class Output {
        private final String pathData;
        private final double tx, ty, sx, sy;

        public Output(String pathData, double tx, double ty, double sx, double sy) {
            this.pathData = pathData;
            this.tx = tx;
            this.ty = ty;
            this.sx = sx;
            this.sy = sy;
        }

        public String getPathData() { return pathData; }
        public double getTx() { return tx; }
        public double getTy() { return ty; }
        public double getSx() { return sx; }
        public double getSy() { return sy; }
    }

    /** Finds the potrace executable: POTRACE_PATH, then the PATH, then common install folders. */
    public static synchronized File locate() throws PotraceNotFoundException {
        if (executable != null) {
            return executable;
        }
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        String name = windows ? "potrace.exe" : "potrace";

        List<File> candidates = new ArrayList<>();
        String override = System.getenv("POTRACE_PATH");
        if (override != null && !override.isBlank()) {
            candidates.add(new File(override));
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                candidates.add(new File(dir, name));
            }
        }
        // IDEs on macOS often start apps without the Homebrew folders on the PATH
        candidates.add(new File("/opt/homebrew/bin", name));
        candidates.add(new File("/usr/local/bin", name));
        candidates.add(new File("/usr/bin", name));
        String workDir = System.getProperty("user.dir");
        candidates.add(new File(workDir + File.separator + "tools", name));
        candidates.add(new File(workDir, name));

        for (File candidate : candidates) {
            if (candidate.isFile() && candidate.canExecute()) {
                executable = candidate;
                System.out.println("Using potrace at " + candidate);
                return candidate;
            }
        }
        throw new PotraceNotFoundException();
    }

    public static boolean isAvailable() {
        try {
            locate();
            return true;
        } catch (PotraceNotFoundException e) {
            return false;
        }
    }

    /**
     * Traces a PBM bitmap. The bitmap is written to a temporary file, Potrace writes the SVG to another
     * temporary file, and both are deleted afterwards. Interrupting the calling thread (for example through
     * ExecutorService.shutdownNow()) stops the Potrace process.
     */
    public Output trace(byte[] pbm, DetailLevel detail) throws IOException, InterruptedException {
        File exe = locate();
        File input = File.createTempFile("se233-layer-", ".pbm");
        File output = File.createTempFile("se233-layer-", ".svg");
        File log = File.createTempFile("se233-potrace-", ".log");
        Process process = null;
        try {
            try (FileOutputStream out = new FileOutputStream(input)) {
                out.write(pbm);
            }
            List<String> command = new ArrayList<>();
            command.add(exe.getAbsolutePath());
            command.add("--svg");
            command.add("--flat");
            command.add("--turdsize");
            command.add(String.valueOf(detail.getTurdSize()));
            command.add("--alphamax");
            command.add(String.valueOf(detail.getAlphaMax()));
            command.add("--opttolerance");
            command.add(String.valueOf(detail.getOptTolerance()));
            command.add("--output");
            command.add(output.getAbsolutePath());
            command.add(input.getAbsolutePath());

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.redirectOutput(log);
            process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new VectorizationException("Potrace failed (exit code " + exitCode + "): " + readFile(log));
            }
            return parse(readFile(output));
        } catch (InterruptedException e) {
            // the batch was cancelled: stop the external process too
            if (process != null) {
                process.destroy();
            }
            throw e;
        } finally {
            input.delete();
            output.delete();
            log.delete();
        }
    }

    /** Extracts the transform and the path data from Potrace's SVG output. */
    static Output parse(String svg) throws VectorizationException {
        String marker = "transform=\"translate(";
        int start = svg.indexOf(marker);
        if (start < 0) {
            throw new VectorizationException("Unexpected Potrace output (no transform found).");
        }
        start += marker.length();
        int end = svg.indexOf(")", start);
        String[] translate = svg.substring(start, end).split(",");
        int scaleStart = svg.indexOf("scale(", end) + "scale(".length();
        int scaleEnd = svg.indexOf(")", scaleStart);
        String[] scale = svg.substring(scaleStart, scaleEnd).split(",");

        String data = "";
        int index = svg.indexOf("<path d=\"");
        while (index >= 0) {
            int dataStart = index + "<path d=\"".length();
            int dataEnd = svg.indexOf("\"", dataStart);
            String path = svg.substring(dataStart, dataEnd).replace('\n', ' ');
            data = data.isEmpty() ? path : data + " " + path;
            index = svg.indexOf("<path d=\"", dataEnd);
        }
        return new Output(data, Double.parseDouble(translate[0]), Double.parseDouble(translate[1]),
                Double.parseDouble(scale[0]), Double.parseDouble(scale[1]));
    }

    /** Reads a whole text file ("\\Z" = end of input, so the first token is the entire file). */
    private static String readFile(File file) throws IOException {
        try (Scanner scanner = new Scanner(file)) {
            scanner.useDelimiter("\\Z");
            if (scanner.hasNext()) {
                return scanner.next();
            }
            return "";
        }
    }
}

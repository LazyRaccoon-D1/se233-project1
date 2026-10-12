package se233.project1.controller;

import se233.project1.exception.InvalidInputException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Turns dropped files into a list of images. Accepted inputs are one or more JPG/PNG files, or a single
 * ZIP archive containing images (extracted to a temporary folder).
 */
public class InputResolver {
    /** The images to process and the names of ignored files (to tell the user). */
    public static class Resolution {
        private final List<File> images;
        private final List<String> skipped;

        public Resolution(List<File> images, List<String> skipped) {
            this.images = images;
            this.skipped = skipped;
        }

        public List<File> getImages() { return images; }
        public List<String> getSkipped() { return skipped; }
    }

    public static boolean isImage(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    public static boolean isZip(String fileName) {
        return fileName.toLowerCase().endsWith(".zip");
    }

    public static Resolution resolve(List<File> files) throws InvalidInputException {
        if (files == null || files.isEmpty()) {
            throw new InvalidInputException("No files were dropped.");
        }
        long zips = files.stream().filter(f -> isZip(f.getName())).count();
        if (zips > 0) {
            if (zips > 1) {
                throw new InvalidInputException("Please drop only one ZIP archive at a time.");
            }
            if (files.size() > 1) {
                throw new InvalidInputException("Please drop either image files or a single ZIP archive, not both together.");
            }
            return extractZip(files.get(0));
        }

        List<File> images = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && isImage(f.getName())) {
                images.add(f);
            } else {
                skipped.add(f.getName());
            }
        }
        if (images.isEmpty()) {
            throw new InvalidInputException("None of the dropped files is a supported image.\n"
                    + "Supported inputs: .jpg / .png images, or one .zip archive of images.\n\nIgnored: " + skipped);
        }
        return new Resolution(images, skipped);
    }

    static Resolution extractZip(File zip) throws InvalidInputException {
        if (!zip.canRead()) {
            throw new InvalidInputException("The ZIP archive \"" + zip.getName() + "\" cannot be read.");
        }
        File target = new File(System.getProperty("java.io.tmpdir"),
                "se233-vectorizer-" + System.currentTimeMillis());
        if (!target.mkdirs()) {
            throw new InvalidInputException("Could not create a temporary folder to extract the archive.");
        }
        target.deleteOnExit();

        List<File> images = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            String targetPath = target.getCanonicalPath() + File.separator;
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                String fileName = new File(entryName).getName();
                if (entry.isDirectory() || entryName.startsWith("__MACOSX/") || fileName.startsWith(".")) {
                    // folders and macOS metadata are not images
                } else if (!isImage(fileName)) {
                    skipped.add(entryName);
                } else {
                    File out = new File(target, entryName);
                    if (!out.getCanonicalPath().startsWith(targetPath)) {
                        // "zip slip": an entry trying to escape the extraction folder
                        skipped.add(entryName);
                    } else {
                        out.getParentFile().mkdirs();
                        out.getParentFile().deleteOnExit();
                        copy(in, out);
                        out.deleteOnExit();
                        images.add(out);
                    }
                }
                entry = in.getNextEntry();
            }
        } catch (IOException e) {
            throw new InvalidInputException("The ZIP archive \"" + zip.getName() + "\" is corrupted or cannot be opened.");
        }
        if (images.isEmpty()) {
            throw new InvalidInputException("The ZIP archive \"" + zip.getName() + "\" does not contain any .jpg or .png images.");
        }
        images.sort((a, b) -> a.getPath().compareTo(b.getPath()));
        System.out.println("Extracted " + images.size() + " images from " + zip.getName() + " into " + target);
        return new Resolution(images, skipped);
    }

    /** Copies the current ZIP entry into a file. */
    private static void copy(ZipInputStream in, File out) throws IOException {
        byte[] buffer = new byte[8192];
        try (FileOutputStream fileOut = new FileOutputStream(out)) {
            int read = in.read(buffer);
            while (read > 0) {
                fileOut.write(buffer, 0, read);
                read = in.read(buffer);
            }
        }
    }
}

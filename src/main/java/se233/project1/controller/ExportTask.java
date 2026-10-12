package se233.project1.controller;

import javafx.application.Platform;
import se233.project1.model.ImageItem;
import se233.project1.model.ItemStatus;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

/** Export of one image, run on a worker thread: trace at export resolution and save the SVG file. */
public class ExportTask implements Callable<ImageItem> {
    private final ImageItem item;
    private final VectorSettings settings;
    private final File target;
    private final Vectorizer vectorizer;

    public ExportTask(ImageItem item, VectorSettings settings, File target, Vectorizer vectorizer) {
        this.item = item;
        this.settings = settings;
        this.target = target;
        this.vectorizer = vectorizer;
    }

    @Override
    public ImageItem call() throws Exception {
        Platform.runLater(() -> item.setStatus(ItemStatus.EXPORTING));
        VectorResult result = vectorizer.vectorize(item.getPixels(), item.getWidth(), item.getHeight(),
                item.getAnalysis(), settings, Vectorizer.EXPORT_MAX_SIDE);

        // write to a temporary file first so a half-written SVG never appears under the final name
        File temp = new File(target.getParentFile(), target.getName() + ".part");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(result.toSvg().getBytes());
        } catch (IOException e) {
            temp.delete();
            throw new IOException("Could not write " + target.getName() + ": " + e.getMessage());
        }
        if (!temp.renameTo(target)) {
            temp.delete();
            throw new IOException("Could not save " + target.getName());
        }
        System.out.println("Exported " + target + " (" + settings.describe() + ")");
        Platform.runLater(() -> {
            item.setMessage("Saved as " + target.getName());
            item.setStatus(ItemStatus.EXPORTED);
        });
        return item;
    }
}

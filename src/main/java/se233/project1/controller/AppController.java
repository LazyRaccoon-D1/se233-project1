package se233.project1.controller;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.project1.exception.PotraceNotFoundException;
import se233.project1.model.ColorMode;
import se233.project1.model.DetailLevel;
import se233.project1.model.ImageItem;
import se233.project1.model.ItemStatus;
import se233.project1.model.VectorResult;
import se233.project1.model.VectorSettings;
import se233.project1.view.Dialogs;
import se233.project1.view.EditorView;
import se233.project1.view.MainView;
import se233.project1.view.PreviewPane;
import se233.project1.view.SettingsPanel;
import se233.project1.view.StatusBar;
import se233.project1.view.VectorNodeFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Connects the views with the vectorization engine and starts all background work:
 * preprocessing of dropped images, previews when a setting changes, and the batch export.
 * Batches run in parallel only when the project constraint holds (several images, all set to
 * Medium detail and Custom colors); otherwise they run on a single worker thread.
 */
public class AppController {
    private static final Logger logger = LogManager.getLogger(AppController.class);

    private final Stage stage;
    private final HostServices hostServices;
    private final MainView view;
    private final EditorView editor;
    private final SettingsPanel settingsPanel;
    private final StatusBar statusBar;
    private final Vectorizer vectorizer = new Vectorizer(new PotraceRunner());

    private final ObservableList<ImageItem> items = FXCollections.observableArrayList();
    private int currentIndex = -1;
    private boolean busy = false;
    private BatchTask currentBatch;
    private boolean previewRunning = false;
    private PrefetchTask prefetchTask;
    private ImageItem shownItem;
    private Image shownImage;
    private File lastDirectory;
    private boolean potraceErrorShown = false;

    public AppController(Stage stage, HostServices hostServices, MainView view, Scene scene) {
        this.stage = stage;
        this.hostServices = hostServices;
        this.view = view;
        this.editor = view.getEditor();
        this.settingsPanel = editor.getSettingsPanel();
        this.statusBar = editor.getStatusBar();
        wireDragAndDrop(scene);
        wireControls();
        if (!PotraceRunner.isAvailable()) {
            view.getDropZone().showWarning("Potrace was not found, so images cannot be vectorized yet. "
                    + "Install it (macOS: brew install potrace) and restart the application.");
        }
    }

    // ------------------------------------------------------------------ event handlers

    private void wireDragAndDrop(Scene scene) {
        scene.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() && !busy) {
                e.acceptTransferModes(TransferMode.COPY);
                view.setDragHover(true);
            }
            e.consume();
        });
        scene.setOnDragExited(e -> view.setDragHover(false));
        scene.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !busy) {
                success = true;
                List<File> files = new ArrayList<>(db.getFiles());
                // handle the files after the drop gesture has finished
                Platform.runLater(() -> handleFiles(files));
            }
            view.setDragHover(false);
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void wireControls() {
        editor.getQueueStrip().setOnSelect(index -> setCurrent(index));
        editor.getQueueStrip().setOnRemove(item -> removeItem(item));
        editor.getQueueStrip().getClearButton().setOnAction(e -> clearAll());
        settingsPanel.setOnSettingsChanged(settings -> onSettingsChanged(settings));
        settingsPanel.getBackButton().setOnAction(e -> setCurrent(currentIndex - 1));
        settingsPanel.getNextButton().setOnAction(e -> setCurrent(currentIndex + 1));
        settingsPanel.getExportAllButton().setOnAction(e -> exportAll());
        settingsPanel.getApplyToAllButton().setOnAction(e -> applyToAll());
        statusBar.getCancelButton().setOnAction(e -> {
            if (currentBatch != null) {
                currentBatch.stopBatch();
            }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        settingsPanel.getExportAllButton().setDisable(value);
        settingsPanel.getApplyToAllButton().setDisable(value);
        editor.getQueueStrip().setEditingDisabled(value);
    }

    // ------------------------------------------------------------------ input

    /** Validates dropped files (and extracts a ZIP) on a background thread, then starts preprocessing. */
    public void handleFiles(List<File> files) {
        if (busy) {
            Dialogs.info(stage, "Please wait", "New files can be added once the current job has finished.");
            return;
        }
        Task<InputResolver.Resolution> resolveTask = new Task<InputResolver.Resolution>() {
            @Override
            protected InputResolver.Resolution call() throws Exception {
                return InputResolver.resolve(files);
            }
        };
        setBusy(true);
        statusBar.setIdle("Reading files...");
        resolveTask.setOnSucceeded(e -> {
            setBusy(false);
            startPreprocessing(resolveTask.getValue());
        });
        resolveTask.setOnFailed(e -> {
            setBusy(false);
            statusBar.setIdle("Ready");
            Throwable error = resolveTask.getException();
            logger.warn("Rejected dropped files: {}", error.getMessage());
            Dialogs.error(stage, "These files cannot be opened", error.getMessage());
        });
        Thread thread = new Thread(resolveTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void startPreprocessing(InputResolver.Resolution resolution) {
        List<ImageItem> added = new ArrayList<>();
        List<Callable<ImageItem>> jobs = new ArrayList<>();
        for (File file : resolution.getImages()) {
            ImageItem item = new ImageItem(file);
            // refresh the editor when this image finishes loading or changes status
            item.fxImageProperty().addListener((obs, oldImage, newImage) -> {
                if (isCurrent(item)) {
                    showCurrent();
                }
            });
            item.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (isCurrent(item)) {
                    showCurrent();
                }
            });
            added.add(item);
            jobs.add(new PreprocessTask(item, vectorizer));
        }
        int firstNew = items.size();
        items.addAll(added);
        view.showEditor();
        currentIndex = firstNew;
        editor.getQueueStrip().refresh(items, currentIndex);
        showCurrent();

        // The default settings (Medium detail + 2 custom colors) meet the parallel constraint,
        // so several dropped images are always preprocessed concurrently.
        boolean parallel = added.size() > 1;
        long start = System.currentTimeMillis();
        runBatch("Preprocessing", added, jobs, parallel, result -> {
            for (ImageItem item : added) {
                if (!item.isLoaded() && item.getStatus() != ItemStatus.FAILED) {
                    item.setMessage("Preprocessing was cancelled");
                    item.setStatus(ItemStatus.FAILED);
                } else if (item.getStatus() == ItemStatus.PROCESSING || item.getStatus() == ItemStatus.QUEUED) {
                    item.setStatus(ItemStatus.READY);
                }
            }
            showCurrent();
            updateExportMode();
            if (result == null) {
                statusBar.setIdle("Preprocessing cancelled");
                return;
            }
            statusBar.setIdle(String.format("Preprocessed %d of %d images in %.1f s (%s)", result.getSucceeded(),
                    result.getTotal(), (System.currentTimeMillis() - start) / 1000.0, parallel ? "parallel" : "sequential"));
            reportFailures(result, "Some images could not be processed");
        });

        if (!resolution.getSkipped().isEmpty()) {
            Dialogs.withDetails(stage, Alert.AlertType.INFORMATION, "Some files were skipped",
                    "Only .jpg and .png images are supported. These files were ignored:", resolution.getSkipped());
        }
    }

    // ------------------------------------------------------------------ batches

    /**
     * Runs one job per image on a background thread with a progress bar.
     * {@code onDone} receives the result, or null when the batch was cancelled or failed.
     */
    private void runBatch(String verb, List<ImageItem> batch, List<Callable<ImageItem>> jobs, boolean parallel,
                          Consumer<BatchResult> onDone) {
        stopPrefetch();
        BatchTask task = new BatchTask(verb, batch, jobs, parallel);
        currentBatch = task;
        setBusy(true);
        statusBar.getProgressBar().progressProperty().bind(task.progressProperty());
        statusBar.getMessage().textProperty().bind(task.messageProperty());
        if (parallel) {
            statusBar.setThreadInfo("Parallel: " + Math.min(BatchTask.THREADS, batch.size()) + " worker threads");
        } else {
            statusBar.setThreadInfo("Sequential: 1 worker thread");
        }
        statusBar.setRunning(true);

        task.setOnSucceeded(e -> {
            finishBatch();
            onDone.accept(task.getValue());
        });
        task.setOnCancelled(e -> {
            finishBatch();
            onDone.accept(null);
        });
        task.setOnFailed(e -> {
            finishBatch();
            logger.error("{} failed", verb, task.getException());
            Dialogs.error(stage, verb + " failed", String.valueOf(task.getException().getMessage()));
            onDone.accept(null);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void finishBatch() {
        currentBatch = null;
        setBusy(false);
        statusBar.setIdle("Ready");
    }

    private void reportFailures(BatchResult result, String header) {
        if (result.getFailures().isEmpty()) {
            return;
        }
        if (result.isPotraceMissing()) {
            showPotraceError();
            return;
        }
        Dialogs.withDetails(stage, Alert.AlertType.ERROR, header,
                result.getFailures().size() + " of " + result.getTotal() + " images failed:", result.getFailures());
    }

    // ------------------------------------------------------------------ navigation and preview

    private ImageItem current() {
        if (currentIndex >= 0 && currentIndex < items.size()) {
            return items.get(currentIndex);
        }
        return null;
    }

    private boolean isCurrent(ImageItem item) {
        return item != null && item == current();
    }

    private void setCurrent(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        currentIndex = index;
        editor.getQueueStrip().select(index);
        showCurrent();
    }

    /** Shows the current image: original, settings and vectorized result. */
    private void showCurrent() {
        ImageItem item = current();
        settingsPanel.setPosition(currentIndex, items.size());
        PreviewPane originalPane = editor.getOriginalPane();
        if (item == null) {
            originalPane.clearContent();
            editor.getResultPane().clearContent();
            shownItem = null;
            shownImage = null;
            return;
        }
        Image image = item.getFxImage();
        if (image != null) {
            if (item != shownItem || image != shownImage) {
                originalPane.setContent(VectorNodeFactory.rasterNode(image, editor.getViewport()),
                        image.getWidth(), image.getHeight());
                editor.getViewport().setContentSize(image.getWidth(), image.getHeight());
            }
            originalPane.hideOverlay();
            originalPane.setCaption(String.format("%s (%d x %d px)", item.getDisplayName(),
                    (int) image.getWidth(), (int) image.getHeight()));
        } else {
            originalPane.clearContent();
            originalPane.setCaption(item.getDisplayName());
            if (item.getStatus() == ItemStatus.FAILED) {
                originalPane.showError(item.getMessage());
            } else {
                originalPane.showLoading("Loading image...");
            }
        }
        shownItem = item;
        shownImage = image;
        settingsPanel.showSettings(item.getSettings(), item.getAnalysis());
        settingsPanel.setSettingsDisabled(!item.isLoaded());
        refreshResult(item);
    }

    /** Shows the vectorized preview for the item's current settings, tracing it first if needed. */
    private void refreshResult(ImageItem item) {
        if (!isCurrent(item)) {
            return;
        }
        PreviewPane resultPane = editor.getResultPane();
        VectorSettings settings = item.getSettings();
        if (settings == null) {
            resultPane.clearContent();
            resultPane.setCaption("");
            if (item.getStatus() == ItemStatus.FAILED) {
                resultPane.showError(item.getMessage());
            } else if (item.getStatus() == ItemStatus.QUEUED) {
                resultPane.showLoading("Waiting in queue...");
            } else {
                resultPane.showLoading("Detecting colors...");
            }
            return;
        }
        VectorResult cached = item.getCachedPreview(settings);
        if (cached != null) {
            showResult(cached);
            startPrefetch(item);
            return;
        }
        if (item.getStatus() == ItemStatus.PROCESSING && settings.sameAs(VectorSettings.defaults(item.getAnalysis()))) {
            // the preprocessing job is already tracing this preview
            resultPane.showLoading("Vectorizing (Medium detail, " + settings.getCustomColors() + " colors)...");
            return;
        }
        requestPreview(item, settings);
    }

    private void showResult(VectorResult result) {
        PreviewPane resultPane = editor.getResultPane();
        resultPane.setContent(VectorNodeFactory.vectorNode(result), result.getWidth(), result.getHeight());
        resultPane.hideOverlay();
        int layers = result.getLayers().size();
        resultPane.setCaption(String.format("%s · %d %s · %.1f KB SVG · traced in %d ms",
                result.getSettings().describe(), layers, layers == 1 ? "layer" : "layers",
                result.getSvgSizeBytes() / 1024.0, result.getElapsedMillis()));
    }

    /**
     * Traces a preview on a background thread. Only one preview runs at a time: if the user changes the
     * settings meanwhile, the newest settings are traced as soon as the running preview finishes.
     */
    private void requestPreview(ImageItem item, VectorSettings settings) {
        PreviewPane resultPane = editor.getResultPane();
        resultPane.showLoading("Updating preview...");
        if (previewRunning) {
            return;
        }
        stopPrefetch();
        previewRunning = true;
        PreviewTask task = new PreviewTask(item, settings, vectorizer);
        task.setOnSucceeded(e -> {
            previewRunning = false;
            refreshResult(current());
        });
        task.setOnFailed(e -> {
            previewRunning = false;
            Throwable error = task.getException();
            logger.error("Preview failed for {}", item.getDisplayName(), error);
            if (error instanceof PotraceNotFoundException) {
                showPotraceError();
            }
            if (isCurrent(item) && settings.sameAs(item.getSettings())) {
                resultPane.showError("Could not vectorize this image:\n" + error.getMessage());
            } else {
                refreshResult(current());
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /** Pre-traces the settings one click away so the next change is shown instantly. */
    private void startPrefetch(ImageItem item) {
        stopPrefetch();
        if (busy || previewRunning || !item.isLoaded()) {
            return;
        }
        VectorSettings s = item.getSettings();
        List<VectorSettings> variants = new ArrayList<>();
        if (s.getColorMode() == ColorMode.CUSTOM) {
            for (int n = 1; n <= item.getAnalysis().getCustomColorLimit(); n++) {
                variants.add(s.withCustomColors(n));
            }
        }
        for (DetailLevel d : DetailLevel.values()) {
            variants.add(s.withDetail(d));
        }
        variants.add(s.withRemoveBackground(!s.isRemoveBackground()));
        variants.add(s.withColorMode(s.getColorMode() == ColorMode.CUSTOM ? ColorMode.UNLIMITED : ColorMode.CUSTOM));
        prefetchTask = new PrefetchTask(item, variants, vectorizer);
        Thread thread = new Thread(prefetchTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void stopPrefetch() {
        if (prefetchTask != null) {
            prefetchTask.cancel();
            prefetchTask = null;
        }
    }

    // ------------------------------------------------------------------ settings

    private void onSettingsChanged(VectorSettings settings) {
        ImageItem item = current();
        if (item == null || !item.isLoaded()) {
            return;
        }
        VectorSettings adapted = settings.adaptTo(item.getAnalysis());
        if (adapted.sameAs(item.getSettings())) {
            return;
        }
        item.setSettings(adapted);
        if (item.getStatus() == ItemStatus.EXPORTED || item.getStatus() == ItemStatus.FAILED) {
            item.setMessage("");
            item.setStatus(ItemStatus.READY);
        }
        updateExportMode();
        refreshResult(item);
    }

    private void applyToAll() {
        ImageItem item = current();
        if (item == null || !item.isLoaded()) {
            return;
        }
        VectorSettings settings = item.getSettings();
        int changed = 0;
        for (ImageItem other : items) {
            if (other != item && other.isLoaded()) {
                other.setSettings(settings.adaptTo(other.getAnalysis()));
                changed++;
            }
        }
        updateExportMode();
        statusBar.setIdle("Applied \"" + settings.describe() + "\" to " + changed + " other image(s)");
    }

    private List<ImageItem> loadedItems() {
        List<ImageItem> loaded = new ArrayList<>();
        for (ImageItem item : items) {
            if (item.isLoaded()) {
                loaded.add(item);
            }
        }
        return loaded;
    }

    private void updateExportMode() {
        List<ImageItem> exportable = loadedItems();
        settingsPanel.setExportMode(isParallelExport(exportable), exportable.size());
    }

    /** The project constraint: parallel only for several images that all use Medium detail + Custom colors. */
    static boolean isParallelExport(List<ImageItem> exportable) {
        if (exportable.size() <= 1) {
            return false;
        }
        for (ImageItem item : exportable) {
            if (!item.getSettings().isParallelEligible()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ export

    private void exportAll() {
        if (busy) {
            return;
        }
        if (loadedItems().isEmpty()) {
            Dialogs.warning(stage, "Nothing to export", "None of the images could be loaded.");
            return;
        }
        if (!PotraceRunner.isAvailable()) {
            potraceErrorShown = false;
            showPotraceError();
            return;
        }
        // the operating system's own folder chooser
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose the output folder for the SVG files");
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File directory = chooser.showDialog(stage);
        if (directory != null) {
            lastDirectory = directory;
            exportTo(directory);
        }
    }

    /** Exports every loaded image into {@code outDir}, in parallel when the constraint allows it. */
    public void exportTo(File outDir) {
        List<ImageItem> exportable = loadedItems();
        if (!outDir.isDirectory() || !outDir.canWrite()) {
            Dialogs.error(stage, "Cannot write to this folder", outDir + " is not writable. Please choose another folder.");
            return;
        }
        List<Callable<ImageItem>> jobs = new ArrayList<>();
        List<String> usedNames = new ArrayList<>();
        for (ImageItem item : exportable) {
            File target = uniqueTarget(outDir, item.getBaseName(), usedNames);
            // the settings are copied now, so changing them during the export has no effect on it
            jobs.add(new ExportTask(item, item.getSettings(), target, vectorizer));
        }
        boolean parallel = isParallelExport(exportable);
        long start = System.currentTimeMillis();
        runBatch("Exporting", exportable, jobs, parallel, result -> {
            for (ImageItem item : exportable) {
                if (item.getStatus() == ItemStatus.EXPORTING || item.getStatus() == ItemStatus.QUEUED) {
                    item.setStatus(ItemStatus.READY);
                }
            }
            if (result == null) {
                statusBar.setIdle("Export cancelled");
                return;
            }
            String done = String.format("Exported %d of %d SVG files in %.1f s (%s)", result.getSucceeded(),
                    result.getTotal(), (System.currentTimeMillis() - start) / 1000.0, parallel ? "parallel" : "sequential");
            statusBar.setIdle(done + " to " + outDir);
            if (!result.getFailures().isEmpty()) {
                reportFailures(result, "Some files could not be exported");
            } else if (Dialogs.confirm(stage, "Export complete", done + "\nSaved to: " + outDir
                    + "\n\nOpen the output folder?")) {
                hostServices.showDocument(outDir.toURI().toString());
            }
        });
    }

    /** A file name that is not used yet: "name.svg", then "name (2).svg", "name (3).svg", ... */
    private static File uniqueTarget(File outDir, String baseName, List<String> usedNames) {
        String base = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String name = base + ".svg";
        int n = 2;
        while (usedNames.contains(name.toLowerCase()) || new File(outDir, name).exists()) {
            name = base + " (" + n + ").svg";
            n++;
        }
        usedNames.add(name.toLowerCase());
        return new File(outDir, name);
    }

    // ------------------------------------------------------------------ queue editing

    private void removeItem(ImageItem item) {
        if (busy) {
            return;
        }
        int index = items.indexOf(item);
        if (index < 0) {
            return;
        }
        stopPrefetch();
        items.remove(index);
        if (items.isEmpty()) {
            clearAll();
            return;
        }
        if (index < currentIndex || currentIndex >= items.size()) {
            currentIndex--;
        }
        editor.getQueueStrip().refresh(items, currentIndex);
        updateExportMode();
        showCurrent();
    }

    private void clearAll() {
        if (busy) {
            return;
        }
        stopPrefetch();
        items.clear();
        currentIndex = -1;
        editor.getQueueStrip().refresh(items, currentIndex);
        statusBar.setIdle("Ready");
        showCurrent();
        view.showDropZone();
    }

    // ------------------------------------------------------------------ helpers

    private void showPotraceError() {
        if (potraceErrorShown) {
            return;
        }
        potraceErrorShown = true;
        Dialogs.error(stage, "Potrace is not installed", new PotraceNotFoundException().getMessage());
    }

    /** Called when the window closes: stop all background work. */
    public void shutdown() {
        stopPrefetch();
        if (currentBatch != null) {
            currentBatch.stopBatch();
        }
    }
}

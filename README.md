# SE233 Term Project I — Vectorizer (PNG/JPG → SVG)

A JavaFX desktop app for batch raster-to-vector conversion, inspired by Vector Magic. Tracing is done by the
**Potrace** command-line tool. Potrace only traces black-and-white bitmaps, so the app detects the image's
colors itself and traces one stacked layer per color.

## Setup

1. **JDK 24+**. The project uses JavaFX 26.0.1, the same setup as the course's chapter projects.
2. **Potrace**. The app finds it automatically and shows an error message if it is missing.
   - macOS: `brew install potrace`
   - Linux: `sudo apt install potrace`
   - Windows: download potrace.exe from <https://potrace.sourceforge.net>, then put it on the `PATH` or in a
     `tools/` folder in the project root. You can also set `POTRACE_PATH` to the full path of the exe.
3. Run it:
   - IntelliJ: run `se233.project1.Launcher`
   - Maven: `./mvnw clean javafx:run`

## Course concepts used (SE233 coursebook, chapters 1-3)

The project only uses techniques from the first half of the course.

| Chapter | Concept | Where in the project |
|---|---|---|
| 1 Event-driven programming | JavaFX layouts and controls, event handlers (`setOnAction`, mouse events, `setOnCloseRequest`), drag and drop (`setOnDragOver` / `setOnDragDropped`, `Dragboard`, `TransferMode`), positioning with `setLayoutX` | `view/*`; `AppController.wireDragAndDrop`; drag to pan and double-click to fit in `PreviewPane` |
| 2 Exception handling | Custom checked exceptions extending `IOException` (like `EmptyFileException`), `throws`, try-catch-finally, try-with-resources, `printStackTrace` / `System.err`, lambdas and streams, `Alert` dialogs | `exception/*`; `PotraceRunner.trace` (`finally` deletes temp files); `Dialogs`; `InputResolver` |
| 3 Concurrency | `Task` with `updateProgress` / `updateMessage` bound to a `ProgressBar`, `setOnSucceeded`; `Callable` job classes; `ExecutorService` (`newFixedThreadPool` / `newSingleThreadExecutor`); `ExecutorCompletionService` + `Future`; `synchronized`; `Platform.runLater`; daemon threads; `shutdownNow()` and `interrupt()` for cancelling; `HostServices.showDocument` | `BatchTask`, `PreprocessTask`, `ExportTask`, `PreviewTask`, `PrefetchTask`, `ResolveTask`, `ImageItem` (synchronized), `AppController` |

Some APIs are outside the coursebook but required by the project spec:

| API | Required for |
|---|---|
| `ProcessBuilder`, `System.getenv("PATH")` | Finding and running the Potrace command-line tool, which the spec recommends |
| `ZipInputStream` | ZIP input |
| `DirectoryChooser` | "The OS default file browser" for the output folder |
| `PixelReader` | Reading pixels to detect the image's colors |
| `SVGPath`, `setScaleX` | Drawing the vector result, and zooming the preview |
| `ToggleButton` and `CheckBox` | The spec's selectors and checkbox |

## Rubric coverage (pair version)

| Item | Pts | Implementation |
|---|---|---|
| Vectorization format | 1 | JPG and PNG are decoded with JavaFX `Image`, traced with Potrace, and saved as valid SVG |
| UI navigation & view | 2 | The top panel has only Zoom In, Zoom Out and Zoom to Fit. Both previews share one zoom/pan state. Back/Next move between images |
| Details & background | 2 | Low/Medium/High map to Potrace `turdsize`, `alphamax` and `opttolerance`, plus the tracing resolution. Remove background flood-fills the border color to transparent; a checkerboard shows the transparency |
| Colors | 3 | Unlimited keeps up to 24 detected colors. Custom detects colors automatically, caps at 5 and defaults to 2. Previews are instant: results are cached per setting and neighbouring settings are traced in the background |
| ZIP handling | 1 | The ZIP is extracted to a temp folder (with zip-slip protection); non-images are skipped with a message |
| Preprocessing | 2 | `PreprocessTask` per image: detect colors, then Medium + 2 colors, then the preview. Images run in parallel on a 4-thread pool, with a progress bar, a "N running concurrently" message and per-thumbnail spinners |
| Export | 2 | Export All opens the OS folder chooser and writes `.svg` files. Export runs in parallel only if every image is Medium + Custom, otherwise on one thread. The panel and status bar show which mode is used |
| Additional feature | 1 | Batch queue panel: live status per image, click to open, × to remove, Clear all, drop more files to add, Apply settings to all |
| UX/UI | 1 | Subjective score from the lecturers |

## Error handling
All of these show a clear message instead of crashing:
- Potrace is missing (`PotraceNotFoundException`): a warning on the start screen and a dialog with install steps.
- A corrupted image (`CorruptedImageException`): a red mark in the queue, the reason in both panes and a summary dialog. The other images still work.
- Bad input (`InvalidInputException`): unsupported files, a ZIP mixed with images, several ZIPs, or a broken or image-less ZIP.
- A Potrace failure (`VectorizationException`) or an unwritable output folder.

## Architecture

```
se233.project1
├── Launcher
├── model       DetailLevel, ColorMode, ItemStatus, VectorSettings, ColorAnalysis, VectorResult, ImageItem
├── controller  AppController   UI wiring, drop handling, preview, export
│               BatchTask       Task: runs the Callables on a thread pool (parallel) or a single thread
│               PreprocessTask  Callable: load → detect colors → Medium + 2 colors → preview
│               ExportTask      Callable: trace at export resolution → write .svg
│               PreviewTask / PrefetchTask   Task: previews for setting changes
│               ResolveTask     Task: checks dropped files / extracts the ZIP
│               Vectorizer      quantize → stack color layers → Potrace per layer
│               ColorAnalyzer   color detection + k-means color reduction
│               PotraceRunner   runs the potrace process (temp PBM file in, SVG file out)
│               InputResolver   validates drops, extracts ZIPs
│               ImageLoader     JavaFX Image decoding, pixel reading
├── view        MainView, DropZoneView, EditorView, PreviewPane, ViewportModel,
│               SettingsPanel, QueueStrip, StatusBar, Dialogs, VectorNodeFactory
└── exception   InvalidInputException, CorruptedImageException,
                PotraceNotFoundException, VectorizationException  (all extend IOException)
```

## Sample files (`samples/`) and a demo flow
1. Drop `samples/sample-images.zip`. You get a message that `readme.txt` was skipped, and the progress bar shows the 5 images being preprocessed in parallel.
2. Use Zoom In, Zoom Out and Zoom to Fit, and drag to pan. Both panes stay in sync.
3. Move between images with Back / Next.
4. On `color-badge.png` it says "Detected 7 colors — capped at 5". Switch Custom 2 → 5 → Unlimited; the preview updates instantly.
5. On `orange-logo.jpg`, tick Remove background; the checkerboard shows through. Try Low and High.
6. Export All with every image on Medium + Custom: a parallel export. Then set one image to High and export again: a sequential export.
7. Drop `samples/error-demo/corrupted.png` to show the error handling.

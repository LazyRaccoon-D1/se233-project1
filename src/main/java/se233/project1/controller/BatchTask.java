package se233.project1.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.project1.exception.PotraceNotFoundException;
import se233.project1.model.ImageItem;
import se233.project1.model.ItemStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs one Callable per image and reports progress, like the indexing Task of the Chapter 3 case study.
 * In parallel mode the jobs run on a fixed thread pool; otherwise on a single thread, one after another.
 * Results are collected with an ExecutorCompletionService in the order the jobs finish.
 */
public class BatchTask extends Task<BatchResult> {
    private static final Logger logger = LogManager.getLogger(BatchTask.class);
    public static final int THREADS = 4;

    private final String verb;
    private final List<ImageItem> items;
    private final List<Callable<ImageItem>> jobs;
    private final boolean parallel;
    private ExecutorService executor;
    private int running = 0;
    private int finished = 0;

    public BatchTask(String verb, List<ImageItem> items, List<Callable<ImageItem>> jobs, boolean parallel) {
        this.verb = verb;
        this.items = items;
        this.jobs = jobs;
        this.parallel = parallel;
    }

    public boolean isParallel() { return parallel; }

    @Override
    protected BatchResult call() throws Exception {
        int total = jobs.size();
        logger.info("{} {} image(s) {}", verb, total, parallel ? "in parallel" : "sequentially");
        if (parallel) {
            executor = Executors.newFixedThreadPool(THREADS);
        } else {
            executor = Executors.newSingleThreadExecutor();
        }
        ExecutorCompletionService<ImageItem> completionService = new ExecutorCompletionService<>(executor);
        Map<Future<ImageItem>, ImageItem> owners = new HashMap<>();
        List<String> failures = new ArrayList<>();
        boolean potraceMissing = false;
        int succeeded = 0;
        updateProgress(0, total);
        publish();
        try {
            for (int i = 0; i < total; i++) {
                Callable<ImageItem> job = jobs.get(i);
                Future<ImageItem> future = completionService.submit(() -> {
                    jobStarted();
                    try {
                        return job.call();
                    } finally {
                        jobFinished();
                    }
                });
                owners.put(future, items.get(i));
            }
            for (int i = 0; i < total; i++) {
                Future<ImageItem> future = completionService.take();
                ImageItem item = owners.get(future);
                try {
                    future.get();
                    succeeded++;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof PotraceNotFoundException) {
                        potraceMissing = true;
                    }
                    failures.add(item.getDisplayName() + ": " + cause.getMessage());
                    logger.warn("{} failed for {}: {}", verb, item.getDisplayName(), cause.toString());
                    Platform.runLater(() -> {
                        item.setMessage(cause.getMessage());
                        item.setStatus(ItemStatus.FAILED);
                    });
                }
                updateProgress(i + 1, total);
                publish();
            }
        } finally {
            executor.shutdown();
        }
        return new BatchResult(total, succeeded, failures, potraceMissing);
    }

    /** Cancel button: stop all worker threads (shutdownNow interrupts them) and cancel this Task. */
    public void stopBatch() {
        if (executor != null) {
            executor.shutdownNow();
        }
        cancel();
    }

    private synchronized void jobStarted() {
        running++;
        publish();
    }

    private synchronized void jobFinished() {
        running--;
        finished++;
    }

    private synchronized void publish() {
        int total = jobs.size();
        if (parallel) {
            updateMessage(String.format("%s %d images in parallel: %d running concurrently, %d of %d done",
                    verb, total, running, finished, total));
        } else {
            updateMessage(String.format("%s %d image(s) sequentially: %d of %d done", verb, total, finished, total));
        }
    }
}

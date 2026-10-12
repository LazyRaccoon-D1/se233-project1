package se233.project1.controller;

import java.util.List;

/** The outcome of a batch: how many images succeeded, why the others failed, and whether it was cancelled. */
public class BatchResult {
    private final int total;
    private final int succeeded;
    private final List<String> failures;
    private final boolean potraceMissing;
    private final boolean cancelled;

    public BatchResult(int total, int succeeded, List<String> failures, boolean potraceMissing, boolean cancelled) {
        this.total = total;
        this.succeeded = succeeded;
        this.failures = failures;
        this.potraceMissing = potraceMissing;
        this.cancelled = cancelled;
    }

    public int getTotal() { return total; }
    public int getSucceeded() { return succeeded; }
    public List<String> getFailures() { return failures; }
    public boolean isPotraceMissing() { return potraceMissing; }
    public boolean isCancelled() { return cancelled; }
}

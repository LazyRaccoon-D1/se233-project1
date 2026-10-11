package se233.project1.controller;

import java.util.List;

/** The outcome of a batch: how many images succeeded and why the others failed. */
public class BatchResult {
    private final int total;
    private final int succeeded;
    private final List<String> failures;
    private final boolean potraceMissing;

    public BatchResult(int total, int succeeded, List<String> failures, boolean potraceMissing) {
        this.total = total;
        this.succeeded = succeeded;
        this.failures = failures;
        this.potraceMissing = potraceMissing;
    }

    public int getTotal() { return total; }
    public int getSucceeded() { return succeeded; }
    public List<String> getFailures() { return failures; }
    public boolean isPotraceMissing() { return potraceMissing; }
}

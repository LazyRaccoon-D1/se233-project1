package se233.project1;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se233.project1.controller.BatchResult;
import se233.project1.controller.BatchTask;
import se233.project1.model.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that a batch really runs concurrently in parallel mode and one at a time otherwise. */
public class BatchTaskTest {
    private int running = 0;
    private int maxRunning = 0;

    @BeforeAll
    public static void initJfxRuntime() {
        TestImages.initJfxRuntime();
    }

    private synchronized void started() {
        running++;
        maxRunning = Math.max(maxRunning, running);
    }

    private synchronized void finished() {
        running--;
    }

    private BatchResult runBatch(boolean parallel, int count) throws Exception {
        List<ImageItem> items = new ArrayList<>();
        List<Callable<ImageItem>> jobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ImageItem item = new ImageItem(new File("image" + i + ".png"));
            items.add(item);
            jobs.add(() -> {
                started();
                Thread.sleep(200);
                finished();
                return item;
            });
        }
        BatchTask task = new BatchTask("Testing", items, jobs, parallel);
        task.run();
        return task.get();
    }

    @Test
    public void call_givenParallelMode_thenJobsRunConcurrently() throws Exception {
        BatchResult result = runBatch(true, 6);
        assertEquals(6, result.getSucceeded());
        assertTrue(maxRunning > 1, "more than one job ran at the same time");
    }

    @Test
    public void call_givenSequentialMode_thenOneJobAtATime() throws Exception {
        BatchResult result = runBatch(false, 4);
        assertEquals(4, result.getSucceeded());
        assertEquals(1, maxRunning);
    }

    @Test
    public void call_givenFailingJob_thenFailureReportedAndOthersSucceed() throws Exception {
        List<ImageItem> items = new ArrayList<>();
        List<Callable<ImageItem>> jobs = new ArrayList<>();
        ImageItem good = new ImageItem(new File("good.png"));
        ImageItem bad = new ImageItem(new File("bad.png"));
        items.add(good);
        items.add(bad);
        jobs.add(() -> good);
        jobs.add(() -> {
            throw new java.io.IOException("broken image");
        });
        BatchTask task = new BatchTask("Testing", items, jobs, true);
        task.run();
        BatchResult result = task.get();
        assertEquals(1, result.getSucceeded());
        assertEquals("bad.png: broken image", result.getFailures().get(0));
    }
}

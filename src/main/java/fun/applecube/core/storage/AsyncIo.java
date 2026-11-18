package fun.applecube.core.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Single background thread for file IO, so writes to the same file never interleave. */
public final class AsyncIo {

    private final Logger logger;
    private final ExecutorService executor;

    public AsyncIo(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "AppleCore-IO");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Background IO task failed", e);
                throw e;
            }
        }, executor);
    }

    public <T> CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                logger.warning("IO thread did not finish within 15s; some data may not be saved");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

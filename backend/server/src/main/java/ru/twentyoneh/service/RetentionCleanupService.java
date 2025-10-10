package ru.twentyoneh.service;

import ru.twentyoneh.dto.FileRecord;
import ru.twentyoneh.repository.FilesRepository;
import ru.twentyoneh.storage.Storage;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetentionCleanupService implements AutoCloseable {
    private final FilesRepository repo;
    private final Storage storage;
    private final DataSource ds;
    private final int retentionDays;
    private final int batchSize;
    private final boolean dryRun;
    private final Logger logger = Logger.getLogger(RetentionCleanupService.class.getName());

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "retention-cleaner");
        t.setDaemon(true);
        return t;
    });

    public RetentionCleanupService(FilesRepository repo, Storage storage, DataSource ds,
                                   int retentionDays, int batchSize, boolean dryRun) {
        this.repo = repo;
        this.storage = storage;
        this.ds = ds;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
        this.dryRun = dryRun;
    }

    public void start(Duration initialDelay, Duration every) {
        exec.scheduleWithFixedDelay(this::runOnceSafely,
                initialDelay.toSeconds(), every.toSeconds(), TimeUnit.SECONDS);
    }

    private void runOnceSafely() {
        try {
            if (!tryAcquireLock()) return;           // другой инстанс уже чистит
            runOnce();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            releaseLock();
        }
    }

    private boolean tryAcquireLock() {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("select pg_try_advisory_lock(?, ?)")) {
            ps.setInt(1, 9021); // произвольный ключ проекта
            ps.setInt(2, 1);
            try (var rs = ps.executeQuery()) { return rs.next() && rs.getBoolean(1); }
        } catch (Exception e) {
            return true; // если БД временно недоступна — не блокировать навсегда
        }
    }
    private void releaseLock() {
        try (var c = ds.getConnection();
             var ps = c.prepareStatement("select pg_advisory_unlock(?, ?)")) {
            ps.setInt(1, 9021); ps.setInt(2, 1); ps.execute();
        } catch (Exception ignore) {}
    }

    public void runOnce() {
        int total = 0;
        for (;;) {
            List<FileRecord> batch = repo.selectExpired(retentionDays, batchSize);
            if (batch.isEmpty()) break;

            for (var fr : batch) {
                try {
                    if (!dryRun) {
                        try { storage.delete(fr.getStoredKey()); } catch (Exception ignore) {}
                        repo.deleteById(fr.getId());
                    }
                    total++;
                } catch (Exception e) {
                    // не валить всю чистку из-за одной записи
                    e.printStackTrace();
                }
            }
            try { Thread.sleep(25); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); break;
            }
        }
        logger.log(Level.INFO,"[cleanup] deleted={0} dryRun={1}: ", new Object[]{total,dryRun});
    }

    @Override public void close() { exec.shutdownNow(); }
}

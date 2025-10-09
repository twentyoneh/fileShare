package ru.twentyoneh.storage;

import org.slf4j.LoggerFactory;
import ru.twentyoneh.api.error.PayloadTooLargeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.*;

public final class LocalStorage implements Storage {
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[a-f0-9]{2}/[a-f0-9]{2}/[a-f0-9]{32}(?:\\.[a-z0-9]{1,8})?$");

    private static final int BUF = 64 * 1024;
    private final Logger logger = Logger.getLogger(LocalStorage.class.getName());

    private final Path root;
    private final long maxUploadBytes;

    public LocalStorage(Path root, long maxUploadBytes) {
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
        this.maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : Long.MAX_VALUE;
        try { Files.createDirectories(this.root); }
        catch (IOException e) { throw new RuntimeException("Cannot create storage root: " + this.root, e); }
    }


    @Override
    public String store(InputStream in, long announcedSize, String suggestedExt) throws IOException {
        Objects.requireNonNull(in,"input"); // в случае null => NullPointerException(message)

        if(announcedSize > 0 && announcedSize >  maxUploadBytes) {
            throw new PayloadTooLargeException("file exceeds configured limit");
        }

        String uuidHex = UUID.randomUUID().toString().replace("-", "");
        String p1 = uuidHex.substring(0, 2), p2 =  uuidHex.substring(2, 4);
        String fileName = uuidHex + suggestedExt;
        logger.log(Level.INFO, "Finally file name: " + fileName);

        Path dir = root.resolve(p1).resolve(p2); // создать подпапку Пример: uuidHex = 9B14774FF21 директория в котоой будет лежать файл: 9B/14/uuidHex+suggestedExt
        Files.createDirectories(dir);
        logger.log(Level.INFO, "Creating directory for file: " + dir);

        Path tmp = dir.resolve("." + fileName + "." + Long.toString(ThreadLocalRandom.current().nextLong(), 36) + ".tmp");
        Path target = dir.resolve(fileName);

        logger.log(Level.INFO, "Starting create file {0}{1}: on local storage", new Object[]{p1,p2});
        long written = 0;
        boolean success = false;
        try(OutputStream out = Files.newOutputStream(tmp,CREATE_NEW,WRITE)) {
            byte[] buf = new byte[BUF];
            while (true) {
                int len = in.read(buf);
                if(len == -1) break;
                written += len;
                if(written > maxUploadBytes) {
                    try {out.close(); } catch (Exception ignore) {}
                    safeDelete(tmp);
                    throw new PayloadTooLargeException("file exceeds configured limit");
                }
                out.write(buf, 0, len);
            }
            out.flush();
            success = true;
        } catch (IOException | RuntimeException ex) {
            safeDelete(tmp);
            throw ex;
        } finally {
            if (!success) safeDelete(tmp);
        }
        try {
            logger.log(Level.INFO, "Tmp for {0}{1} has been created, start move", new  Object[]{p1,p2});
            Files.move(tmp, target, ATOMIC_MOVE);
        } catch (IOException moveEx) {
            if (Files.notExists(target)) Files.move(tmp, target);
            else {
                logger.log(Level.WARNING, "Move failed for {0}", new  Object[]{p1,p2});
                safeDelete(tmp);
                throw moveEx;
            }
        }
        logger.log(Level.INFO, "Move complete");

        return p1 + "/" + p2 + "/" + fileName;
    }

    @Override
    public InputStream open(String storedKey) throws IOException {
        Path p = resolveKey(storedKey);
        if (!Files.isRegularFile(p)) throw new NoSuchFileException("not found: " + storedKey);
        return Files.newInputStream(p, READ);
    }

    @Override
    public boolean delete(String storedKey) throws IOException {
        return Files.deleteIfExists(resolveKey(storedKey));
    }

    @Override
    public boolean exists(String storedKey) {
        try { return Files.isRegularFile(resolveKey(storedKey)); }
        catch (IllegalArgumentException ex) { return false; }
    }

    private static void safeDelete(Path p){ try { Files.deleteIfExists(p); } catch (Exception ignore) {} }

    private Path resolveKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches())
            throw new IllegalArgumentException("bad storedKey format");
        String[] parts = key.split("/");
        Path p = root.resolve(parts[0]).resolve(parts[1]).resolve(parts[2]).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("bad storedKey (traversal)");
        return p;
    }
}

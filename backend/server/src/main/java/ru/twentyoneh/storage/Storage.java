package ru.twentyoneh.storage;

import java.io.IOException;
import java.io.InputStream;

public interface Storage {
    String store(InputStream in, long size, String suggestedExt) throws IOException; // returns storedKey
    InputStream open(String storedKey) throws IOException;
    boolean delete(String storedKey) throws IOException;
    boolean exists(String storedKey);
}

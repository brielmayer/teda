package com.brielmayer.teda.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

public final class ResourceReader {

    private ResourceReader() {}

    /**
     * Resolves a resource to a filesystem path. Used for the CSV parser, which
     * reads a directory of files rather than a single stream.
     */
    public static Path asPath(String fileName) {
        URL resource = ClassLoader.getSystemClassLoader().getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + fileName);
        }
        try {
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Resource is not a valid path: " + fileName, e);
        }
    }

    public static InputStream asInputStream(String fileName) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    public static String asString(String fileName) {
        try (InputStream is = asInputStream(fileName)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + fileName);
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

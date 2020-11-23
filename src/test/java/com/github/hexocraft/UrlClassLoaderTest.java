package com.github.hexocraft;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class UrlClassLoaderTest {

    private static Path tmpDir;
    private static List<DMRelocator.Repository> repositories;

    @BeforeAll
    static void init() throws IOException {
        // Temporary folder
        tmpDir = Files.createTempDirectory("DMR_");
        // Maven central repository
        repositories = new LinkedList<>(Collections.singletonList(new DMRelocator.Repository(new URL("https://repo1.maven.org/maven2/")).name("Maven Central")));
    }

    @AfterAll
    static void end() {
        Assertions.assertThrows(IOException.class, () -> {
            DMRelocator.Downloader.deleteDir(tmpDir);
        });
    }

    @Test
    void AddToClassLoader() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.Artifact artifact = new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6");
            DMRelocator.Downloader.download(artifact, repositories, tmpDir);
            DMRelocator.UrlClassLoader.addToClassLoader(UrlClassLoaderTest.class.getClassLoader(), tmpDir.resolve(artifact.toPath()).toFile());

            Assertions.assertDoesNotThrow(() -> { Class.forName("com.google.gson.Gson"); });
        });
    }
}
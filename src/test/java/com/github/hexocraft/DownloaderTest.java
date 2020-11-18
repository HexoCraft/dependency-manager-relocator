package com.github.hexocraft;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


class DownloaderTest {

    private static Path tmpDir;
    private static List<DMRelocator.Repository> repositories;

    @BeforeAll
    static void init() throws IOException {
        // Restore default values
        DMRelocator.forceDownload = false;
        DMRelocator.forceRelocate = false;
        DMRelocator.flattenDownloadDir = false;
        DMRelocator.flattenRelocateDir = false;
        // Temporary folder
        tmpDir = Files.createTempDirectory("DMR_");
        Assertions.assertTrue(tmpDir.toFile().exists());
        // Maven central repository
        repositories = new LinkedList<>(Arrays.asList(
                new DMRelocator.Repository(new URL("https://repo.maven.org/maven2/")).name("Fake repository")
                , new DMRelocator.Repository(new URL("https://repo1.maven.org/maven2/")).name("Maven Central")));
    }

    @AfterAll
    static void end() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.Downloader.deleteDir(tmpDir);
            Assertions.assertFalse(tmpDir.toFile().exists());
        });
    }

    @Test
    void ArtifactFromUrl() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar");
            DMRelocator.Artifact artifact = new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6").url(url);

            DMRelocator.Downloader.download(artifact, null, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromUrlWithSha1() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar");
            DMRelocator.Artifact artifact = new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6").url(url).sha1("9180733b7df8542621dc12e21e87557e8c99b8cb");

            DMRelocator.Downloader.download(artifact, null, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromRepositories() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.Artifact artifact = new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6");

            DMRelocator.Downloader.download(artifact, repositories, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromRepositoriesFlatDir() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.flattenDownloadDir = true;

            DMRelocator.Artifact artifact = new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6");
            Assertions.assertEquals(tmpDir.resolve(artifact.toPath(true)), tmpDir.resolve("gson-2.8.6.jar"));

            DMRelocator.Downloader.download(artifact, repositories, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath(true)).toFile().exists());
        });
    }

}
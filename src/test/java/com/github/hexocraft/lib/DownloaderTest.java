package com.github.hexocraft.lib;

import com.github.hexocraft.lib.DMRelocator.Artifact;
import com.github.hexocraft.lib.DMRelocator.Downloader;
import com.github.hexocraft.lib.DMRelocator.Repository;
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

    private static Path tmpFolder;
    private static List<Repository> repositories;

    @BeforeAll
    static void init() throws IOException {
        // Temporary folder
        tmpFolder = Files.createTempDirectory("DMR_");
        Assertions.assertTrue(tmpFolder.toFile().exists());
        // Maven central repository
        repositories = new LinkedList<>(Arrays.asList(
                new Repository(new URL("https://repo.maven.org/maven2/")).name("Fake repository")
                , new Repository(new URL("https://repo1.maven.org/maven2/")).name("Maven Central")));
    }

    @AfterAll
    static void end() {
        Assertions.assertDoesNotThrow(() -> {
            Downloader.deleteDir(tmpFolder);
            Assertions.assertFalse(tmpFolder.toFile().exists());
        });
    }

    @Test
    void ArtifactFromUrl() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar");
            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6").url(url);

            Downloader.download(artifact, null, tmpFolder);

            Assertions.assertTrue(tmpFolder.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromUrlWithSha1() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar");
            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6").url(url).sha1("9180733b7df8542621dc12e21e87557e8c99b8cb");

            Downloader.download(artifact, null, tmpFolder);

            Assertions.assertTrue(tmpFolder.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromRepositories() {
        Assertions.assertDoesNotThrow(() -> {
            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6");

            Downloader.download(artifact, repositories, tmpFolder);

            Assertions.assertTrue(tmpFolder.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromRepositoriesFlatDir() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.useFlatDir = true;

            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6");
            Assertions.assertEquals(tmpFolder.resolve(artifact.toPath()), tmpFolder.resolve("gson-2.8.6.jar"));

            Downloader.download(artifact, repositories, tmpFolder);

            Assertions.assertTrue(tmpFolder.resolve(artifact.toPath()).toFile().exists());
        });
    }

}
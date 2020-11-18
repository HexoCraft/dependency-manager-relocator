package com.github.hexocraft;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class RelocatorTest {

    private static final Path cacheDir = Paths.get("target", "relocator", "cache");
    private static final Path libDir = Paths.get("target", "relocator", "libs");

    @BeforeAll
    static void init() {
        // Restore default values
        DMRelocator.forceDownload = false;
        DMRelocator.forceRelocate = false;
        DMRelocator.flattenDownloadDir = false;
        DMRelocator.flattenRelocateDir = false;
    }

    @AfterAll
    static void end() {
    }

    @Test
    void Relocate() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.forceDownload = true;
            DMRelocator.forceRelocate = true;
            DMRelocator.flattenDownloadDir = false;
            DMRelocator.flattenRelocateDir = true;

            DMRelocator.Relocator(RelocatorTest.class.getClassLoader())
                    .cacheDir(cacheDir)
                    .libDir(libDir)
                    .addArtifact(new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6"))
                    .addArtifact(new DMRelocator.Artifact("org.apache.commons", "commons-lang3", "3.11").name("apache.commons.lang3-3.11"))
                    .addRelocation(new DMRelocator.Relocation("com.google.gson", "com.github.hexocraft.lib.libs.gson"))
                    .addRelocation(new DMRelocator.Relocation("org.apache.commons.lang3", "com.github.hexocraft.lib.libs.commons-lang3"))
                    .relocate();

            Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName("com.google.gson.Gson"));
            Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.commons.lang3.ClassUtils"));
            Assertions.assertDoesNotThrow(() -> Class.forName("com.github.hexocraft.lib.libs.gson.Gson"));
            Assertions.assertDoesNotThrow(() -> Class.forName("com.github.hexocraft.lib.libs.commons-lang3.ClassUtils"));
        });
    }
}
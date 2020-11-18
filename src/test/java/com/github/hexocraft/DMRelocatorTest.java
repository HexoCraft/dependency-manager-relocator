package com.github.hexocraft;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static com.github.hexocraft.DMRelocator.Relocator;

class DMRelocatorTest {

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
    void AddRepository() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = DMRelocator.Relocator(DMRelocatorTest.class.getClassLoader()).addRepository(new DMRelocator.Repository(new URL("https://repo.hexocube.fr/release")));
            Assertions.assertNotNull(relocator.getRepositories().stream().filter(r -> r.toString().equals("https://repo.hexocube.fr/release")).map(DMRelocator.Repository::toString).findFirst().orElse(null));
        });
    }

    @Test
    void AddArtifact() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = DMRelocator.Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("com.google.com.gson", "gson", "2.8.6"));
            Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("com.google.com.gson:gson:2.8.6")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
        });
    }

    @Test
    void AsmVersion() {
        DMRelocator relocator = DMRelocator.Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("org.ow2.asm", "asm", "9.0"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("org.ow2.asm:asm:9.0")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
    }

    @Test
    void AsmCommonsVersion() {
        DMRelocator relocator = DMRelocator.Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("org.ow2.asm", "asm-commons", "9.0"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("org.ow2.asm:asm-commons:9.0")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
    }

    @Test
    void JarRelocatorVersion() {
        DMRelocator relocator = DMRelocator.Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("me.lucko", "jar-relocator", "1.3"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("me.lucko:jar-relocator:1.3")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
    }
}
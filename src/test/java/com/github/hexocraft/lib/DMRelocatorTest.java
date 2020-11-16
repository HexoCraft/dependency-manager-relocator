package com.github.hexocraft.lib;

import com.github.hexocraft.lib.DMRelocator.Artifact;
import com.github.hexocraft.lib.DMRelocator.Repository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static com.github.hexocraft.lib.DMRelocator.Relocator;

class DMRelocatorTest {

    @BeforeAll
    static void init() {
    }

    @AfterAll
    static void end() {
    }

    @Test
    void AddRepository() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = Relocator(DMRelocatorTest.class.getClassLoader()).addRepository(new Repository(new URL("https://repo.hexocube.fr/release")));
            Assertions.assertNotNull(relocator.getRepositories().stream().filter(r -> r.toString().equals("https://repo.hexocube.fr/release")).map(Repository::toString).findFirst().orElse(null));
        });
    }

    @Test
    void AddArtifact() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new Artifact("com.google.com.gson", "gson", "2.8.6"));
            Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("com.google.com.gson:gson:2.8.6")).map(Artifact::toString).findFirst().orElse(null));
        });
    }

    @Test
    void AsmVersion() {
        DMRelocator relocator = Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new Artifact("org.ow2.asm", "asm", "9.0"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("org.ow2.asm:asm:9.0")).map(Artifact::toString).findFirst().orElse(null));
    }

    @Test
    void AsmCommonsVersion() {
        DMRelocator relocator = Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new Artifact("org.ow2.asm", "asm-commons", "9.0"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("org.ow2.asm:asm-commons:9.0")).map(Artifact::toString).findFirst().orElse(null));
    }

    @Test
    void JarRelocatorVersion() {
        DMRelocator relocator = Relocator(DMRelocatorTest.class.getClassLoader()).addArtifact(new Artifact("me.lucko", "jar-relocator", "1.3"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("me.lucko:jar-relocator:1.3")).map(Artifact::toString).findFirst().orElse(null));
    }
}
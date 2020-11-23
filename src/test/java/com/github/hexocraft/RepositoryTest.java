package com.github.hexocraft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static com.github.hexocraft.DMRelocator.Relocator;
import static com.github.hexocraft.DMRelocator.Repository;

class RepositoryTest {

    @BeforeAll
    static void init() {
    }

    @Test
    void AddRepository() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = Relocator(AttifactTest.class.getClassLoader()).addRepository(new Repository(new URL("https://repo.hexocube.fr/release")));
            Assertions.assertNotNull(relocator.getRepositories().stream().filter(r -> r.toString().equals("https://repo.hexocube.fr/release")).map(Repository::toString).findFirst().orElse(null));
        });
    }

    @Test
    void MavenCentralRepositoryTest() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/");
            Repository repository = new Repository(url).name("Maven Central");

            Assertions.assertEquals(repository.name(), "Maven Central");
            Assertions.assertTrue(repository.isRemote());
            Assertions.assertFalse(repository.isLocale());
        });
    }

}
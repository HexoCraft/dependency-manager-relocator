package com.github.hexocraft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

class RepositoryTest {

    @BeforeAll
    static void init() {
        // Restore default values
        DMRelocator.forceDownload = false;
        DMRelocator.forceRelocate = false;
        DMRelocator.flattenDownloadDir = false;
        DMRelocator.flattenRelocateDir = false;
    }

    @Test
    void MavenCentralRepositoryTest() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/");
            DMRelocator.Repository repository = new DMRelocator.Repository(url).name("Maven Central");

            Assertions.assertEquals(repository.name(), "Maven Central");
            Assertions.assertTrue(repository.isRemote());
            Assertions.assertFalse(repository.isLocale());
        });
    }

}
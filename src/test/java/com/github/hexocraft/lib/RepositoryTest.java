package com.github.hexocraft.lib;

import com.github.hexocraft.lib.DMRelocator.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;

class RepositoryTest {

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
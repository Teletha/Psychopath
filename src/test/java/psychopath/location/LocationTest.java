/*
 * Copyright (C) 2019 psychopath Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package psychopath.location;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import antibug.Code;
import psychopath.Directory;
import psychopath.File;
import psychopath.LocationTestHelper;
import psychopath.Locator;

class LocationTest extends LocationTestHelper {

    @Test
    void asFile() {
        assert locateAbsent("absent").asFile() instanceof File;
        assert locateFile("file").asFile() instanceof File;
        assertThrows(IllegalStateException.class, () -> locateDirectory("not file").asFile());
    }

    @Test
    void asDirectory() {
        assert locateAbsent("absent").asDirectory() instanceof Directory;
        assert locateDirectory("directory").asDirectory() instanceof Directory;
        assertThrows(IllegalStateException.class, () -> locateFile("not directory").asDirectory());
    }

    @Test
    void fileByString() {
        // relative
        assert Locator.file("test.file").isAbsent();
        assert Locator.file("noExtensionFile").isAbsent();
        assert Locator.file("inDirectory/file").isAbsent();

        // absolute
        String base = locateAbsoluteAbsentDirectory("test").toString();
        assert Locator.file(base + "/test.file").isAbsent();
        assert Locator.file(base + "/noExtensionFile").isAbsent();
        assert Locator.file(base + "/inDirectory/file").isAbsent();

        // abnormal
        assert Code.rejectEmptyArgs(Locator::file);
    }

    @Test
    void fileByPath() {
        // relative absent
        assert Locator.file(Paths.get("test.file")).isAbsent();
        assert Locator.file(Paths.get("noExtensionFile")).isAbsent();
        assert Locator.file(Paths.get("inDirectory/file")).isAbsent();

        // absolute absent
        Path base = locateAbsoluteAbsentDirectory("test").asJavaPath();
        assert Locator.file(base.resolve("test.file")).isAbsent();
        assert Locator.file(base.resolve("noExtensionFile")).isAbsent();
        assert Locator.file(base.resolve("inDirectory/file")).isAbsent();

        // abnormal
        assert Code.rejectEmptyArgs(Locator::file);
    }

    @Test
    void asJavaPath() {
        Path path = Locator.file("test").asJavaPath();
        assert path.isAbsolute() == false;
        assert path.getFileName().toString().equals("test");
        assert path.getParent() == null;
    }

    @Test
    void asJavaFile() {
        java.io.File file = Locator.file("test").asJavaFile();
        assert file.exists() == false;
        assert file.isAbsolute() == false;
        assert file.getName().equals("test");
        assert file.getParentFile() == null;
    }

    @Test
    void match() {
        assert Locator.file("file").match("file") == true;
        assert Locator.file("file").match("fil?") == true;
        assert Locator.file("dir/file").match("*/file") == true;
        assert Locator.file("file").match("**") == true;

        assert Locator.file("file").match("not") == false;
        assert Locator.file("file").match("!not") == true;
        assert Locator.file("dir/file").match("not/**") == false;
        assert Locator.file("dir/file").match("!not/**") == true;
    }
}

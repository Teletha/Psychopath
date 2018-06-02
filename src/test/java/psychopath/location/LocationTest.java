/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package psychopath.location;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CleanRoom;
import antibug.Code;
import psychopath.Locator;

/**
 * @version 2018/05/31 8:42:45
 */
class LocationTest {

    @RegisterExtension
    static final CleanRoom room = new CleanRoom();

    @Test
    void fileByString() {
        // relative
        assert Locator.file("test.file").isAbsent();
        assert Locator.file("noExtensionFile").isAbsent();
        assert Locator.file("inDirectory/file").isAbsent();

        // absolute
        String base = room.root.toAbsolutePath().toString();
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
        Path base = room.root.toAbsolutePath();
        assert Locator.file(base.resolve("test.file")).isAbsent();
        assert Locator.file(base.resolve("noExtensionFile")).isAbsent();
        assert Locator.file(base.resolve("inDirectory/file")).isAbsent();

        // abnormal
        assert Code.rejectEmptyArgs(Locator::file);
    }

    @Test
    void asPath() {
        Path path = Locator.file("test").asPath();
        assert path.isAbsolute() == false;
        assert path.getFileName().toString().equals("test");
        assert path.getParent() == null;
    }

    @Test
    void asFile() {
        java.io.File file = Locator.file("test").asFile();
        assert file.exists() == false;
        assert file.isAbsolute() == false;
        assert file.getName().equals("test");
        assert file.getParentFile() == null;
    }
}

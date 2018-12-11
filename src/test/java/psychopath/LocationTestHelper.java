/*
 * Copyright (C) 2018 psychopath Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package psychopath;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CleanRoom;
import antibug.CleanRoom.FileSystemDSL;

/**
 * @version 2018/12/10 12:32:53
 */
public class LocationTestHelper {

    @RegisterExtension
    CleanRoom room = new CleanRoom();

    /**
     * Build file tree structure.
     * 
     * @param name A file name.
     * @return
     */
    public final File locateAbsent(String name) {
        return Locator.file(room.locateAbsent(name));
    }

    /**
     * Build file tree structure.
     * 
     * @param name A file name.
     * @return
     */
    public final Directory locateAbsentDirectory(String name) {
        return Locator.directory(room.locateAbsent(name));
    }

    /**
     * Build file tree structure.
     * 
     * @param name A file name.
     * @return
     */
    public final File locateFile(String name) {
        return Locator.file(room.locateFile(name));
    }

    /**
     * Build file tree structure.
     * 
     * @param name A file name.
     * @param content A content.
     * @return
     */
    public final File locateFile(String name, String content) {
        return Locator.file(room.locateFile(name, content));
    }

    /**
     * Build file tree structure.
     * 
     * @param name A directory name.
     * @return
     */
    public final Directory locateDirectory(String name) {
        return Locator.directory(room.locateDirectory(name));
    }

    /**
     * Build file tree structure.
     * 
     * @param name A directory name.
     * @param dsl A definition of file tree.
     * @return
     */
    public final Directory locateDirectory(String name, Consumer<FileSystemDSL> dsl) {
        return Locator.directory(room.locateDirectory(name, dsl));
    }

    /**
     * Test matching file tree structure.
     * 
     * @param actual An actual directory.
     * @param expected An expected directory structure.
     * @return
     */
    public final boolean match(Directory actual, Consumer<FileSystemDSL> expected) {
        Directory e = locateDirectory(actual.name() + " expected", expected);

        List<Location<?>> actuals = actual.descendant().toList();
        List<Location<?>> expecteds = e.descendant().toList();
        System.out.println(actuals);
        System.out.println(expecteds);
        assertIterableEquals(actuals, expecteds);

        return true;
    }
}

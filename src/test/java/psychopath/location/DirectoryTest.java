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

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import antibug.CleanRoom;
import antibug.CleanRoom.FileSystemDSL;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

/**
 * @version 2018/04/08 15:21:41
 */
public class DirectoryTest {

    @RegisterExtension
    static final CleanRoom room = new CleanRoom();

    private Directory current;

    @BeforeEach
    void setup() {
        current = null;
    }

    @Test
    void name() {
        // relative
        assert relative("name").name().equals("name");
        assert relative("nest/name").name().equals("name");
        assert relative("root/nest/name").name().equals("name");

        // absolute
        assert absolute("name").name().equals("name");
        assert absolute("nest/name").name().equals("name");
        assert absolute("root/nest/name").name().equals("name");
    }

    @Test
    void findDirectorySingle() {
        define($ -> {
            $.dir("dir1");
        });

        assert findDirectory() == 1;
    }

    @Test
    void fineDirectoryMultiple() {
        define($ -> {
            $.dir("dir1");
            $.dir("dir2");
            $.dir("dir3");
        });

        assert findDirectory() == 3;
    }

    @Test
    void fineDirectoryNest() {
        define($ -> {
            $.dir("dir1", () -> {
                $.dir("nest11");
                $.dir("nest12");
                $.file("fileA1");
                $.file("fileA2");
            });
            $.dir("dir2", () -> {
                $.dir("nest21");
                $.dir("nest22");
                $.file("fileB1");
                $.file("fileB2");
            });
            $.file("fileC");
            $.file("fileC");
        });

        assert findDirectory() == 6;
        assert findDirectory("*") == 2;
        assert findDirectory("*/*") == 4;
        assert findDirectory("**") == 6;
        assert findDirectory("*", "*/*1") == 4;
    }

    /**
     * Helper to locate {@link File}.
     * 
     * @param path
     * @return
     */
    private File absolute(String path) {
        return Locator.file(path).absolutize();
    }

    /**
     * Helper to locate {@link Directory}.
     * 
     * @param path
     * @return
     */
    private Directory relative(String path) {
        return Locator.directory(path);
    }

    /**
     * Helper.
     * 
     * @param definition
     * @return
     */
    private Directory define(Consumer<FileSystemDSL> definition) {
        room.with(definition);

        return current = Locator.directory(room.root);
    }

    /**
     * Compute directory size.
     * 
     * @param patterns
     * @return
     */
    private int findDirectory(String... patterns) {
        return current.directories(patterns).toList().size();
    }
}

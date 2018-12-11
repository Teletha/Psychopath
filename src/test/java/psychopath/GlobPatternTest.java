/*
 * Copyright (C) 2017 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package psychopath;

import org.junit.jupiter.api.Test;

/**
 * @version 2018/03/31 3:01:30
 */
class GlobPatternTest extends LocationTestHelper {

    @Test
    void topLevelWildcard() {
        Directory root = locateDirectory("root", $ -> {
            $.file("file");
            $.file("text");
            $.dir("dir", () -> {
                $.file("file");
                $.file("text");
            });
        });

        assert root.walkFiles("*").toList().size() == 2;
        assert root.walkFiles("*/text").toList().size() == 1;
        assert root.walkFiles("*", "*/text").toList().size() == 3;
    }

    @Test
    void secondLevelWildcard() {
        Directory root = locateDirectory("root", $ -> {
            $.file("file");
            $.dir("dir1", () -> {
                $.file("file1");
                $.file("file2");
                $.file("text1");
            });
            $.dir("dir2", () -> {
                $.file("file1");
                $.file("file2");
                $.file("text1");
            });
        });

        assert root.walkFiles("*/*").toList().size() == 6;
        assert root.walkFiles("*/file*").toList().size() == 4;
        assert root.walkFiles("*/*1").toList().size() == 4;
    }

    @Test
    void character() {
        Directory root = locateDirectory("root", $ -> {
            $.file("text1");
            $.file("text2");
            $.dir("dir", () -> {
                $.file("text1");
                $.file("text2");
            });
        });

        assert root.walkFiles("text?").toList().size() == 2;
        assert root.walkFiles("????1").toList().size() == 1;
        assert root.walkFiles("**text?").toList().size() == 4;
    }

    @Test
    void range() {
        Directory root = locateDirectory("root", $ -> {
            $.file("text1");
            $.file("text2");
            $.file("text3");
            $.file("text4");
        });

        assert root.walkFiles("text[1-2]").toList().size() == 2;
        assert root.walkFiles("text[2-5]").toList().size() == 3;
    }

    @Test
    void negate() {
        Directory root = locateDirectory("root", $ -> {
            $.file("text1");
            $.file("text2");
            $.file("text3");
            $.file("text4");
        });

        assert root.walkFiles("text[!3]").toList().size() == 3;
        assert root.walkFiles("text[!34]").toList().size() == 2;
    }

    @Test
    void excludeFile() {
        Directory root = locateDirectory("root", $ -> {
            $.file("text1");
            $.file("text2");
            $.file("text3");
            $.file("text4");
        });
        assert root.walkFiles("**", "!text1").toList().size() == 3;
    }

    @Test
    void excludeDirectory() {
        Directory root = locateDirectory("root", $ -> {
            $.file("text1");
            $.file("text2");
            $.file("text3");
            $.file("text4");

            $.dir("ignore", () -> {
                $.file("text1");
                $.file("text2");
                $.file("text3");
                $.file("text4");
            });
        });
        assert root.walkFiles("**", "!ignore/**").toList().size() == 4;
    }

    @Test
    void multiple() {
        Directory root = locateDirectory("root", $ -> {
            $.file("text1");
            $.file("text2");
            $.file("text3");
            $.file("text4");
        });
        assert root.walkFiles("**", "!**1", "!**3").toList().size() == 2;
    }
}

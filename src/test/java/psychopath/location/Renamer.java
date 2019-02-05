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

import java.time.ZonedDateTime;

import psychopath.Directory;
import psychopath.Location;
import psychopath.Locator;

/**
 * @version 2018/07/17 23:06:20
 */
public class Renamer {

    public static void main(String[] args) {
        Directory root = Locator.directory("e:\\");

        root.walkFile("*.zip").take(5).effectOnComplete(Location::delete).to(archive -> {
            archive.unpackTo(root.directory(archive.base()))
                    .children()
                    .single()
                    .as(Directory.class)
                    .effectOnComplete(Location::delete)
                    .flatMap(Location::children)
                    .to(file -> file.lastModified(ZonedDateTime.now()).moveUp());
        });
    }
}

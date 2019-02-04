/*
 * Copyright (C) 2019 psychopath Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package psychopath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import kiss.I;
import kiss.Signal;
import kiss.Ⅱ;

/**
 * Virtual directory to manage resources from various entries.
 */
public final class Folder {

    /** The operations. */
    private final List<Operation> operations = new ArrayList();

    /**
     * 
     */
    Folder() {
    }

    /**
     * Add entry by path expression.
     * 
     * @param entry A path to entry.
     * @return Chainable API.
     */
    public Folder add(String entry) {
        return add(entry, Function.identity());
    }

    /**
     * Add entry by path expression.
     * 
     * @param entry A path to entry.
     * @return Chainable API.
     */
    public Folder add(String entry, Function<Option, Option> option) {
        if (entry == null) {
            return this;
        }
        return add(Locator.locate(entry), option);
    }

    /**
     * Add entry by {@link Path}.
     * 
     * @param entry A path to entry.
     * @return Chainable API.
     */
    public Folder add(Path entry) {
        return add(entry, Function.identity());
    }

    /**
     * Add entry by {@link Path}.
     * 
     * @param entry A path to entry.
     * @return Chainable API.
     */
    public Folder add(Path entry, Function<Option, Option> option) {
        if (entry == null) {
            return this;
        }
        return add(Locator.locate(entry), option);
    }

    /**
     * Merge entries from other {@link Folder}.
     * 
     * @param entries A entries to merge.
     * @return Chainable API.
     */
    public Folder add(Folder entries) {
        if (entries != null) {
            operations.addAll(entries.operations);
        }
        return this;
    }

    /**
     * Merge entries from other {@link Folder}.
     * 
     * @param entries A entries to merge.
     * @return Chainable API.
     */
    public Folder add(Folder entries, Function<Option, Option> option) {
        if (entries != null) {
            for (Operation operation : entries.operations) {
                this.operations.add(new LayerOperation(operation, option));
            }
        }
        return this;
    }

    /**
     * Add entries by {@link Location}.
     * 
     * @param entry A location to entry.
     * @return Chainable API.
     */
    public Folder add(Location entry) {
        return add(I.signal(entry), Function.identity());
    }

    /**
     * Add entries by {@link Location}.
     * 
     * @param entry A location to entry.
     * @return Chainable API.
     */
    public Folder add(Location entry, Function<Option, Option> option) {
        return add(I.signal(entry), option);
    }

    /**
     * Add entries.
     * 
     * @param entries
     */
    public Folder add(Signal<? extends Location> entries) {
        return add(entries, Function.identity());
    }

    /**
     * Build {@link Operation} for {@link Location}.
     * 
     * @param entries
     * @param option
     * @return
     */
    private Signal<Operation> buildOperation(Signal<? extends Location> entries, Function<Option, Option> option) {
        return entries.map(entry -> {
            if (entry.isDirectory()) {
                return new DirectoryOperation((Directory) entry, option);
            } else {
                return new FileOperation((File) entry);
            }
        });
    }

    /**
     * Add entries.
     * 
     * @param entries
     */
    public Folder add(Signal<? extends Location> entries, Function<Option, Option> option) {
        if (entries != null) {
            operations.add(new Operation() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Location> delete(String... patterns) {
                    return buildOperation(entries, option).flatMap(op -> op.delete(patterns));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Location> moveTo(Directory destination, Function<Option, Option> option) {
                    return buildOperation(entries, option).flatMap(op -> op.moveTo(destination, option));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Location> copyTo(Directory destination, Function<Option, Option> option) {
                    return buildOperation(entries, option).flatMap(op -> op.copyTo(destination, option));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Location> packTo(ArchiveOutputStream archive, BiFunction<String, File, ArchiveEntry> builder, Directory relative, Function<Option, Option> option) {
                    return buildOperation(entries, option).flatMap(op -> op.packTo(archive, builder, relative, option));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Ⅱ<Directory, File>> walkFiles(String... patterns) {
                    return buildOperation(entries, option).flatMap(op -> op.walkFiles(patterns));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Ⅱ<Directory, Directory>> walkDirectories(String... patterns) {
                    return buildOperation(entries, option).flatMap(op -> op.walkDirectories(patterns));
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public Signal<Location> entry() {
                    return entries.as(Location.class);
                }
            });
        }
        return this;
    }

    /**
     * Add pattern matching path.
     * 
     * @param base A base path.
     * @param patterns "glob" include/exclude patterns.
     */
    public Folder add(Directory base, String... patterns) {
        return add(base, o -> o.glob(patterns));
    }

    /**
     * Add pattern matching path.
     * 
     * @param base A base path.
     * @param patterns "glob" include/exclude patterns.
     */
    public Folder add(Directory base, Function<Option, Option> option) {
        if (base != null) {
            operations.add(new DirectoryOperation(base, option));
        }
        return this;
    }

    /**
     * <p>
     * Use destination relative path for entries.
     * </p>
     * <pre>
     * folder.add("main.jar").add("lib", entry -> entry.add("one.jar").add("other.jar"));
     * folder.copyTo("output");
     * </pre>
     * <p>
     * {@link Folder} will deploy jars into "lib" directory.
     * </p>
     * <pre>
     * output
     * - main.jar
     * - lib
     * - one.jar
     * - other.jar
     * </pre>
     *
     * @param relative A destination relative path.
     * @param entries Your entries.
     * @return
     */
    public Folder addIn(String relative, Consumer<Folder> entries) {
        return addIn(Locator.directory(relative), entries);
    }

    /**
     * <p>
     * Use destination relative path for entries.
     * </p>
     * <pre>
     * folder.add("main.jar").add("lib", entry -> entry.add("one.jar").add("other.jar"));
     * folder.copyTo("output");
     * </pre>
     * <p>
     * {@link Folder} will deploy jars into "lib" directory.
     * </p>
     * <pre>
     * output
     * - main.jar
     * - lib
     * - one.jar
     * - other.jar
     * </pre>
     *
     * @param relative A destination relative path.
     * @param entries Your entries.
     * @return
     */
    public Folder addIn(Path relative, Consumer<Folder> entries) {
        return addIn(Locator.directory(relative), entries);
    }

    /**
     * <p>
     * Use destination relative path for entries.
     * </p>
     * <pre>
     * folder.add("main.jar").add("lib", entry -> entry.add("one.jar").add("other.jar"));
     * folder.copyTo("output");
     * </pre>
     * <p>
     * {@link Folder} will deploy jars into "lib" directory.
     * </p>
     * <pre>
     * output
     * - main.jar
     * - lib
     * - one.jar
     * - other.jar
     * </pre>
     *
     * @param relative A destination relative path.
     * @param entries Your entries.
     * @return
     */
    public Folder addIn(Directory relative, Consumer<Folder> entries) {
        if (entries != null) {
            Folder folder = Locator.folder();
            entries.accept(folder);
            operations.addAll(I.signal(folder.operations).map(o -> new Allocator(o, relative)).toList());
        }
        return this;
    }

    /**
     * Delete all resources.
     * 
     * @return
     */
    public Signal<Location> delete(String... patterns) {
        return I.signal(operations).flatMap(operation -> operation.delete(patterns));
    }

    /**
     * Delete all resources.
     * 
     * @return
     */
    public Folder deleteNow(String... patterns) {
        delete(patterns).to(I.NoOP);

        return this;
    }

    /**
     * Copy all resources to the specified {@link Directory}.
     * 
     * @param destination A destination {@link Directory}.
     */
    public Signal<Location> copyTo(Directory destination, String... patterns) {
        Objects.requireNonNull(destination);

        return I.signal(operations).flatMap(operation -> operation.copyTo(destination, o -> o.glob(patterns)));
    }

    /**
     * Copy all resources to the specified {@link Directory}.
     * 
     * @param destination A destination {@link Directory}.
     */
    public Directory copyToNow(Directory destination, String... patterns) {
        Objects.requireNonNull(destination);

        operations.forEach(operation -> operation.copyToNow(destination, o -> o.glob(patterns)));

        return destination;
    }

    /**
     * Move all resources to the specified {@link Directory}.
     * 
     * @param destination A destination {@link Directory}.
     */
    public Signal<Location> moveTo(Directory destination, String... patterns) {
        Objects.requireNonNull(destination);

        return I.signal(operations).flatMap(operation -> operation.moveTo(destination, o -> o.glob(patterns)));
    }

    /**
     * Move all resources to the specified {@link Directory}.
     * 
     * @param destination A destination {@link Directory}.
     */
    public Directory moveToNow(Directory destination, String... patterns) {
        moveTo(destination, patterns).to(I.NoOP);

        return destination;
    }

    /**
     * Pack all resources.
     * 
     * @param archive
     */
    public Signal<Location> packTo(File archive, String... patterns) {
        return new Signal<>((observer, disposer) -> {
            BiFunction<String, File, ArchiveEntry> builder = detectEntryBuilder(archive.extension());

            try (ArchiveOutputStream out = new ArchiveStreamFactory()
                    .createArchiveOutputStream(archive.extension().replaceAll("7z", "7z-override"), archive.newOutputStream())) {
                I.signal(operations)
                        .flatMap(operation -> operation.packTo(out, builder, Locator.directory(""), o -> o.glob(patterns)))
                        .to(observer);
                out.finish();
                observer.complete();
            } catch (Exception e) {
                observer.error(e);
            }

            return disposer;
        });
    }

    /**
     * Pack all resources.
     * 
     * @param archive
     */
    public File packToNow(File archive, String... patterns) {
        packTo(archive, patterns).to(I.NoOP);

        return archive;
    }

    /**
     * List up all {@link File}s.
     * 
     * @return
     */
    public Signal<File> walkFiles(String... patterns) {
        return walkFilesWithBase(patterns).map(Ⅱ<Directory, File>::ⅱ);
    }

    /**
     * List up all {@link File}s.
     * 
     * @return
     */
    public Signal<Ⅱ<Directory, File>> walkFilesWithBase(String... patterns) {
        return I.signal(operations).flatMap(op -> op.walkFiles(patterns));
    }

    /**
     * List up all {@link Directory}.
     * 
     * @return
     */
    public Signal<Directory> walkDirectories(String... patterns) {
        return walkDirectoriesWithBase(patterns).map(Ⅱ<Directory, Directory>::ⅱ);
    }

    /**
     * List up all {@link Directory}.
     * 
     * @return
     */
    public Signal<Ⅱ<Directory, Directory>> walkDirectoriesWithBase(String... patterns) {
        return I.signal(operations).flatMap(op -> op.walkDirectories(patterns));
    }

    /**
     * List up all entries.
     * 
     * @return
     */
    public Signal<Location> entries() {
        return I.signal(operations).flatMap(Operation::entry);
    }

    /**
     * Build {@link ArchiveEntry} for each resources.
     * 
     * @param out
     * @param directory
     * @param file
     * @param relative
     */
    private static Signal<Location> pack(ArchiveOutputStream out, BiFunction<String, File, ArchiveEntry> builder, Directory directory, File file, Directory relative) {
        return new Signal<>((observer, disposer) -> {
            try {
                ArchiveEntry entry = builder.apply(relative.file(directory.relativize(file).toString()).path(), file);
                out.putArchiveEntry(entry);

                try (InputStream in = file.newInputStream()) {
                    in.transferTo(out);
                    observer.accept(file);
                }
                out.closeArchiveEntry();
                observer.complete();
            } catch (IOException e) {
                observer.error(e);
            }
            return disposer;
        });
    }

    private static BiFunction<String, File, ArchiveEntry> detectEntryBuilder(String extension) {
        switch (extension) {
        case "jar":
            return (name, file) -> {
                JarArchiveEntry entry = new JarArchiveEntry(name);

                return entry;
            };

        case "zip":
            return (name, file) -> {
                ZipArchiveEntry entry = new ZipArchiveEntry(name);

                return entry;
            };

        case "7z":
            return (name, file) -> {
                SevenZArchiveEntry entry = new SevenZArchiveEntry();
                entry.setName(name);

                return entry;
            };

        default:
            throw new Error();
        }
    }

    /**
     * Definition of {@link Folder} operation.
     */
    private interface Operation {

        /**
         * Delete resources.
         * 
         * @param patterns
         */
        Signal<Location> delete(String... patterns);

        /**
         * Delete resources.
         * 
         * @param patterns
         */
        default void deleteNow(String... patterns) {
            delete(patterns).to(I.NoOP);
        }

        /**
         * Move reosources to the specified {@link Directory}.
         * 
         * @param destination
         * @param patterns
         */
        Signal<Location> moveTo(Directory destination, Function<Option, Option> option);

        /**
         * Move reosources to the specified {@link Directory}.
         * 
         * @param destination
         * @param patterns
         */
        default void moveToNow(Directory destination, Function<Option, Option> option) {
            moveTo(destination, option).to(I.NoOP);
        }

        /**
         * Copy reosources to the specified {@link Directory}.
         * 
         * @param destination
         * @param patterns
         */
        Signal<Location> copyTo(Directory destination, Function<Option, Option> option);

        /**
         * Copy reosources to the specified {@link Directory}.
         * 
         * @param destination
         * @param patterns
         */
        default void copyToNow(Directory destination, Function<Option, Option> option) {
            copyTo(destination, option).to(I.NoOP);
        }

        /**
         * Pack reosources to the specified {@link File}.
         * 
         * @param relative
         * @param patterns
         */
        Signal<Location> packTo(ArchiveOutputStream archive, BiFunction<String, File, ArchiveEntry> builder, Directory relative, Function<Option, Option> option);

        /**
         * List up all resources.
         * 
         * @param patterns
         * @return
         */
        Signal<Ⅱ<Directory, File>> walkFiles(String... patterns);

        /**
         * List up all resources.
         * 
         * @param patterns
         * @return
         */
        Signal<Ⅱ<Directory, Directory>> walkDirectories(String... patterns);

        Signal<Location> entry();
    }

    /**
     * Allocator for destination path.
     */
    private static class Allocator implements Operation {

        /** The delegation. */
        private final Operation delegator;

        /** The destination relative path. */
        private final Directory relative;

        /**
         * @param delegator
         * @param relative
         */
        private Allocator(Operation delegator, Directory relative) {
            this.delegator = delegator;
            this.relative = relative;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> delete(String... patterns) {
            return delegator.delete(patterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> moveTo(Directory destination, Function<Option, Option> option) {
            return delegator.moveTo(destination.directory(relative), option);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> copyTo(Directory destination, Function<Option, Option> option) {
            return delegator.copyTo(destination.directory(relative), option);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> packTo(ArchiveOutputStream archive, BiFunction<String, File, ArchiveEntry> builder, Directory relative, Function<Option, Option> option) {
            return delegator.packTo(archive, builder, this.relative, option);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, File>> walkFiles(String... patterns) {
            return delegator.walkFiles(patterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, Directory>> walkDirectories(String... patterns) {
            return delegator.walkDirectories(patterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> entry() {
            return delegator.entry();
        }
    }

    /**
     * Operation for {@link File}.
     */
    private static class FileOperation implements Operation {

        private final File file;

        /**
         * @param file
         */
        private FileOperation(File file) {
            this.file = file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> delete(String... patterns) {
            return file.delete();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> moveTo(Directory destination, Function<Option, Option> option) {
            return file.moveTo(destination);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> copyTo(Directory destination, Function<Option, Option> option) {
            return file.copyTo(destination);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> packTo(ArchiveOutputStream archive, BiFunction<String, File, ArchiveEntry> builder, Directory relative, Function<Option, Option> option) {
            return pack(archive, builder, file.parent(), file, relative);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, File>> walkFiles(String... patterns) {
            if (patterns.length == 0 || file.match(patterns)) {
                return I.signal(I.pair(file.parent(), file));
            } else {
                return Signal.empty();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, Directory>> walkDirectories(String... patterns) {
            return Signal.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> entry() {
            return I.signal(file);
        }
    }

    /**
     * Operation for {@link Directory}.
     */
    private static class DirectoryOperation implements Operation {

        private final Directory directory;

        private final Function<Option, Option> option;

        /**
         * @param directory
         * @param option
         */
        private DirectoryOperation(Directory directory, Function<Option, Option> option) {
            this.directory = directory;
            this.option = option;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> delete(String... patterns) {
            return directory.delete(option.andThen(o -> o.glob(patterns)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> moveTo(Directory destination, Function<Option, Option> option) {
            return directory.moveTo(destination, this.option.andThen(option));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> copyTo(Directory destination, Function<Option, Option> option) {
            return directory.copyTo(destination, this.option.andThen(option));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> packTo(ArchiveOutputStream archive, BiFunction<String, File, ArchiveEntry> builder, Directory relative, Function<Option, Option> option) {
            Function<Option, Option> combined = this.option.andThen(option);
            Option o = combined.apply(new Option());

            return directory.walkFiles(combined)
                    .flatMap(file -> pack(archive, builder, !directory.isRoot() && o.acceptRoot ? directory.parent()
                            : directory, file, relative));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, File>> walkFiles(String... patterns) {
            return directory.walkFiles(option.andThen(o -> o.glob(patterns))).map(file -> I.pair(directory, file));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, Directory>> walkDirectories(String... patterns) {
            return directory.walkDirectories(option.andThen(o -> o.glob(patterns))).map(dir -> I.pair(directory, dir));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> entry() {
            return I.signal(directory);
        }
    }

    /**
     * 
     */
    private static class LayerOperation implements Operation {

        private final Operation operation;

        private final Function<Option, Option> option;

        /**
         * @param operation
         * @param option
         */
        private LayerOperation(Operation operation, Function<Option, Option> option) {
            this.operation = operation;
            this.option = option;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> delete(String... patterns) {
            return operation.delete(patterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> moveTo(Directory destination, Function<Option, Option> option) {
            return operation.moveTo(destination, this.option.andThen(option));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> copyTo(Directory destination, Function<Option, Option> option) {
            return operation.copyTo(destination, this.option.andThen(option));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> packTo(ArchiveOutputStream archive, BiFunction<String, File, ArchiveEntry> builder, Directory relative, Function<Option, Option> option) {
            return operation.packTo(archive, builder, relative, this.option.andThen(option));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, File>> walkFiles(String... patterns) {
            return operation.walkFiles(patterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Ⅱ<Directory, Directory>> walkDirectories(String... patterns) {
            return operation.walkDirectories(patterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<Location> entry() {
            return operation.entry();
        }
    }
}

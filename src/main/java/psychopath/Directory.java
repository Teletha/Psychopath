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

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkPermission;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.function.BiPredicate;

import kiss.I;
import kiss.Observer;
import kiss.Signal;
import kiss.WiseRunnable;

/**
 * @version 2018/04/08 12:22:35
 */
public class Directory extends Location<Directory> {

    /**
     * @param path
     */
    Directory(Path path) {
        super(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Directory> asDirectory() {
        return I.signal(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<File> asFile() {
        return Signal.empty();
    }

    public boolean isEmpty() {
        try {
            return Files.list(path).count() == 0;
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Location<?>> children() {
        return new Signal<>((observer, disposer) -> {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                stream.forEach(file -> {
                    if (Files.isDirectory(file)) {
                        observer.accept(Locator.directory(file));
                    } else {
                        observer.accept(Locator.file(file));
                    }
                });
                observer.complete();
            } catch (Exception e) {
                observer.error(e);
            }
            return disposer;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Location<?>> descendant() {
        return I.signal(true, (Location) this, dir -> dir.flatMap(Location<?>::children)).skip(this);
    }

    /**
     * Shortcut for {@link Locator#file(String)}
     * 
     * @param path A file path.
     * @return A located {@link File}.
     */
    public File file(String path) {
        return Locator.file(this, path);
    }

    /**
     * Shortcut for {@link Locator#file(Path)}
     * 
     * @param path A file path.
     * @return A located {@link File}.
     */
    public File file(Path path) {
        return Locator.file(this, path);
    }

    /**
     * Shortcut for {@link Locator#file(Path)}
     * 
     * @param path A file path.
     * @return A located {@link File}.
     */
    public File file(File path) {
        return file(path.path);
    }

    /**
     * Shortcut for {@link Locator#directory(String)}
     * 
     * @param path A directory path.
     * @return A located {@link Directory}.
     */
    public Directory directory(String path) {
        return Locator.directory(this, path);
    }

    /**
     * Shortcut for {@link Locator#directory(Path)}
     * 
     * @param path A directory path.
     * @return A located {@link Directory}.
     */
    public Directory directory(Path path) {
        return Locator.directory(this, path);
    }

    /**
     * Shortcut for {@link Locator#directory(Path)}
     * 
     * @param path A directory path.
     * @return A located {@link Directory}.
     */
    public Directory directory(Directory path) {
        return directory(path.path);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Glob patterns.
     * @return All matched {@link File}s.
     */
    public Signal<Location> walk(String... filters) {
        return walk(Location.class, null, 3, filters, null, Integer.MAX_VALUE, false);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Glob patterns.
     * @return All matched {@link File}s.
     */
    public Signal<File> walkFiles(String... filters) {
        return walk(File.class, null, 3, filters, null, Integer.MAX_VALUE, false);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Your condition.
     * @return All matched {@link File}s.
     */
    public Signal<File> walkFiles(BiPredicate<Path, BasicFileAttributes> filters) {
        return walk(File.class, null, 3, null, filters, Integer.MAX_VALUE, false);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Your condition.
     * @param depth A max file tree depth to search.
     * @return All matched {@link File}s.
     */
    public Signal<File> walkFiles(BiPredicate<Path, BasicFileAttributes> filters, int depth) {
        return walk(File.class, null, 3, null, filters, depth, false);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Glob patterns.
     * @return All matched {@link File}s.
     */
    public Signal<Directory> walkDirectories(String... filters) {
        return walk(Directory.class, null, 4, filters, null, Integer.MAX_VALUE, false).skip(this);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Your condition.
     * @return All matched {@link File}s.
     */
    public Signal<Directory> walkDirectories(BiPredicate<Path, BasicFileAttributes> filters) {
        return walk(Directory.class, null, 4, null, filters, Integer.MAX_VALUE, false).skip(this);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param filters Your condition.
     * @param depth A max file tree depth to search.
     * @return All matched {@link File}s.
     */
    public Signal<Directory> walkDirectories(BiPredicate<Path, BasicFileAttributes> filters, int depth) {
        return walk(Directory.class, null, 4, null, filters, depth, false).skip(this);
    }

    /**
     * Walk file tree and collect {@link File}s which are filtered by various conditions.
     * 
     * @param type Scan type.
     * @param patterns Glob patterns.
     * @param filters Your condition.
     * @param depth A max file tree depth to search.
     * @return All matched {@link File}s.
     */
    private <L extends Location> Signal<L> walk(Class<L> clazz, Path out, int type, String[] patterns, BiPredicate<Path, BasicFileAttributes> filters, int depth, boolean relatively) {
        return new Signal<L>((observer, disposer) -> {
            // build new scanner
            CymaticScan scanner;

            if (filters == null) {
                scanner = new CymaticScan(path, out, type, observer, disposer, patterns);
            } else {
                scanner = new CymaticScan(path, out, type, observer, disposer, filters);
            }

            // try to scan
            try {
                Files.walkFileTree(path, Collections.EMPTY_SET, depth, scanner);
                observer.complete();
            } catch (IOException e) {
                observer.error(e);
            }
            return disposer;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveTo(Directory destination) {
        moveTo(destination, (BiPredicate) null);
    }

    /**
     * <p>
     * Move a input {@link Path} to an output {@link Path} with its attributes. Simplified strategy
     * is the following:
     * </p>
     * <p>
     * <pre>
     * if (input.isFile) {
     *   if (output.isFile) {
     *     // Move input file to output file.
     *   } else {
     *     // Move input file under output directory.
     *   }
     * } else {
     *   if (output.isFile) {
     *     // NoSuchFileException will be thrown.
     *   } else {
     *     // Move input directory under output directory deeply.
     *     // You can also specify <a href="#Patterns">include/exclude patterns</a>.
     *   }
     * }
     * </pre>
     * <p>
     * If the output file already exists, it will be replaced by input file unconditionaly. The
     * exact file attributes that are copied is platform and file system dependent and therefore
     * unspecified. Minimally, the last-modified-time is copied to the output file if supported by
     * both the input and output file store. Copying of file timestamps may result in precision
     * loss.
     * </p>
     * <p>
     * Moving a file is an atomic operation.
     * </p>
     *
     * @param input A input {@link Path} object which can be file or directory.
     * @param output An output {@link Path} object which can be file or directory.
     * @param filter A file filter to move.
     * @throws IOException If an I/O error occurs.
     * @throws NullPointerException If the specified input or output file is <code>null</code>.
     * @throws NoSuchFileException If the input file is directory and the output file is
     *             <em>not</em> directory.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public void moveTo(Directory destination, String... patterns) {
        walk(Location.class, destination.path, 1, patterns, null, Integer.MAX_VALUE, false).to(I.NoOP);
    }

    /**
     * <p>
     * Move a input {@link Path} to an output {@link Path} with its attributes. Simplified strategy
     * is the following:
     * </p>
     * <p>
     * <pre>
     * if (input.isFile) {
     *   if (output.isFile) {
     *     // Move input file to output file.
     *   } else {
     *     // Move input file under output directory.
     *   }
     * } else {
     *   if (output.isFile) {
     *     // NoSuchFileException will be thrown.
     *   } else {
     *     // Move input directory under output directory deeply.
     *     // You can also specify <a href="#Patterns">include/exclude patterns</a>.
     *   }
     * }
     * </pre>
     * <p>
     * If the output file already exists, it will be replaced by input file unconditionaly. The
     * exact file attributes that are copied is platform and file system dependent and therefore
     * unspecified. Minimally, the last-modified-time is copied to the output file if supported by
     * both the input and output file store. Copying of file timestamps may result in precision
     * loss.
     * </p>
     * <p>
     * Moving a file is an atomic operation.
     * </p>
     *
     * @param input A input {@link Path} object which can be file or directory.
     * @param output An output {@link Path} object which can be file or directory.
     * @param filter A file filter to move.
     * @throws IOException If an I/O error occurs.
     * @throws NullPointerException If the specified input or output file is <code>null</code>.
     * @throws NoSuchFileException If the input file is directory and the output file is
     *             <em>not</em> directory.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public void moveTo(Directory destination, BiPredicate<Path, BasicFileAttributes> filter) {
        walk(Location.class, destination.path, 1, null, filter, Integer.MAX_VALUE, false).to(I.NoOP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyTo(Directory destination) {
        copyTo(destination, (BiPredicate) null);
    }

    /**
     * <p>
     * Copy a input {@link Path} to the output {@link Path} with its attributes. Simplified strategy
     * is the following:
     * </p>
     * <p>
     * <pre>
     * if (input.isFile) {
     *   if (output.isFile) {
     *     // Copy input file to output file.
     *   } else {
     *     // Copy input file to output directory.
     *   }
     * } else {
     *   if (output.isFile) {
     *     // NoSuchFileException will be thrown.
     *   } else {
     *     // Copy input directory under output directory deeply.
     *     // You can also specify <a href="#Patterns">include/exclude patterns</a>.
     *   }
     * }
     * </pre>
     * <p>
     * If the output file already exists, it will be replaced by input file unconditionaly. The
     * exact file attributes that are copied is platform and file system dependent and therefore
     * unspecified. Minimally, the last-modified-time is copied to the output file if supported by
     * both the input and output file store. Copying of file timestamps may result in precision
     * loss.
     * </p>
     * <p>
     * Copying a file is not an atomic operation. If an {@link IOException} is thrown then it
     * possible that the output file is incomplete or some of its file attributes have not been
     * copied from the input file.
     * </p>
     *
     * @param destination An output {@link Path} object which can be file or directory.
     * @param patterns <a href="#Patterns">include/exclude patterns</a> you want to sort out.
     * @throws IOException If an I/O error occurs.
     * @throws NullPointerException If the specified input or output file is <code>null</code>.
     * @throws NoSuchFileException If the input file is directory and the output file is
     *             <em>not</em> directory.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public void copyTo(Directory destination, String... patterns) {
        walk(Location.class, destination.path, 0, patterns, null, Integer.MAX_VALUE, false).to(I.NoOP);
    }

    /**
     * <p>
     * Copy a input {@link Path} to the output {@link Path} with its attributes. Simplified strategy
     * is the following:
     * </p>
     * <p>
     * <pre>
     * if (input.isFile) {
     *   if (output.isFile) {
     *     // Copy input file to output file.
     *   } else {
     *     // Copy input file to output directory.
     *   }
     * } else {
     *   if (output.isFile) {
     *     // NoSuchFileException will be thrown.
     *   } else {
     *     // Copy input directory under output directory deeply.
     *   }
     * }
     * </pre>
     * <p>
     * If the output file already exists, it will be replaced by input file unconditionaly. The
     * exact file attributes that are copied is platform and file system dependent and therefore
     * unspecified. Minimally, the last-modified-time is copied to the output file if supported by
     * both the input and output file store. Copying of file timestamps may result in precision
     * loss.
     * </p>
     * <p>
     * Copying a file is not an atomic operation. If an {@link IOException} is thrown then it
     * possible that the output file is incomplete or some of its file attributes have not been
     * copied from the input file.
     * </p>
     *
     * @param destination An output {@link Path} object which can be file or directory.
     * @param filter A file filter to copy.
     * @throws IOException If an I/O error occurs.
     * @throws NullPointerException If the specified input or output file is <code>null</code>.
     * @throws NoSuchFileException If the input file is directory and the output file is
     *             <em>not</em> directory.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public void copyTo(Directory destination, BiPredicate<Path, BasicFileAttributes> filter) {
        walk(Location.class, destination.path, 0, null, filter, Integer.MAX_VALUE, false).to(I.NoOP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete() {
        delete((BiPredicate) null);
    }

    /**
     * <p>
     * Delete a input {@link Path}. Simplified strategy is the following:
     * </p>
     * <p>
     * <pre>
     * if (input.isFile) {
     *   // Delete input file unconditionaly.
     * } else {
     *   // Delete input directory deeply.
     *   // You can also specify <a href="#Patterns">include/exclude patterns</a>.
     * }
     * </pre>
     * <p>
     * On some operating systems it may not be possible to remove a file when it is open and in use
     * by this Java virtual machine or other programs.
     * </p>
     *
     * @param input A input {@link Path} object which can be file or directory.
     * @param patterns <a href="#Patterns">include/exclude patterns</a> you want to sort out.
     * @throws IOException If an I/O error occurs.
     * @throws NullPointerException If the specified input file is <code>null</code>.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public void delete(String... patterns) {
        walk(Location.class, null, 2, patterns, null, Integer.MAX_VALUE, false).to(I.NoOP);
    }

    /**
     * <p>
     * Delete a input {@link Path}. Simplified strategy is the following:
     * </p>
     * <p>
     * <pre>
     * if (input.isFile) {
     *   // Delete input file unconditionaly.
     * } else {
     *   // Delete input directory deeply.
     *   // You can also specify <a href="#Patterns">include/exclude patterns</a>.
     * }
     * </pre>
     * <p>
     * On some operating systems it may not be possible to remove a file when it is open and in use
     * by this Java virtual machine or other programs.
     * </p>
     *
     * @param input A input {@link Path} object which can be file or directory.
     * @param filter A file filter.
     * @throws IOException If an I/O error occurs.
     * @throws NullPointerException If the specified input file is <code>null</code>.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public void delete(BiPredicate<Path, BasicFileAttributes> filter) {
        walk(Location.class, null, 2, null, filter, Integer.MAX_VALUE, false).to(I.NoOP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<WatchEvent<Location>> observe() {
        return observe(new String[0]);
    }

    /**
     * <p>
     * Observe the file system change and raises events when a file, directory, or file in a
     * directory, changes.
     * </p>
     * <p>
     * You can watch for changes in files and subdirectories of the specified directory.
     * </p>
     * <p>
     * The operating system interpret a cut-and-paste action or a move action as a rename action for
     * a directory and its contents. If you cut and paste a folder with files into a directory being
     * watched, the {@link Observer} object reports only the directory as new, but not its contents
     * because they are essentially only renamed.
     * </p>
     * <p>
     * Common file system operations might raise more than one event. For example, when a file is
     * moved from one directory to another, several Modify and some Create and Delete events might
     * be raised. Moving a file is a complex operation that consists of multiple simple operations,
     * therefore raising multiple events. Likewise, some applications might cause additional file
     * system events that are detected by the {@link Observer}.
     * </p>
     *
     * @param patterns <a href="#Patterns">include/exclude patterns</a> you want to sort out. Ignore
     *            patterns if you want to observe a file.
     * @return A observable event stream.
     * @throws NullPointerException If the specified path or listener is <code>null</code>.
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String)} method is invoked to
     *             check read access to the source file, the
     *             {@link SecurityManager#checkWrite(String)} is invoked to check write access to
     *             the target file. If a symbolic link is copied the security manager is invoked to
     *             check {@link LinkPermission}("symbolic").
     */
    public Signal<WatchEvent<Location>> observe(String... patterns) {
        return new Signal<>((observer, disposer) -> {
            // Create logical file system watch service.
            CymaticScan watcher = new CymaticScan(path, observer, disposer, patterns);

            // Run in anothor thread.
            I.schedule(watcher);

            // API definition
            return watcher;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileLock lock(WiseRunnable failed) {
        return file(".lock").lock(failed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Directory convert(Path path) {
        return Locator.directory(path);
    }
}

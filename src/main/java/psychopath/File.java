/*
 * Copyright (C) 2018 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package psychopath;

import static java.nio.file.StandardCopyOption.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;

import kiss.I;
import kiss.Signal;

/**
 * @version 2018/05/31 18:23:03
 */
public class File extends Location<File> {

    /**
     * @param path
     */
    File(Path path) {
        super(path);
    }

    /**
     * Retrieve the base name of this {@link File}.
     * 
     * @return A base name.
     */
    public final String base() {
        String name = name();
        int index = name.lastIndexOf(".");
        return index == -1 ? name : name.substring(0, index);
    }

    /**
     * Locate {@link File} with the specified new base name, but extension is same.
     * 
     * @param newBaseName A new base name.
     * @return New located {@link File}.
     */
    public final File base(String newBaseName) {
        String extension = extension();
        return Locator.file(path.resolveSibling(extension.isEmpty() ? newBaseName : newBaseName + "." + extension));
    }

    /**
     * Retrieve the extension of this {@link File}.
     * 
     * @return An extension or empty if it has no extension.
     */
    public final String extension() {
        String name = name();
        int index = name.lastIndexOf(".");
        return index == -1 ? "" : name.substring(index + 1);
    }

    /**
     * Locate {@link File} with the specified new extension, but base name is same.
     * 
     * @param newExtension A new extension.
     * @return New located {@link File}.
     */
    public final File extension(String newExtension) {
        return Locator.file(path.resolveSibling(base() + "." + newExtension));
    }

    /**
     * Cast to archive.
     * 
     * @return
     */
    public Directory asArchive() {
        return new Archive(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Location<? extends Location>> children() {
        return Signal.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isContainer() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<Directory> asDirectory() {
        return Signal.EMPTY;
    }

    public long lastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveTo(Directory destination) {
        try {
            destination.create();
            Files.move(path, destination.file(name()).path, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void moveTo(File destination) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyTo(Directory destination) {
        try {
            destination.create();
            Files.copy(path, destination.file(name()).path, REPLACE_EXISTING, COPY_ATTRIBUTES);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    public Directory unpackTo(Directory destination) {
        try (ArchiveInputStream in = detect()) {
            destination.create();

            ArchiveEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                if (in.canReadEntryData(entry)) {
                    if (entry.isDirectory()) {
                        destination.directory(entry.getName()).create();
                    } else {
                        try (OutputStream out = destination.file(entry.getName()).newOutputStream()) {
                            I.copy(in, out, false);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw I.quiet(e);
        }
        return destination;
    }

    /**
     * {@inheritDoc}
     */
    public void copyTo(File destination) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create() {
        try {
            Files.createFile(path);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<WatchEvent<Path>> observe() {
        return null;
    }

    /**
     * Opens a file, returning an input stream to read from the file. The stream will not be buffered,
     * and is not required to support the {@link InputStream#mark mark} or {@link InputStream#reset
     * reset} methods. The stream will be safe for access by multiple concurrent threads. Reading
     * commences at the beginning of the file. Whether the returned stream is <i>asynchronously
     * closeable</i> and/or <i>interruptible</i> is highly file system provider specific and therefore
     * not specified.
     * <p>
     * The {@code options} parameter determines how the file is opened. If no options are present then
     * it is equivalent to opening the file with the {@link StandardOpenOption#READ READ} option. In
     * addition to the {@code
     * READ} option, an implementation may also support additional implementation specific options.
     *
     * @param options options specifying how the file is opened
     * @return a new input stream
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked
     *             to check read access to the file.
     */
    public InputStream newInputStream(OpenOption... options) {
        try {
            return Files.newInputStream(path, options);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Opens or creates a file, returning an output stream that may be used to write bytes to the file.
     * The resulting stream will not be buffered. The stream will be safe for access by multiple
     * concurrent threads. Whether the returned stream is <i>asynchronously closeable</i> and/or
     * <i>interruptible</i> is highly file system provider specific and therefore not specified.
     * <p>
     * This method opens or creates a file in exactly the manner specified by the
     * {@link #newByteChannel(Path,Set,FileAttribute[]) newByteChannel} method with the exception that
     * the {@link StandardOpenOption#READ READ} option may not be present in the array of options. If no
     * options are present then this method works as if the {@link StandardOpenOption#CREATE CREATE},
     * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and
     * {@link StandardOpenOption#WRITE WRITE} options are present. In other words, it opens the file for
     * writing, creating the file if it doesn't exist, or initially truncating an existing
     * {@link #isRegularFile regular-file} to a size of {@code 0} if it exists.
     * <p>
     *
     * @param options options specifying how the file is opened
     * @return a new output stream
     * @throws IllegalArgumentException if {@code options} contains an invalid combination of options
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkWrite(String) checkWrite} method is
     *             invoked to check write access to the file. The
     *             {@link SecurityManager#checkDelete(String) checkDelete} method is invoked to check
     *             delete access if the file is opened with the {@code DELETE_ON_CLOSE} option.
     */
    public OutputStream newOutputStream(OpenOption... options) {
        try {
            parent().create();
            return Files.newOutputStream(path, options);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the file.
     * <p>
     * This method opens or creates a file in exactly the manner specified by the
     * {@link #newByteChannel(Path,Set,FileAttribute[]) newByteChannel} method.
     *
     * @param options options specifying how the file is opened
     * @return a new seekable byte channel
     * @throws IllegalArgumentException if the set contains an invalid combination of options
     * @throws UnsupportedOperationException if an unsupported open option is specified
     * @throws FileAlreadyExistsException if a file of that name already exists and the
     *             {@link StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified <i>(optional
     *             specific exception)</i>
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *             installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked
     *             to check read access to the path if the file is opened for reading. The
     *             {@link SecurityManager#checkWrite(String) checkWrite} method is invoked to check
     *             write access to the path if the file is opened for writing. The
     *             {@link SecurityManager#checkDelete(String) checkDelete} method is invoked to check
     *             delete access if the file is opened with the {@code DELETE_ON_CLOSE} option.
     * @see java.nio.channels.FileChannel#open(Path,OpenOption[])
     */
    public SeekableByteChannel newByteChannel(OpenOption... options) {
        try {
            return Files.newByteChannel(path, options);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected File convert(Path path) {
        return Locator.file(path);
    }

    /**
     * Detect archive file system.
     * 
     * @return An archive.
     */
    private ArchiveInputStream detect() {
        ArchiveStreamFactory factory = new ArchiveStreamFactory(System.getProperty("sun.jnu.encoding"));

        try {
            try {
                return factory.createArchiveInputStream(extension(), new BufferedInputStream(newInputStream()));
            } catch (StreamingNotSupportedException e) {
                return factory.createArchiveInputStream(new BufferedInputStream(newInputStream()));
            }
        } catch (ArchiveException e) {
            throw I.quiet(e);
        }
    }
}

package com.github.onsharp.installer;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.Files.*;

public final class Unzipper {

    public static void unzip(final Path zipFile, final Path destDir) throws IOException {
        if (notExists(destDir)) {
            createDirectories(destDir);
        }

        try (FileSystem zipFileSystem = FileSystems.newFileSystem(zipFile, null)) {
            final Path root = zipFileSystem.getRootDirectories().iterator().next();

            walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destDir.toString(), file.toString());
                    try {
                        copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (DirectoryNotEmptyException ignore) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());
                    if (notExists(dirToCreate)) {
                        createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}

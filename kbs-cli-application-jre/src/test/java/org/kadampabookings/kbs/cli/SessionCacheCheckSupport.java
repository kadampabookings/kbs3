package org.kadampabookings.kbs.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

/** Changes a file's mode, so a check can present a cache the tool must refuse. */
final class SessionCacheCheckSupport {

    private SessionCacheCheckSupport() {}

    static void setMode(Path file, String mode) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (view != null)
            view.setPermissions(PosixFilePermissions.fromString(mode));
    }
}

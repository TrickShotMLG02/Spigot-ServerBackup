package net.server_backup.core;

import java.nio.file.Files;
import java.nio.file.Path;

/** Validates and resolves the existing zip and unzip command paths. */
public final class ArchiveCommandPlanner {
    public enum Error { ALREADY_ZIP, NOT_ZIP, NOT_FOUND, TARGET_EXISTS, INVALID_PATH }
    public static final class Plan {
        public final Path source, target;
        public final Error error;
        private Plan(Path source, Path target, Error error) { this.source = source; this.target = target; this.error = error; }
    }
    public Plan plan(Path directory, String name, boolean unzip) {
        if (unzip && !name.endsWith(".zip")) return new Plan(null, null, Error.NOT_ZIP);
        if (!unzip && name.endsWith(".zip")) return new Plan(null, null, Error.ALREADY_ZIP);
        Path root = directory.toAbsolutePath().normalize();
        Path source = root.resolve(name).normalize();
        Path target = root.resolve(unzip ? name.substring(0, name.length() - 4) : name + ".zip").normalize();
        if (!source.startsWith(root) || source.equals(root) || !target.startsWith(root) || target.equals(root))
            return new Plan(source, target, Error.INVALID_PATH);
        if (Files.exists(target)) return new Plan(source, target, Error.TARGET_EXISTS);
        if (!Files.exists(source)) return new Plan(source, target, Error.NOT_FOUND);
        try {
            if (!source.toRealPath().startsWith(root.toRealPath())) return new Plan(source, target, Error.INVALID_PATH);
        } catch (java.io.IOException e) { return new Plan(source, target, Error.INVALID_PATH); }
        return new Plan(source, target, null);
    }
}

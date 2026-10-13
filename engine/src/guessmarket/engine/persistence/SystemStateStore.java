package guessmarket.engine.persistence;

import guessmarket.engine.exception.SystemStateFileException;
import guessmarket.engine.market.GuessMarketSystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the whole running system to a file and reads it back again.
 *
 * <p>Everything the system is made of is serializable, so the entire object graph, down to the
 * last trade in the last event, travels in a single write and a single read. The caller gives a
 * path without an extension and this class adds its own, which keeps saved states easy to spot
 * and keeps them apart from the XML files the system is described by.
 */
public final class SystemStateStore {

    /** The extension every saved state carries. */
    public static final String STATE_FILE_EXTENSION = ".gmstate";

    private SystemStateStore() {
    }

    /**
     * @param system              the system to write
     * @param pathWithoutExtension the full path and file name to write it to
     * @return the file that was actually written
     * @throws SystemStateFileException if the file cannot be written
     */
    public static String save(GuessMarketSystem system, String pathWithoutExtension) {
        Path file = resolve(pathWithoutExtension);
        Path folder = file.getParent();
        if (folder != null && !Files.isDirectory(folder)) {
            throw new SystemStateFileException("There is no folder called \"" + folder
                    + "\", so the system could not be saved there. "
                    + "Create the folder first, or choose a different path.");
        }
        try (OutputStream output = Files.newOutputStream(file)) {
            save(system, output);
        } catch (IOException e) {
            throw new SystemStateFileException("The system could not be saved to \"" + file
                    + "\": " + e.getMessage()
                    + ". Check that the path is correct and that you may write to it.", e);
        }
        return file.toString();
    }

    /**
     * @param pathWithoutExtension the full path and file name a system was saved to
     * @return the system that was read back
     * @throws SystemStateFileException if the file is missing or is not a saved Guess Market
     */
    public static GuessMarketSystem load(String pathWithoutExtension) {
        Path file = resolve(pathWithoutExtension);
        if (!Files.isRegularFile(file)) {
            throw new SystemStateFileException("There is no saved system at \"" + file
                    + "\". Give the same path and file name you saved with, "
                    + "without the " + STATE_FILE_EXTENSION + " extension.");
        }
        try (InputStream input = Files.newInputStream(file)) {
            return load(input, file.toString());
        } catch (IOException e) {
            throw new SystemStateFileException("The file \"" + file
                    + "\" could not be read as a saved Guess Market system. "
                    + "It may belong to a different program, or it may have been damaged.", e);
        }
    }

    /** Writes a complete state to a caller-owned stream. */
    public static void save(GuessMarketSystem system, OutputStream output) {
        if (system == null || output == null) {
            throw new SystemStateFileException("A system and an output stream are required to save state.");
        }
        try {
            ObjectOutputStream objects = new ObjectOutputStream(output);
            objects.writeObject(system);
            objects.flush();
        } catch (IOException e) {
            throw new SystemStateFileException("The system state could not be written: "
                    + safeMessage(e) + ".", e);
        }
    }

    /**
     * Reads a complete state from a caller-owned stream. The filter admits only the market model,
     * its DTO enums and the small set of JDK value/collection classes used by saved market states.
     * This keeps a browser upload from turning Java deserialization into arbitrary code.
     */
    public static GuessMarketSystem load(InputStream input, String sourceName) {
        if (input == null) {
            throw new SystemStateFileException("No saved system data was supplied.");
        }
        String displayName = sourceName == null || sourceName.isBlank()
                ? "the uploaded file" : "\"" + sourceName + "\"";
        try {
            ObjectInputStream objects = new ObjectInputStream(input);
            objects.setObjectInputFilter(SystemStateStore::filterStateClass);
            Object restored = objects.readObject();
            if (!(restored instanceof GuessMarketSystem system)) {
                throw new ClassCastException("The root object is not a Guess Market system.");
            }
            return system;
        } catch (ClassNotFoundException | ClassCastException | IOException e) {
            throw new SystemStateFileException("The file " + displayName
                    + " could not be read as a saved Guess Market system. "
                    + "It may belong to a different program, a different format, or it may have been damaged.", e);
        }
    }

    private static ObjectInputFilter.Status filterStateClass(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 100 || info.references() > 1_000_000
                || info.streamBytes() > 16L * 1024 * 1024 || info.arrayLength() > 1_000_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String name = type.getName();
        if (name.startsWith("guessmarket.engine.market.")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        if (name.equals("guessmarket.dto.CommissionType")
                || name.equals("guessmarket.dto.EventStatus")
                || name.equals("guessmarket.dto.OrderSide")
                || name.equals("guessmarket.dto.TradeKind")
                || name.equals("java.lang.String")
                || name.equals("java.lang.Integer")
                || name.equals("java.lang.Long")
                || name.equals("java.lang.Double")
                || name.equals("java.lang.Number")
                || name.equals("java.lang.Object")
                || name.equals("java.lang.Enum")
                || name.equals("java.util.ArrayList")
                || name.equals("java.util.HashMap")
                || name.equals("java.util.LinkedHashMap")
                || name.equals("java.util.Map$Entry")
                || name.equals("java.util.HashSet")
                || name.equals("java.util.LinkedHashSet")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }

    private static String safeMessage(IOException error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getSimpleName() : error.getMessage();
    }

    /** Adds the state extension to the given path, accepting a path that already carries it. */
    private static Path resolve(String pathWithoutExtension) {
        if (pathWithoutExtension == null || pathWithoutExtension.trim().isEmpty()) {
            throw new SystemStateFileException("No file path was given. "
                    + "Type the full path and file name to use, without an extension.");
        }
        String path = pathWithoutExtension.trim();
        if (path.toLowerCase().endsWith(STATE_FILE_EXTENSION)) {
            path = path.substring(0, path.length() - STATE_FILE_EXTENSION.length());
        }
        return new File(path + STATE_FILE_EXTENSION).toPath();
    }
}

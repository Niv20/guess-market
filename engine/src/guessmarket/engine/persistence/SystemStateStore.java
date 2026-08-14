package guessmarket.engine.persistence;

import guessmarket.engine.exception.SystemStateFileException;
import guessmarket.engine.market.GuessMarketSystem;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(system);
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
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (GuessMarketSystem) in.readObject();
        } catch (ClassNotFoundException | ClassCastException | IOException e) {
            throw new SystemStateFileException("The file \"" + file
                    + "\" could not be read as a saved Guess Market system. "
                    + "It may belong to a different program, or it may have been damaged.", e);
        }
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

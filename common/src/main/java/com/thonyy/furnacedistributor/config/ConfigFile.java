package com.thonyy.furnacedistributor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Missing fields use defaults; unknown fields survive saves. Invalid files are backed up before replacing. */
public final class ConfigFile<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("Furnace Distributor");
    private final Path path;
    private final Class<T> type;
    private final Consumer<T> validator;
    private JsonObject original = new JsonObject();
    private boolean invalid;

    public ConfigFile(Path path, Class<T> type, Consumer<T> validator) {
        this.path = path;
        this.type = type;
        this.validator = validator;
    }

    public T load(Supplier<T> defaults) {
        T value = defaults.get();
        if (Files.exists(path)) {
            try {
                original = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                value = GSON.fromJson(original, type);
                validator.accept(value);
                invalid = false;
                return value;
            } catch (IOException | RuntimeException exception) {
                invalid = true;
                LOGGER.warn("Cannot read {} ({}). Using defaults; the original file is preserved.", path, exception.getMessage());
                value = defaults.get();
            }
        } else {
            save(value);
        }
        validator.accept(value);
        return value;
    }

    public boolean save(T value) {
        validator.accept(value);
        try {
            Files.createDirectories(path.getParent());
            if (invalid && Files.exists(path)) {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".invalid-" + System.currentTimeMillis()));
                invalid = false;
                original = new JsonObject();
            }
            JsonObject updated = original.deepCopy();
            GSON.toJsonTree(value).getAsJsonObject().entrySet().forEach(entry -> updated.add(entry.getKey(), entry.getValue()));
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(updated) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            original = updated;
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Cannot save {}", path, exception);
            return false;
        }
    }
}

package kr.kwon.capitalcraft.client.hud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HudSettings {
    public static final int BANK_WIDTH = 180;
    public static final int BANK_HEIGHT = 60;
    public boolean bankVisible = true;
    public double bankX = 1;
    public double bankY = 0.45;

    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    public Bounds bounds(int width, int height) {
        return new Bounds(
                (int) Math.round(unit(bankX, 1) * Math.max(0, width - BANK_WIDTH)),
                (int) Math.round(unit(bankY, 0.45) * Math.max(0, height - BANK_HEIGHT)),
                BANK_WIDTH,
                BANK_HEIGHT);
    }

    public void move(double x, double y, int width, int height) {
        bankX = unit(x / Math.max(1, width - BANK_WIDTH), 1);
        bankY = unit(y / Math.max(1, height - BANK_HEIGHT), 0.45);
    }

    public HudSettings copy() {
        HudSettings copy = new HudSettings();
        copy.bankVisible = bankVisible;
        copy.bankX = bankX;
        copy.bankY = bankY;
        return copy;
    }

    public static HudSettings load(Path path) throws IOException {
        HudSettings settings = new HudSettings();
        if (!Files.exists(path)) return settings;
        if (Files.size(path) > 4096) throw new IOException("HUD settings file is too large");
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (json.has("bankVisible"))
                settings.bankVisible = json.get("bankVisible").getAsBoolean();
            if (json.has("bankX")) settings.bankX = unit(json.get("bankX").getAsDouble(), 1);
            if (json.has("bankY")) settings.bankY = unit(json.get("bankY").getAsDouble(), 0.45);
            return settings;
        } catch (RuntimeException error) {
            throw new IOException("Invalid HUD settings", error);
        }
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, new Gson().toJson(this));
        try {
            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static double unit(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : fallback;
    }
}

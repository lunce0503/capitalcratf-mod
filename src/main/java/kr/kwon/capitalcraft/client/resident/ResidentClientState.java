package kr.kwon.capitalcraft.client.resident;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ResidentClientState {
    private static final Map<UUID, String> APPEARANCES = new HashMap<>();
    private static final Map<UUID, Speech> SPEECHES = new HashMap<>();
    private static final Map<UUID, String> NAMES = new HashMap<>();
    public record Speech(String text, long started, long expires) {}

    private ResidentClientState() {
    }

    public static void sync(JsonObject payload) {
        Map<UUID, String> next = new HashMap<>();
        Map<UUID, Speech> speech = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();
        if (payload.has("residents") && payload.get("residents").isJsonArray()) {
            for (JsonElement element : payload.getAsJsonArray("residents")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject resident = element.getAsJsonObject();
                try {
                    UUID entityUuid = UUID.fromString(resident.get("entityUuid").getAsString());
                    String appearance = resident.get("appearance").getAsString();
                    if (resident.has("name") && resident.get("name").isJsonPrimitive()) {
                        String name = resident.get("name").getAsString();
                        if (!name.isBlank() && name.codePointCount(0, name.length()) <= 64) names.put(entityUuid, name);
                    }
                    if (appearance.equals("mari") || appearance.equals("seia")) {
                        next.put(entityUuid, appearance);
                    }
                    if (resident.has("speech") && resident.has("speechRemainingMs")) {
                        String text = resident.get("speech").getAsString();
                        long remaining = Math.max(0, Math.min(25000, resident.get("speechRemainingMs").getAsLong()));
                        if (!text.isBlank() && text.codePointCount(0, text.length()) <= 240 && remaining > 0) {
                            long now = System.currentTimeMillis();
                            long duration = Math.max(10000, Math.min(25000, text.codePointCount(0, text.length()) * 100L));
                            Speech previous = SPEECHES.get(entityUuid);
                            long started = now - Math.max(0, duration - remaining);
                            if (previous != null && previous.text().equals(text) && Math.abs(previous.expires() - now - remaining) < 1000) started = previous.started();
                            speech.put(entityUuid, new Speech(text, started, now + remaining));
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Ignore one malformed entry without discarding the rest of the snapshot.
                }
            }
        }
        APPEARANCES.clear();
        APPEARANCES.putAll(next);
        SPEECHES.clear();
        SPEECHES.putAll(speech);
        NAMES.clear();
        NAMES.putAll(names);
    }

    public static String appearance(UUID entityUuid) {
        return APPEARANCES.getOrDefault(entityUuid, "vanilla");
    }

    public static void reset() {
        APPEARANCES.clear();
        SPEECHES.clear();
        NAMES.clear();
    }

    public static String name(UUID entityUuid) {
        return NAMES.get(entityUuid);
    }

    public static String speech(UUID entityUuid) {
        Speech value = SPEECHES.get(entityUuid);
        return value == null || value.expires() <= System.currentTimeMillis() ? "" : value.text();
    }

    public static Map<UUID, Speech> speeches() {
        long now = System.currentTimeMillis();
        Map<UUID, Speech> active = new HashMap<>();
        SPEECHES.forEach((id, value) -> {
            if (value.expires() > now) active.put(id, value);
        });
        return Map.copyOf(active);
    }
}

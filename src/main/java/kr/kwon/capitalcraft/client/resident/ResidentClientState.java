package kr.kwon.capitalcraft.client.resident;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ResidentClientState {
    private static final Map<UUID, String> APPEARANCES = new HashMap<>();

    private ResidentClientState() {
    }

    public static void sync(JsonObject payload) {
        Map<UUID, String> next = new HashMap<>();
        if (payload.has("residents") && payload.get("residents").isJsonArray()) {
            for (JsonElement element : payload.getAsJsonArray("residents")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject resident = element.getAsJsonObject();
                try {
                    UUID entityUuid = UUID.fromString(resident.get("entityUuid").getAsString());
                    String appearance = resident.get("appearance").getAsString();
                    if (appearance.equals("mari")) {
                        next.put(entityUuid, appearance);
                    }
                } catch (RuntimeException ignored) {
                    // Ignore one malformed entry without discarding the rest of the snapshot.
                }
            }
        }
        APPEARANCES.clear();
        APPEARANCES.putAll(next);
    }

    public static String appearance(UUID entityUuid) {
        return APPEARANCES.getOrDefault(entityUuid, "vanilla");
    }

    public static void reset() {
        APPEARANCES.clear();
    }
}


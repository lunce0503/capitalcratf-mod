package kr.kwon.capitalcraft.client.butchery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ButcheryClientState {
    private static final Map<String, ButcheryCarcassState> CARCASSES = new LinkedHashMap<>();

    private ButcheryClientState() {
    }

    public static void reset() {
        CARCASSES.clear();
    }

    public static void sync(JsonObject payload) {
        CARCASSES.clear();
        if (!payload.has("carcasses") || !payload.get("carcasses").isJsonArray()) {
            return;
        }
        for (JsonElement element : payload.getAsJsonArray("carcasses")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            Set<String> removedParts = new LinkedHashSet<>();
            JsonArray removed = object.has("removedParts") && object.get("removedParts").isJsonArray()
                ? object.getAsJsonArray("removedParts")
                : new JsonArray();
            for (JsonElement part : removed) {
                if (part.isJsonPrimitive()) {
                    removedParts.add(part.getAsString());
                }
            }
            ButcheryCarcassState state = new ButcheryCarcassState(
                string(object, "world", ""),
                integer(object, "x", 0),
                integer(object, "y", 0),
                integer(object, "z", 0),
                string(object, "materialType", "capitalcraft:animal/pig"),
                decimal(object, "weight", 80.0D),
                longValue(object, "createdAt", 0L),
                removedParts
            );
            CARCASSES.put(state.key(), state);
        }
    }

    public static Collection<ButcheryCarcassState> carcasses() {
        return CARCASSES.values();
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double decimal(JsonObject object, String key, double fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}

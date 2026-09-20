package kr.kwon.capitalcraft.client.automotive;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VehicleClientState {
    private static final Map<UUID, VehicleSnapshot> VEHICLES = new LinkedHashMap<>();
    private static long serverTick;

    private VehicleClientState() {
    }

    public static void reset() {
        VEHICLES.clear();
        serverTick = 0L;
    }

    public static void sync(JsonObject payload) {
        Map<UUID, VehicleSnapshot> updated = new LinkedHashMap<>();
        serverTick = longValue(payload, "serverTick", serverTick);
        if (payload.has("vehicles") && payload.get("vehicles").isJsonArray()) {
            for (JsonElement element : payload.getAsJsonArray("vehicles")) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                UUID vehicleId = uuid(object, "vehicleId");
                if (vehicleId == null) continue;
                VehicleSnapshot snapshot = new VehicleSnapshot(
                    vehicleId,
                    string(object, "vehicleType", "capitalcraft:automotive/compact_sedan"),
                    string(object, "renderProfile", "compact_sedan_v1"),
                    string(object, "world", ""),
                    decimal(object, "x", 0.0D),
                    decimal(object, "y", 0.0D),
                    decimal(object, "z", 0.0D),
                    (float) decimal(object, "yaw", 0.0D),
                    decimal(object, "speed", 0.0D),
                    uuid(object, "driverUuid")
                );
                updated.put(vehicleId, snapshot);
            }
        }
        VEHICLES.clear();
        VEHICLES.putAll(updated);
    }

    public static VehicleSnapshot drivenBy(UUID playerUuid) {
        if (playerUuid == null) return null;
        for (VehicleSnapshot snapshot : VEHICLES.values()) {
            if (playerUuid.equals(snapshot.driverUuid())) return snapshot;
        }
        return null;
    }

    public static Collection<VehicleSnapshot> vehicles() {
        return VEHICLES.values();
    }

    public static long serverTick() {
        return serverTick;
    }

    private static UUID uuid(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return null;
        try {
            return UUID.fromString(object.get(key).getAsString());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static double decimal(JsonObject object, String key, double fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}

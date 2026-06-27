package kr.kwon.capitalcraft.client.foodindustry.butchery;

import java.util.LinkedHashSet;
import java.util.Set;

public record ButcheryCarcassState(
    String world,
    int x,
    int y,
    int z,
    String materialType,
    double weight,
    long createdAt,
    Set<String> removedParts
) {
    public ButcheryCarcassState {
        removedParts = new LinkedHashSet<>(removedParts);
    }

    public String key() {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public int remainingPartCount() {
        return Math.max(0, 6 - removedParts.size());
    }
}

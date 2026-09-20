package kr.kwon.capitalcraft.client.automotive;

import java.util.UUID;

public record VehicleSnapshot(
    UUID vehicleId,
    String vehicleType,
    String renderProfile,
    String world,
    double x,
    double y,
    double z,
    float yaw,
    double speed,
    UUID driverUuid
) {
}

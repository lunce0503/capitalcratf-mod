package kr.kwon.capitalcraft.client.automotive;

import java.util.UUID;
import kr.kwon.capitalcraft.client.network.CapitalCraftNetwork;
import net.minecraft.client.Minecraft;

public final class VehicleInputController {
    private static UUID activeVehicleId;
    private static long sequence;
    private static boolean exitSent;

    private VehicleInputController() {
    }

    public static void reset() {
        activeVehicleId = null;
        sequence = 0L;
        exitSent = false;
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            reset();
            return;
        }
        VehicleSnapshot driven = VehicleClientState.drivenBy(client.player.getUUID());
        if (driven == null) {
            activeVehicleId = null;
            exitSent = false;
            return;
        }
        if (!driven.vehicleId().equals(activeVehicleId)) {
            activeVehicleId = driven.vehicleId();
            sequence = 0L;
            exitSent = false;
        }

        boolean wantsExit = client.options.keyShift.isDown();
        if (wantsExit) {
            if (!exitSent) {
                CapitalCraftNetwork.sendVehicleExit(activeVehicleId);
                exitSent = true;
            }
            return;
        }
        exitSent = false;

        double throttle = 0.0D;
        if (client.options.keyUp.isDown()) throttle += 1.0D;
        if (client.options.keyDown.isDown()) throttle -= 1.0D;
        double steering = 0.0D;
        if (client.options.keyLeft.isDown()) steering -= 1.0D;
        if (client.options.keyRight.isDown()) steering += 1.0D;
        boolean brake = client.options.keyJump.isDown();
        CapitalCraftNetwork.sendVehicleInput(activeVehicleId, ++sequence, throttle, steering, brake);
    }
}

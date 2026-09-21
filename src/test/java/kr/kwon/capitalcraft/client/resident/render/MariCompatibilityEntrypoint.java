package kr.kwon.capitalcraft.client.resident.render;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Runs with Fabric's real Mixin transformer, then exits before window creation. */
public final class MariCompatibilityEntrypoint implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        if (!Boolean.getBoolean("capitalcraft.verifyMariCompatibility")) return;
        try {
            if (!FabricLoader.getInstance().isModLoaded("sodium")) throw new AssertionError("Sodium missing");
            if (Boolean.getBoolean("capitalcraft.verifyIris") && !FabricLoader.getInstance().isModLoaded("iris")) {
                throw new AssertionError("Iris missing");
            }
            MariMeshVerification.main(new String[0]);
            System.out.println("PASS: real Fabric/Sodium mixins; Iris=" + FabricLoader.getInstance().isModLoaded("iris"));
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }
}

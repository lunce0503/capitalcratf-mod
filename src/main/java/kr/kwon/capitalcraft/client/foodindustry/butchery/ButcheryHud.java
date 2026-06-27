package kr.kwon.capitalcraft.client.foodindustry.butchery;

import java.util.Comparator;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public final class ButcheryHud {
    private ButcheryHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) {
            return;
        }
        ButcheryCarcassState target = nearestCarcass(client);
        if (target == null) {
            return;
        }
        int x = 12;
        int y = 12;
        graphics.drawString(client.font, "CapitalCraft 도축", x, y, 0xFFE6D2, true);
        graphics.drawString(
            client.font,
            String.format(Locale.US, "돼지 사체 %.1fkg", target.weight()),
            x,
            y + 12,
            0xFFFFFF,
            true
        );
        graphics.drawString(
            client.font,
            "남은 부위 " + target.remainingPartCount() + " / 6",
            x,
            y + 24,
            0xD8D8D8,
            true
        );
    }

    private static ButcheryCarcassState nearestCarcass(Minecraft client) {
        Vec3 playerPosition = client.player.position();
        return ButcheryClientState.carcasses().stream()
            .filter(state -> distanceSquared(playerPosition, state) <= 25.0D)
            .min(Comparator.comparingDouble(state -> distanceSquared(playerPosition, state)))
            .orElse(null);
    }

    private static double distanceSquared(Vec3 position, ButcheryCarcassState state) {
        double dx = position.x - (state.x() + 0.5D);
        double dy = position.y - (state.y() - 0.8D);
        double dz = position.z - (state.z() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }
}

package kr.kwon.capitalcraft.client.automotive;

import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class VehicleHud {
    private VehicleHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) return;
        VehicleSnapshot vehicle = VehicleClientState.drivenBy(client.player.getUUID());
        if (vehicle == null) return;

        int x = 12;
        int y = graphics.guiHeight() - 48;
        double blocksPerSecond = Math.abs(vehicle.speed()) * 20.0D;
        graphics.drawString(client.font, "CapitalCraft 자동차", x, y, 0xFFF0D060, true);
        graphics.drawString(
            client.font,
            String.format(Locale.ROOT, "속도 %.1f block/s", blocksPerSecond),
            x,
            y + 12,
            0xFFFFFFFF,
            true
        );
        graphics.drawString(client.font, "WASD 주행 · Space 제동 · Shift 하차", x, y + 24, 0xFFD8D8D8, true);
    }
}

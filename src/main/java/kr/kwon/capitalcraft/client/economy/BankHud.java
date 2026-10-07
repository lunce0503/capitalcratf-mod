package kr.kwon.capitalcraft.client.economy;

import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import kr.kwon.capitalcraft.client.hud.HudSettings;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Path;

public final class BankHud {
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("capitalcraft-hud.json");
    private static HudSettings settings = new HudSettings();

    private BankHud() {}

    public static void register() {
        try {
            settings = HudSettings.load(FILE);
        } catch (IOException error) {
            CapitalCraftClientMod.LOGGER.warn("Could not load HUD settings", error);
        }
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("capitalcraft", "bank"),
                (graphics, ticks) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player == null
                            || client.level == null
                            || client.gui.screen() != null
                            || !settings.bankVisible
                            || !BankClientState.ready()) return;
                    draw(
                            graphics,
                            settings.bounds(graphics.guiWidth(), graphics.guiHeight()),
                            false);
                });
    }

    public static HudSettings settings() {
        return settings.copy();
    }

    public static void save(HudSettings next) throws IOException {
        next.save(FILE);
        settings = next.copy();
    }

    public static void toggle() {
        HudSettings next = settings();
        next.bankVisible = !next.bankVisible;
        try {
            save(next);
        } catch (IOException error) {
            CapitalCraftClientMod.LOGGER.warn("Could not save HUD settings", error);
        }
    }

    public static void draw(
            GuiGraphicsExtractor graphics, HudSettings.Bounds bounds, boolean preview) {
        Minecraft client = Minecraft.getInstance();
        int x = bounds.x(), y = bounds.y(), right = x + bounds.width();
        graphics.fill(x, y, right, y + bounds.height(), 0xB0181818);
        graphics.fill(x, y, right, y + 1, 0xFFE0BD54);
        graphics.text(client.font, "중앙 은행", x + 8, y + 8, 0xFFE0BD54, false);
        String name = BankClientState.playerName();
        if (name.isEmpty())
            name = client.player == null ? "Player" : client.player.getName().getString();
        graphics.text(
                client.font,
                client.font.plainSubstrByWidth(name, bounds.width() - 16),
                x + 8,
                y + 24,
                0xFFFFFFFF,
                false);
        String amount = preview && !BankClientState.ready() ? "0 VIL" : BankClientState.amount();
        graphics.text(
                client.font,
                client.font.plainSubstrByWidth(amount, bounds.width() - 16),
                x + 8,
                y + 42,
                0xFF7DE1B0,
                false);
    }
}

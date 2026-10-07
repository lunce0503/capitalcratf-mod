package kr.kwon.capitalcraft.client.resident;

import kr.kwon.capitalcraft.client.economy.BankClientState;
import kr.kwon.capitalcraft.client.economy.BankHud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResidentSpeechHud {
    private record Speaker(
            ResidentClientState.Speech speech,
            double x,
            double y,
            double priority,
            ResidentSpeechLayout.Bounds nameBounds) {}

    private ResidentSpeechHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("capitalcraft", "resident_speech"),
                (graphics, ticks) -> render(graphics, ticks.getGameTimeDeltaPartialTick(false)));
    }

    private static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.gui.screen() != null) return;
        var active = ResidentClientState.speeches();
        if (active.isEmpty()) return;
        var camera = client.gameRenderer.mainCamera();
        if (!camera.isInitialized()) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        List<Speaker> speakers = new ArrayList<>();
        for (var entity : client.level.entitiesForRendering()) {
            var speech = active.get(entity.getUUID());
            if (speech == null
                    || !(entity instanceof Villager villager)
                    || !villager.isAlive()
                    || villager.isInvisibleTo(client.player)
                    || villager.distanceToSqr(client.player) > 256) continue;
            Vec3 position = villager.getPosition(partialTick);
            Vec3 relative = position.subtract(camera.position());
            if (relative.dot(new Vec3(camera.forwardVector())) <= 0) continue;
            Vec3 center =
                    client.gameRenderer.projectPointToScreen(
                            position.add(0, villager.getBbHeight() / 2.0, 0));
            if (!Double.isFinite(center.x)
                    || !Double.isFinite(center.y)
                    || Math.abs(center.x) > 1.1
                    || Math.abs(center.y) > 1.1) continue;
            if (!client.player.hasLineOfSight(villager)) continue;
            Vec3 attachment =
                    villager.getAttachments().get(EntityAttachment.NAME_TAG, 0, villager.getYRot());
            Vec3 namePosition = position.add(attachment).add(0, 0.5, 0);
            Vec3 anchor = client.gameRenderer.projectPointToScreen(namePosition);
            if (!Double.isFinite(anchor.x) || !Double.isFinite(anchor.y)) continue;
            double x = (anchor.x + 1) * width / 2, y = (1 - anchor.y) * height / 2;
            String name = ResidentClientState.name(villager.getUUID());
            if (name == null) name = villager.getName().getString();
            Vec3 left =
                    client.gameRenderer.projectPointToScreen(
                            namePosition.add(
                                    new Vec3(camera.leftVector())
                                            .scale(client.font.width(name) * 0.025 / 2)));
            Vec3 top =
                    client.gameRenderer.projectPointToScreen(
                            namePosition.add(new Vec3(camera.upVector()).scale(0.25)));
            int nameWidth = Math.max(8, (int) Math.ceil(Math.abs(left.x - anchor.x) * width));
            int nameHeight = Math.max(8, (int) Math.ceil(Math.abs(top.y - anchor.y) * height / 2));
            var nameBounds =
                    new ResidentSpeechLayout.Bounds(
                            (int) x - nameWidth / 2, (int) y - nameHeight, nameWidth, nameHeight);
            speakers.add(
                    new Speaker(
                            speech, x, y, center.x * center.x + center.y * center.y, nameBounds));
        }
        speakers.sort(Comparator.comparingDouble(Speaker::priority));
        var metrics = ResidentSpeechLayout.metrics(width, height, client.font.lineHeight);
        List<ResidentSpeechLayout.Bounds> occupied = new ArrayList<>();
        speakers.forEach(speaker -> occupied.add(speaker.nameBounds()));
        var bankSettings = BankHud.settings();
        if (bankSettings.bankVisible && BankClientState.ready()) {
            var bank = bankSettings.bounds(width, height);
            occupied.add(
                    new ResidentSpeechLayout.Bounds(
                            bank.x(), bank.y(), bank.width(), bank.height()));
        }
        occupied.add(new ResidentSpeechLayout.Bounds(0, height - 54, width, 54));
        long now = System.currentTimeMillis();
        int rendered = 0;
        for (Speaker speaker : speakers) {
            boolean placed = false;
            int previousWidth = -1;
            for (int preferredWidth : List.of(240, 180, 120, 96)) {
                int panelWidth = Math.min(metrics.width(), preferredWidth);
                if (panelWidth == previousWidth) continue;
                previousWidth = panelWidth;
                var fitted =
                        new ResidentSpeechLayout.Metrics(
                                panelWidth, metrics.lineHeight(), metrics.linesPerPage());
                List<FormattedCharSequence> lines =
                        client.font.split(
                                Component.literal(speaker.speech().text()), fitted.textWidth());
                var page =
                        ResidentSpeechLayout.page(
                                lines,
                                fitted,
                                now - speaker.speech().started(),
                                speaker.speech().expires() - speaker.speech().started());
                var bounds =
                        ResidentSpeechLayout.place(
                                width,
                                height,
                                speaker.x(),
                                speaker.y(),
                                fitted.width(),
                                page.height(),
                                occupied);
                if (bounds == null) continue;
                draw(graphics, bounds, page, fitted);
                occupied.add(bounds);
                placed = true;
                break;
            }
            if (placed && ++rendered == 3) break;
        }
    }

    private static void draw(
            GuiGraphicsExtractor graphics,
            ResidentSpeechLayout.Bounds bounds,
            ResidentSpeechLayout.Page<FormattedCharSequence> page,
            ResidentSpeechLayout.Metrics metrics) {
        var font = Minecraft.getInstance().font;
        int x = bounds.x(), y = bounds.y();
        graphics.fill(x, y, x + bounds.width(), y + bounds.height(), 0xEC171B1D);
        graphics.outline(x, y, bounds.width(), bounds.height(), 0xFF829A8D);
        graphics.enableScissor(x + 1, y + 1, x + bounds.width() - 1, y + bounds.height() - 1);
        for (int i = 0; i < page.lines().size(); i++) {
            graphics.text(
                    font,
                    page.lines().get(i),
                    x + ResidentSpeechLayout.PADDING,
                    y + ResidentSpeechLayout.PADDING + i * metrics.lineHeight(),
                    0xFFFFFFFF,
                    false);
        }
        if (page.count() > 1) {
            graphics.text(
                    font,
                    (page.index() + 1) + "/" + page.count(),
                    x + ResidentSpeechLayout.PADDING,
                    y + bounds.height() - 12,
                    0xFFABC0B2,
                    false);
        }
        graphics.disableScissor();
    }
}

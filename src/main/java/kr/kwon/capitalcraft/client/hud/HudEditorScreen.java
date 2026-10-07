package kr.kwon.capitalcraft.client.hud;

import com.mojang.blaze3d.platform.InputConstants;

import kr.kwon.capitalcraft.client.economy.BankHud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class HudEditorScreen extends Screen {
    private HudSettings draft = BankHud.settings();
    private boolean dragging;
    private double offsetX, offsetY;
    private String error = "";

    public HudEditorScreen() {
        super(Component.literal("HUD 편집"));
    }

    @Override
    protected void init() {
        dragging = false;
        HudSettings.Bounds bounds = draft.bounds(width, height);
        draft.move(bounds.x(), Math.max(56, bounds.y()), width, height);
        addRenderableWidget(
                Checkbox.builder(Component.literal("중앙 은행"), font)
                        .pos(8, 28)
                        .selected(draft.bankVisible)
                        .onValueChange((box, value) -> draft.bankVisible = value)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.literal("초기화"),
                                button -> {
                                    draft = new HudSettings();
                                    rebuildWidgets();
                                })
                        .bounds(width - 158, 27, 54, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.literal("취소"), button -> onClose())
                        .bounds(width - 100, 27, 44, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.literal("저장"),
                                button -> {
                                    try {
                                        BankHud.save(draft);
                                        onClose();
                                    } catch (IOException exception) {
                                        error = "설정을 저장하지 못했습니다.";
                                    }
                                })
                        .bounds(width - 52, 27, 44, 20)
                        .build());
    }

    private HudSettings.Bounds previewBounds() {
        return draft.bounds(width, height);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x60000000);
        for (int x = 0; x < width; x += 16) graphics.fill(x, 56, x + 1, height, 0x15FFFFFF);
        for (int y = 56; y < height; y += 16) graphics.fill(0, y, width, y + 1, 0x15FFFFFF);
        graphics.fill(0, 0, width, 54, 0xE0202020);
        graphics.text(font, title, 8, 9, 0xFFFFFFFF, false);
        if (draft.bankVisible) {
            HudSettings.Bounds bounds = previewBounds();
            BankHud.draw(graphics, bounds, true);
            int x = bounds.x(),
                    y = bounds.y(),
                    right = x + bounds.width(),
                    bottom = y + bounds.height();
            int color = dragging || bounds.contains(mouseX, mouseY) ? 0xFFFFFFFF : 0xFF888888;
            graphics.fill(x, y, right, y + 1, color);
            graphics.fill(x, bottom - 1, right, bottom, color);
            graphics.fill(x, y, x + 1, bottom, color);
            graphics.fill(right - 1, y, right, bottom, color);
        }
        if (!error.isEmpty()) graphics.text(font, error, 8, height - 16, 0xFFFF8E8E, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        HudSettings.Bounds bounds = previewBounds();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && draft.bankVisible
                && bounds.contains(event.x(), event.y())) {
            dragging = true;
            offsetX = event.x() - bounds.x();
            offsetY = event.y() - bounds.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double y =
                    Math.max(56, Math.min(height - HudSettings.BANK_HEIGHT, event.y() - offsetY));
            draft.move(event.x() - offsetX, y, width, height);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE
                && (event.modifiers() & InputConstants.MOD_ALT) != 0) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package kr.kwon.capitalcraft.client.resident;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;
import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ResidentDialogueScreen extends Screen {
    private final int entityId;
    private final UUID entityUuid;
    private final String residentId;
    private final String residentName;
    private String draft = "";
    private String error = "";
    private boolean submitted;
    private EditBox messageInput;
    private Button sendButton;
    private Bounds panel;

    public record Bounds(int x, int y, int width, int height) {}

    public ResidentDialogueScreen(int entityId, UUID entityUuid, String residentId, String name) {
        super(Component.literal(name + " / 대화"));
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.residentId = residentId;
        this.residentName = name;
    }

    public static Bounds bounds(int width, int height) {
        int w = Math.min(420, Math.max(0, width - 16));
        int h = Math.min(142, Math.max(0, height - 16));
        return new Bounds((width - w) / 2, (height - h) / 2, w, h);
    }

    @Override
    protected void init() {
        panel = bounds(width, height);
        int left = panel.x() + 12;
        int innerWidth = Math.max(1, panel.width() - 24);
        messageInput = new EditBox(font, left, panel.y() + 40, innerWidth, 20,
                Component.literal("대화 내용"));
        messageInput.setMaxLength(ResidentDialogueInput.MAX_LENGTH);
        messageInput.setHint(Component.literal("대화 내용"));
        messageInput.setValue(draft);
        messageInput.setResponder(value -> {
            draft = value;
            error = "";
            refreshControls();
        });
        addRenderableWidget(messageInput);
        int buttonWidth = Math.max(1, (innerWidth - 8) / 2);
        int buttonY = panel.y() + panel.height() - 32;
        addRenderableWidget(Button.builder(Component.literal("취소"), button -> onClose())
                .bounds(left, buttonY, buttonWidth, 20).build());
        sendButton = addRenderableWidget(Button.builder(Component.literal("전송"), button -> submit())
                .bounds(left + innerWidth - buttonWidth, buttonY, buttonWidth, 20).build());
        setInitialFocus(messageInput);
        refreshControls();
    }

    private String targetError() {
        if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
            return "서버 연결이 끊어졌습니다.";
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof Villager) || !entityUuid.equals(entity.getUUID())
                || !entity.isAlive() || entity.isRemoved()
                || !residentId.equals(ResidentClientState.residentId(entityUuid))) {
            return "대화할 주민을 찾을 수 없습니다.";
        }
        if (minecraft.player.isSpectator()) return "관전 상태에서는 대화할 수 없습니다.";
        if (minecraft.player.distanceToSqr(entity) > 16 * 16) {
            return "주민으로부터 16블록 안에서 대화하세요.";
        }
        return "";
    }

    private void refreshControls() {
        if (sendButton != null) {
            sendButton.active = !submitted && !draft.isBlank() && targetError().isEmpty();
        }
    }

    @Override
    public void tick() {
        refreshControls();
    }

    private void submit() {
        if (submitted) return;
        error = targetError();
        if (!error.isEmpty()) return;
        try {
            String command = ResidentDialogueInput.command(residentId, draft);
            minecraft.getConnection().sendCommand(command);
            submitted = true;
            onClose();
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        } catch (RuntimeException exception) {
            CapitalCraftClientMod.LOGGER.warn("Failed to send resident dialogue", exception);
            error = "대화를 보내지 못했습니다. 다시 시도하세요.";
        }
        refreshControls();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)
                && messageInput.isFocused()) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x60000000);
        int x = panel.x(), y = panel.y(), right = x + panel.width();
        graphics.fill(x, y, right, y + panel.height(), 0xE0202424);
        graphics.fill(x, y, right, y + 1, 0xFF6AC6A7);
        String count = draft.length() + "/" + ResidentDialogueInput.MAX_LENGTH;
        int countWidth = font.width(count);
        graphics.text(font, font.plainSubstrByWidth(residentName + " / 대화",
                Math.max(0, panel.width() - 32 - countWidth)), x + 12, y + 17, 0xFFFFFFFF, false);
        graphics.text(font, count, right - 12 - countWidth, y + 17, 0xFFAABCB5, false);
        String status = targetError();
        if (status.isEmpty()) status = error;
        int statusY = y + 66;
        int maxLines = Math.min(2, Math.max(0, (y + panel.height() - 36 - statusY) / font.lineHeight));
        var lines = font.split(Component.literal(status), Math.max(1, panel.width() - 24));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(font, lines.get(i), x + 12, statusY + i * font.lineHeight, 0xFFFF9999, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}

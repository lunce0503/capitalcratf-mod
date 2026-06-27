package kr.kwon.capitalcraft.client.economy.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.kwon.capitalcraft.client.network.CapitalCraftNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TradeScreen extends Screen {
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 4;
    private static final int GRID_SIZE = SLOT_SIZE * 3 + SLOT_GAP * 2;

    private final SlotView[] leftSlots = SlotView.emptyGrid();
    private final SlotView[] rightSlots = SlotView.emptyGrid();
    private String status = "거래할 플레이어를 바라본 뒤 금액 또는 아이템을 올리세요.";
    private String leftName = "나";
    private String rightName = "상대";
    private boolean active;
    private boolean leftAccepted;
    private boolean rightAccepted;
    private EditBox moneyInput;
    private EditBox itemAmountInput;
    private Button acceptButton;
    private Button rightAcceptStatusButton;

    public TradeScreen() {
        super(Component.literal("CapitalCraft Trade"));
    }

    @Override
    protected void init() {
        int panelWidth = 360;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(18, (this.height - 248) / 2);

        moneyInput = new EditBox(this.font, left + 20, top + 144, 92, 20, Component.literal("금액"));
        moneyInput.setMaxLength(18);
        moneyInput.setHint(Component.literal("금액"));
        addRenderableWidget(moneyInput);

        addRenderableWidget(Button.builder(Component.literal("돈 올리기"), button -> submitMoney())
            .bounds(left + 118, top + 144, 76, 20)
            .build());

        itemAmountInput = new EditBox(this.font, left + 20, top + 170, 92, 20, Component.literal("수량"));
        itemAmountInput.setMaxLength(3);
        itemAmountInput.setHint(Component.literal("수량"));
        itemAmountInput.setValue("1");
        addRenderableWidget(itemAmountInput);

        addRenderableWidget(Button.builder(Component.literal("손 아이템"), button -> submitItem())
            .bounds(left + 118, top + 170, 76, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("비우기"), button -> {
                status = "내 거래 제안을 비우는 중입니다.";
                CapitalCraftNetwork.clearTradeOffer();
            })
            .bounds(left + 20, top + 196, 82, 20)
            .build());

        acceptButton = Button.builder(Component.literal("내 수락"), button -> {
                status = "거래 수락 요청 중입니다.";
                CapitalCraftNetwork.acceptTrade();
            })
            .bounds(left + 112, top + 196, 82, 20)
            .build();
        addRenderableWidget(acceptButton);

        addRenderableWidget(Button.builder(Component.literal("취소"), button -> {
                status = "거래 취소 요청 중입니다.";
                CapitalCraftNetwork.cancelTrade();
            })
            .bounds(left + 204, top + 196, 62, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("거절"), button -> {
                status = "거래 거절 요청 중입니다.";
                CapitalCraftNetwork.denyTrade();
            })
            .bounds(left + 276, top + 196, 62, 20)
            .build());

        rightAcceptStatusButton = Button.builder(Component.literal("상대 대기"), button -> {
            })
            .bounds(left + 222, top + 144, 116, 20)
            .build();
        rightAcceptStatusButton.active = false;
        addRenderableWidget(rightAcceptStatusButton);

        CapitalCraftNetwork.setOpenTradeScreen(this);
        CapitalCraftNetwork.requestTradeState();
    }

    @Override
    public void removed() {
        CapitalCraftNetwork.clearOpenTradeScreen(this);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x90000000);
        int panelWidth = 360;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(18, (this.height - 248) / 2);
        int right = left + panelWidth;
        int bottom = top + 248;
        int leftGridX = left + 38;
        int rightGridX = right - 38 - GRID_SIZE;
        int gridY = top + 48;

        graphics.fill(left, top, right, bottom, 0xE0101216);
        graphics.fill(left, top, right, top + 1, active ? 0xFF69D38B : 0xFF76808F);
        graphics.drawString(this.font, "CapitalCraft 거래", left + 20, top + 16, 0xFFFFFFFF, false);
        graphics.drawString(this.font, active ? "거래 진행 중" : "대기 중", right - 82, top + 16, active ? 0xFF69D38B : 0xFFB8C0CC, false);

        graphics.drawString(this.font, leftName, leftGridX, top + 34, 0xFFFFD36B, false);
        graphics.drawString(this.font, rightName, rightGridX, top + 34, 0xFFFFD36B, false);
        drawGrid(graphics, leftGridX, gridY, leftSlots, true);
        drawGrid(graphics, rightGridX, gridY, rightSlots, false);

        graphics.drawString(this.font, "내 제안", left + 20, top + 130, 0xFFB8C0CC, false);
        graphics.drawString(this.font, "상대 수락 상태", left + 222, top + 130, 0xFFB8C0CC, false);
        graphics.drawString(this.font, status, left + 20, bottom - 20, 0xFFB8C0CC, false);

        if (acceptButton != null) {
            acceptButton.setMessage(Component.literal(leftAccepted ? "수락 완료" : "내 수락"));
        }
        if (rightAcceptStatusButton != null) {
            rightAcceptStatusButton.setMessage(Component.literal(rightAccepted ? "상대 수락 완료" : "상대 대기"));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void updateTradeState(JsonObject payload) {
        active = bool(payload, "active", false);
        readSide(payload.getAsJsonObject("left"), true);
        readSide(payload.getAsJsonObject("right"), false);
        if (!active) {
            status = "거래할 플레이어를 바라본 뒤 금액 또는 아이템을 올리세요.";
        } else {
            status = "양쪽 모두 수락하면 거래가 진행됩니다.";
        }
    }

    private void submitMoney() {
        String amount = moneyInput.getValue().trim();
        if (amount.isEmpty()) {
            status = "금액을 입력하세요.";
            return;
        }
        status = "내 거래 금액을 올리는 중입니다.";
        CapitalCraftNetwork.offerTradeMoney(amount);
    }

    private void submitItem() {
        String amount = itemAmountInput.getValue().trim();
        if (amount.isEmpty()) {
            status = "아이템 수량을 입력하세요.";
            return;
        }
        status = "주 손 아이템을 거래창에 올리는 중입니다.";
        CapitalCraftNetwork.offerTradeItem(amount);
    }

    private void readSide(JsonObject side, boolean left) {
        if (side == null) {
            side = new JsonObject();
        }
        SlotView[] target = left ? leftSlots : rightSlots;
        String name = string(side, "playerName", left ? "나" : "상대");
        boolean accepted = bool(side, "accepted", false);
        if (left) {
            leftName = name;
            leftAccepted = accepted;
        } else {
            rightName = name;
            rightAccepted = accepted;
        }

        for (int index = 0; index < target.length; index++) {
            target[index] = SlotView.empty();
        }
        if (!side.has("slots") || !side.get("slots").isJsonArray()) {
            return;
        }
        JsonArray slots = side.getAsJsonArray("slots");
        int count = Math.min(target.length, slots.size());
        for (int index = 0; index < count; index++) {
            if (!slots.get(index).isJsonObject()) {
                continue;
            }
            JsonObject slot = slots.get(index).getAsJsonObject();
            target[index] = new SlotView(
                string(slot, "type", "empty"),
                string(slot, "label", ""),
                string(slot, "detail", "")
            );
        }
    }

    private void drawGrid(GuiGraphics graphics, int x, int y, SlotView[] slots, boolean ownSide) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                int slotX = x + column * (SLOT_SIZE + SLOT_GAP);
                int slotY = y + row * (SLOT_SIZE + SLOT_GAP);
                drawSlot(graphics, slotX, slotY, slots[index], ownSide);
            }
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, SlotView slot, boolean ownSide) {
        int border = ownSide ? 0xFF566272 : 0xFF6B5A49;
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, border);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF171B21);
        if ("empty".equals(slot.type)) {
            return;
        }
        int color = "money".equals(slot.type) ? 0xFFFFD36B : 0xFFE6E6E6;
        String label = compact(slot.label, 4);
        graphics.drawCenteredString(this.font, label, x + SLOT_SIZE / 2, y + 5, color);
        graphics.drawCenteredString(this.font, compact(slot.detail, 5), x + SLOT_SIZE / 2, y + 15, 0xFFB8C0CC);
    }

    private String compact(String value, int max) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private boolean bool(JsonObject object, String key, boolean fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private record SlotView(String type, String label, String detail) {
        static SlotView empty() {
            return new SlotView("empty", "", "");
        }

        static SlotView[] emptyGrid() {
            SlotView[] slots = new SlotView[9];
            for (int index = 0; index < slots.length; index++) {
                slots[index] = empty();
            }
            return slots;
        }
    }
}

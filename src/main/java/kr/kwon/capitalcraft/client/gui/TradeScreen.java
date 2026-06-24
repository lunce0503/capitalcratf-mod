package kr.kwon.capitalcraft.client.gui;

import kr.kwon.capitalcraft.client.network.CapitalCraftNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TradeScreen extends Screen {
    private String status = "바라보는 플레이어에게 거래를 제안할 수 있습니다.";
    private EditBox payAmountInput;
    private EditBox sellPriceInput;
    private EditBox sellAmountInput;

    public TradeScreen() {
        super(Component.literal("CapitalCraft Trade"));
    }

    @Override
    protected void init() {
        int panelWidth = 300;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(24, (this.height - 232) / 2);

        payAmountInput = new EditBox(this.font, left + 20, top + 56, 126, 20, Component.literal("지급 금액"));
        payAmountInput.setMaxLength(18);
        payAmountInput.setHint(Component.literal("지급 금액"));
        addRenderableWidget(payAmountInput);

        addRenderableWidget(Button.builder(Component.literal("돈 지급 제안"), button -> submitPayOffer())
            .bounds(left + 154, top + 56, 126, 20)
            .build());

        sellPriceInput = new EditBox(this.font, left + 20, top + 106, 126, 20, Component.literal("판매 가격"));
        sellPriceInput.setMaxLength(18);
        sellPriceInput.setHint(Component.literal("판매 가격"));
        addRenderableWidget(sellPriceInput);

        sellAmountInput = new EditBox(this.font, left + 154, top + 106, 50, 20, Component.literal("수량"));
        sellAmountInput.setMaxLength(3);
        sellAmountInput.setHint(Component.literal("수량"));
        sellAmountInput.setValue("1");
        addRenderableWidget(sellAmountInput);

        addRenderableWidget(Button.builder(Component.literal("아이템 판매 제안"), button -> submitSellOffer())
            .bounds(left + 212, top + 106, 68, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("수락"), button -> {
                status = "거래 수락 요청 중입니다.";
                CapitalCraftNetwork.acceptTrade();
            })
            .bounds(left + 20, top + 158, 80, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("거절"), button -> {
                status = "거래 거절 요청 중입니다.";
                CapitalCraftNetwork.denyTrade();
            })
            .bounds(left + 110, top + 158, 80, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("취소"), button -> {
                status = "거래 취소 요청 중입니다.";
                CapitalCraftNetwork.cancelTrade();
            })
            .bounds(left + 200, top + 158, 80, 20)
            .build());

        CapitalCraftNetwork.setOpenTradeScreen(this);
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
        int panelWidth = 340;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(18, (this.height - 248) / 2);
        int right = left + panelWidth;
        int bottom = top + 248;

        graphics.fill(left, top, right, bottom, 0xE0101216);
        graphics.fill(left, top, right, top + 1, 0xFF69D38B);
        graphics.drawString(this.font, "CapitalCraft 거래", left + 20, top + 18, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "대상: 5블록 안에서 바라보는 플레이어", left + 20, top + 36, 0xFFB8C0CC, false);
        graphics.drawString(this.font, "돈 지급", left + 20, top + 90, 0xFFFFD36B, false);
        graphics.drawString(this.font, "아이템 판매: 주 손 아이템", left + 20, top + 140, 0xFFFFD36B, false);
        graphics.drawString(this.font, status, left + 20, bottom - 24, 0xFFB8C0CC, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private void submitPayOffer() {
        String amount = payAmountInput.getValue().trim();
        if (amount.isEmpty()) {
            status = "지급 금액을 입력하세요.";
            return;
        }
        status = "돈 지급 거래를 제안하는 중입니다.";
        CapitalCraftNetwork.requestTradePay(amount);
    }

    private void submitSellOffer() {
        String price = sellPriceInput.getValue().trim();
        String amount = sellAmountInput.getValue().trim();
        if (price.isEmpty()) {
            status = "판매 가격을 입력하세요.";
            return;
        }
        if (amount.isEmpty()) {
            status = "판매 수량을 입력하세요.";
            return;
        }
        status = "아이템 판매 거래를 제안하는 중입니다.";
        CapitalCraftNetwork.requestTradeSell(price, amount);
    }
}

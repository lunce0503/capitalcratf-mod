package kr.kwon.capitalcraft.client.gui;

import kr.kwon.capitalcraft.client.network.CapitalCraftNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FinanceScreen extends Screen {
    private String balance = "-";
    private String currency = "VIL";
    private String accountName = "기본 계좌";
    private String accountStatus = "조회 중";
    private String status = "서버에서 잔액을 불러오는 중입니다.";
    private EditBox targetInput;
    private EditBox amountInput;
    private EditBox memoInput;

    public FinanceScreen() {
        super(Component.literal("CapitalCraft Finance"));
    }

    @Override
    protected void init() {
        int panelWidth = 260;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(32, (this.height - 180) / 2);

        targetInput = new EditBox(this.font, left, top + 64, panelWidth, 20, Component.literal("받는 플레이어"));
        targetInput.setMaxLength(32);
        targetInput.setHint(Component.literal("받는 플레이어"));
        addRenderableWidget(targetInput);

        amountInput = new EditBox(this.font, left, top + 92, panelWidth, 20, Component.literal("금액"));
        amountInput.setMaxLength(18);
        amountInput.setHint(Component.literal("금액"));
        addRenderableWidget(amountInput);

        memoInput = new EditBox(this.font, left, top + 120, panelWidth, 20, Component.literal("메모"));
        memoInput.setMaxLength(40);
        memoInput.setHint(Component.literal("메모"));
        addRenderableWidget(memoInput);

        addRenderableWidget(Button.builder(Component.literal("송금"), button -> submitTransfer())
            .bounds(left, top + 150, 124, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("새로고침"), button -> {
                status = "잔액을 다시 조회하는 중입니다.";
                CapitalCraftNetwork.requestBalance();
            })
            .bounds(left + 136, top + 150, 124, 20)
            .build());

        CapitalCraftNetwork.setOpenFinanceScreen(this);
        CapitalCraftNetwork.requestBalance();
    }

    @Override
    public void removed() {
        CapitalCraftNetwork.clearOpenFinanceScreen(this);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = 300;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(20, (this.height - 220) / 2);
        int right = left + panelWidth;
        int bottom = top + 220;

        graphics.fill(left, top, right, bottom, 0xE0101216);
        graphics.fill(left, top, right, top + 1, 0xFF5EA1FF);
        graphics.drawString(this.font, "CapitalCraft 금융", left + 20, top + 18, 0xFFFFFFFF, false);
        graphics.drawString(this.font, accountName + " / " + accountStatus, left + 20, top + 40, 0xFFB8C0CC, false);
        graphics.drawString(this.font, currency + " " + balance, left + 20, top + 58, 0xFFFFD36B, false);
        graphics.drawString(this.font, status, left + 20, bottom - 24, 0xFFB8C0CC, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void updateBalance(String balance, String currency, String accountName, String accountStatus) {
        this.balance = balance;
        this.currency = currency;
        this.accountName = accountName;
        this.accountStatus = accountStatus;
        this.status = "잔액 조회 완료";
    }

    public void updateTransferResult(String message, String newBalance) {
        this.balance = newBalance;
        this.status = message;
        if (amountInput != null) {
            amountInput.setValue("");
        }
        if (memoInput != null) {
            memoInput.setValue("");
        }
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private void submitTransfer() {
        String target = targetInput.getValue().trim();
        String amount = amountInput.getValue().trim();
        String memo = memoInput.getValue().trim();
        if (target.isEmpty()) {
            status = "받는 플레이어를 입력하세요.";
            return;
        }
        if (amount.isEmpty()) {
            status = "금액을 입력하세요.";
            return;
        }
        status = "송금 요청 중입니다.";
        CapitalCraftNetwork.requestTransfer(target, amount, memo);
    }
}

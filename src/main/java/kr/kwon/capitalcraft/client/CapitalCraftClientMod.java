package kr.kwon.capitalcraft.client;

import kr.kwon.capitalcraft.client.automotive.VehicleHud;
import kr.kwon.capitalcraft.client.automotive.VehicleInputController;
import kr.kwon.capitalcraft.client.foodindustry.butchery.ButcheryHud;
import kr.kwon.capitalcraft.client.economy.gui.FinanceScreen;
import kr.kwon.capitalcraft.client.economy.gui.TradeScreen;
import kr.kwon.capitalcraft.client.network.CapitalCraftNetwork;
import kr.kwon.capitalcraft.client.network.FinancePayload;
import kr.kwon.capitalcraft.client.resident.ResidentClientState;
import kr.kwon.capitalcraft.client.resident.render.ResidentRendering;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CapitalCraftClientMod implements ClientModInitializer {
    public static final String MOD_ID = "capitalcraft-mod";
    public static final String RESOURCE_NAMESPACE = "capitalcraft";
    public static final String MOD_VERSION = "0.9.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category KEY_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("capitalcraft", "finance"));
    private static KeyMapping financeKey;
    private static KeyMapping tradeKey;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.serverboundPlay().register(FinancePayload.TYPE, FinancePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FinancePayload.TYPE, FinancePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(FinancePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> CapitalCraftNetwork.handle(payload.json()));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CapitalCraftNetwork.reset();
            CapitalCraftNetwork.queueHello();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CapitalCraftNetwork.reset();
            ResidentClientState.reset();
        });
        ButcheryHud.register();
        VehicleHud.register();
        ResidentRendering.register();

        financeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.capitalcraft.finance",
            InputConstants.KEY_V,
            KEY_CATEGORY
        ));
        tradeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.capitalcraft.trade",
            InputConstants.KEY_G,
            KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CapitalCraftNetwork.tick();
            VehicleInputController.tick(client);
            while (financeKey.consumeClick()) {
                openFinanceScreen(client);
            }
            while (tradeKey.consumeClick()) {
                openTradeScreen(client);
            }
        });
    }

    private static void openFinanceScreen(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.gui.screen() instanceof FinanceScreen) {
            return;
        }
        try {
            client.setScreenAndShow(new FinanceScreen());
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to open CapitalCraft finance screen", exception);
            client.player.sendSystemMessage(
                Component.literal("CapitalCraft 금융 화면을 열 수 없습니다. 로그를 확인해 주세요."));
        }
    }

    private static void openTradeScreen(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.gui.screen() instanceof TradeScreen) {
            return;
        }
        try {
            client.setScreenAndShow(new TradeScreen());
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to open CapitalCraft trade screen", exception);
            client.player.sendSystemMessage(
                Component.literal("CapitalCraft 거래 화면을 열 수 없습니다. 로그를 확인해 주세요."));
        }
    }
}

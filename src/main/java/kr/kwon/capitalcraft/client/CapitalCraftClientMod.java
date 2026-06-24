package kr.kwon.capitalcraft.client;

import kr.kwon.capitalcraft.client.gui.FinanceScreen;
import kr.kwon.capitalcraft.client.network.CapitalCraftNetwork;
import kr.kwon.capitalcraft.client.network.FinancePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CapitalCraftClientMod implements ClientModInitializer {
    public static final String MOD_ID = "capitalcraft-mod";
    public static final String MOD_VERSION = "0.1.0";
    private static final KeyMapping.Category KEY_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("capitalcraft", "finance"));
    private static KeyMapping financeKey;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(FinancePayload.TYPE, FinancePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FinancePayload.TYPE, FinancePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(FinancePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> CapitalCraftNetwork.handle(payload.json()));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CapitalCraftNetwork.reset();
            CapitalCraftNetwork.sendHello();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CapitalCraftNetwork.reset());

        financeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.capitalcraft.finance",
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (financeKey.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.setScreen(new FinanceScreen());
                }
            }
        });
    }
}

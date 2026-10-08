package kr.kwon.capitalcraft.client.resident;

import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ResidentDialogueController {
    private ResidentDialogueController() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!level.isClientSide() || player.isSpectator()
                    || hand != InteractionHand.MAIN_HAND
                    || !(entity instanceof Villager) || !entity.isAlive()) {
                return InteractionResult.PASS;
            }
            String residentId = ResidentClientState.residentId(entity.getUUID());
            if (residentId == null) return InteractionResult.PASS;
            Minecraft client = Minecraft.getInstance();
            if (client.player != player || client.gui.screen() != null) return InteractionResult.PASS;
            String name = ResidentClientState.name(entity.getUUID());
            try {
                client.setScreenAndShow(new ResidentDialogueScreen(
                        entity.getId(), entity.getUUID(), residentId,
                        name == null ? entity.getName().getString() : name));
            } catch (RuntimeException exception) {
                CapitalCraftClientMod.LOGGER.error("Failed to open resident dialogue", exception);
                player.sendSystemMessage(Component.literal("주민 대화창을 열 수 없습니다."));
            }
            // SUCCESS/CONSUME would also send the vanilla interaction packet and
            // trigger the server's automatic greeting before the player types.
            return InteractionResult.FAIL;
        });
    }
}

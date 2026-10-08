package kr.kwon.capitalcraft.client.resident;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.Bootstrap;

public final class ResidentDialogueVerification {
    private static int checks;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        check(ResidentDialogueInput.command("mari", "  안녕하세요  ")
                .equals("resident talk mari 안녕하세요"), "targeted Korean command and trim");
        check(ResidentDialogueInput.command("seia", "/resident delete mari")
                .equals("resident talk seia /resident delete mari"), "message cannot replace command prefix");
        check(ResidentDialogueInput.command("mari-1_2", "가".repeat(500)).length() > 500,
                "server's 500 UTF-16 unit message limit");
        check(ResidentDialogueInput.command("mari", "hello \ud83d\ude00").endsWith("hello \ud83d\ude00"),
                "valid supplementary characters");
        for (String message : List.of("안녕하세요", "가".repeat(500), "\ud83d\ude00".repeat(250))) {
            var bytes = Unpooled.buffer();
            try {
                var buffer = new FriendlyByteBuf(bytes);
                String command = ResidentDialogueInput.command("mari", message);
                ServerboundChatCommandPacket.STREAM_CODEC.encode(buffer, new ServerboundChatCommandPacket(command));
                check(ServerboundChatCommandPacket.STREAM_CODEC.decode(buffer).command().equals(command),
                        "native command packet preserves Korean/supplementary text and 500-unit limit");
            } finally {
                bytes.release();
            }
        }
        for (String id : List.of("", "../mari", "MARI", "mari other", "a".repeat(25), "mari\nstop")) {
            reject(id, "hello");
        }
        reject(null, "hello");
        reject("mari", null);
        for (String message : List.of("", " ", "가".repeat(501), "hi\nstop", "hi\rstop",
                "hi\tstop", "hi\u0000", "hi\u007f", "hi\u00a7c", "hi\ud800", "hi\udc00")) {
            reject("mari", message);
        }
        UUID id = UUID.randomUUID();
        JsonObject resident = new JsonObject();
        resident.addProperty("entityUuid", id.toString());
        resident.addProperty("appearance", "vanilla");
        resident.addProperty("name", "마리");
        resident.addProperty("residentId", "mari");
        JsonArray entries = new JsonArray();
        entries.add(resident);
        entries.add("malformed");
        JsonObject payload = new JsonObject();
        payload.add("residents", entries);
        ResidentClientState.sync(payload);
        check("mari".equals(ResidentClientState.residentId(id)), "registered vanilla residents can talk");
        check(ResidentClientState.residentId(UUID.randomUUID()) == null, "ordinary villagers remain unchanged");
        resident.addProperty("appearance", "seia");
        resident.addProperty("residentId", "seia");
        ResidentClientState.sync(payload);
        check("seia".equals(ResidentClientState.residentId(id)), "snapshot replaces target ID");
        resident.addProperty("residentId", "mari stop");
        ResidentClientState.sync(payload);
        check(ResidentClientState.residentId(id) == null, "malformed server IDs cannot construct commands");
        check("마리".equals(ResidentClientState.name(id)), "invalid ID does not remove name or appearance");
        resident.addProperty("residentId", true);
        ResidentClientState.sync(payload);
        check(ResidentClientState.residentId(id) == null, "non-string IDs are not command targets");
        resident.remove("residentId");
        ResidentClientState.sync(payload);
        check(ResidentClientState.residentId(id) == null, "old snapshot safely falls back");
        resident.addProperty("residentId", "mari");
        ResidentClientState.sync(payload);
        ResidentClientState.sync(new JsonObject());
        check(ResidentClientState.residentId(id) == null, "empty snapshot removes dialogue target");
        ResidentClientState.sync(payload);
        ResidentClientState.reset();
        check(ResidentClientState.residentId(id) == null, "disconnect removes dialogue target");
        for (int width : List.of(160, 240, 320, 427, 854, 1920)) {
            for (int height : List.of(120, 180, 240, 480, 1080)) {
                var bounds = ResidentDialogueScreen.bounds(width, height);
                check(bounds.x() >= 8 && bounds.y() >= 8
                        && bounds.x() + bounds.width() <= width - 8
                        && bounds.y() + bounds.height() <= height - 8, "panel within viewport");
                check(bounds.width() >= 136 && bounds.height() >= 104, "usable field/button dimensions");
                check(bounds.y() + 60 <= bounds.y() + bounds.height() - 36,
                        "input above status and buttons");
            }
        }
        System.out.println("PASS resident dialogue command/state/layout: " + checks + " checks");
    }

    private static void reject(String id, String message) {
        try {
            ResidentDialogueInput.command(id, message);
            throw new AssertionError("Invalid dialogue accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
        checks++;
    }
}

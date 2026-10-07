package kr.kwon.capitalcraft.client.hud;

import com.google.gson.JsonObject;

import kr.kwon.capitalcraft.client.economy.BankClientState;
import kr.kwon.capitalcraft.client.resident.ResidentClientState;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public final class ClientHudVerification {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        HudSettings settings = new HudSettings();
        for (int width : List.of(320, 427, 854, 1920)) {
            for (int height : List.of(180, 240, 480, 1080)) {
                settings.move(-500, -500, width, height);
                check(
                        settings.bounds(width, height).x() == 0
                                && settings.bounds(width, height).y() == 0,
                        "top-left clamp");
                settings.move(10000, 10000, width, height);
                var bounds = settings.bounds(width, height);
                check(
                        bounds.x() + bounds.width() == width
                                && bounds.y() + bounds.height() == height,
                        "bottom-right clamp");
                check(
                        bounds.contains(bounds.x(), bounds.y()) && !bounds.contains(width, height),
                        "drag hit box");
            }
        }
        var path = Files.createTempDirectory("capitalcraft-hud-").resolve("settings.json");
        settings.bankVisible = false;
        settings.bankX = 0.25;
        settings.bankY = 0.75;
        settings.save(path);
        HudSettings loaded = HudSettings.load(path);
        check(
                !loaded.bankVisible && loaded.bankX == 0.25 && loaded.bankY == 0.75,
                "HUD persistence");
        HudSettings draft = loaded.copy();
        draft.bankVisible = true;
        draft.move(0, 0, 320, 180);
        check(!loaded.bankVisible && loaded.bankX == 0.25, "editor draft/cancel isolation");
        Files.writeString(path, "{\"bankX\":999,\"bankY\":-5}");
        loaded = HudSettings.load(path);
        check(loaded.bankX == 1 && loaded.bankY == 0, "loaded coordinate validation");
        Files.writeString(path, "invalid");
        try {
            HudSettings.load(path);
            throw new AssertionError("Corrupt settings accepted");
        } catch (java.io.IOException expected) {
            checks++;
        }

        BankClientState.reset();
        JsonObject balance = new JsonObject();
        balance.addProperty("balance", "9223372036854775807");
        balance.addProperty("currency", "VIL");
        balance.addProperty("playerName", "BankTester");
        BankClientState.sync(balance);
        check(
                BankClientState.ready()
                        && BankClientState.amount().equals("9,223,372,036,854,775,807 VIL"),
                "exact 64-bit balance");
        check(BankClientState.playerName().equals("BankTester"), "player name snapshot");
        balance.addProperty("balance", "NaN");
        BankClientState.sync(balance);
        check(BankClientState.amount().startsWith("9,223"), "invalid balance ignored");
        BankClientState.reset();
        check(
                !BankClientState.ready() && BankClientState.playerName().isEmpty(),
                "disconnect clears bank data");

        UUID id = UUID.randomUUID();
        JsonObject resident = new JsonObject();
        resident.addProperty("entityUuid", id.toString());
        resident.addProperty("appearance", "vanilla");
        resident.addProperty("speech", "moving speech");
        resident.addProperty("name", "Resident");
        resident.addProperty("speechRemainingMs", 10000);
        JsonObject payload = new JsonObject();
        com.google.gson.JsonArray entries = new com.google.gson.JsonArray();
        entries.add(resident);
        entries.add("malformed entry");
        payload.add("residents", entries);
        ResidentClientState.sync(payload);
        check("Resident".equals(ResidentClientState.name(id)), "original name separated from legacy speech label");
        check(
                ResidentClientState.speech(id).equals("moving speech"),
                "vanilla resident speech sync");
        resident.addProperty("speechRemainingMs", 0);
        ResidentClientState.sync(payload);
        check(ResidentClientState.speech(id).isEmpty(), "speech expiry");
        resident.addProperty("appearance", "mari");
        resident.addProperty("speechRemainingMs", 10000);
        ResidentClientState.sync(payload);
        check(ResidentClientState.appearance(id).equals("mari"), "appearance preserved");
        ResidentClientState.reset();
        check(ResidentClientState.speech(id).isEmpty(), "disconnect clears speech");
        check(ResidentClientState.name(id) == null, "disconnect clears resident name");
        resident.remove("name");
        ResidentClientState.sync(payload);
        check(ResidentClientState.name(id) == null && ResidentClientState.speech(id).equals("moving speech"), "older server snapshot still supports speech");
        ResidentClientState.sync(new JsonObject());
        check(ResidentClientState.speech(id).isEmpty() && ResidentClientState.name(id) == null, "empty snapshot removes resident state");

        System.out.println("PASS client HUD/name-tag verification: " + checks + " checks");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
        checks++;
    }
}

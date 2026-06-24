package kr.kwon.capitalcraft.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import kr.kwon.capitalcraft.client.gui.FinanceScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class CapitalCraftNetwork {
    private static final Gson GSON = new Gson();
    private static final int PROTOCOL = 1;
    private static FinanceScreen openFinanceScreen;
    private static boolean handshakeAccepted;

    private CapitalCraftNetwork() {
    }

    public static void reset() {
        handshakeAccepted = false;
        openFinanceScreen = null;
    }

    public static void setOpenFinanceScreen(FinanceScreen screen) {
        openFinanceScreen = screen;
    }

    public static void clearOpenFinanceScreen(FinanceScreen screen) {
        if (openFinanceScreen == screen) {
            openFinanceScreen = null;
        }
    }

    public static void sendHello() {
        JsonObject payload = new JsonObject();
        payload.addProperty("modId", CapitalCraftClientMod.MOD_ID);
        payload.addProperty("modVersion", CapitalCraftClientMod.MOD_VERSION);
        payload.addProperty("minecraftVersion", "1.21.11");
        payload.add("features", GSON.toJsonTree(new String[] {"finance_ui"}));
        send("C2S_HELLO", "hello-" + UUID.randomUUID(), payload);
    }

    public static void requestBalance() {
        if (!send("C2S_BALANCE_REQUEST", "balance-" + UUID.randomUUID(), new JsonObject())) {
            updateScreenStatus("서버 금융 채널이 아직 준비되지 않았습니다.");
        }
    }

    public static void requestTransfer(String targetPlayerName, String amount, String memo) {
        JsonObject payload = new JsonObject();
        payload.addProperty("targetPlayerName", targetPlayerName);
        payload.addProperty("amount", amount);
        payload.addProperty("memo", memo);
        if (!send("C2S_TRANSFER_REQUEST", "transfer-" + UUID.randomUUID(), payload)) {
            updateScreenStatus("서버 금융 채널이 아직 준비되지 않았습니다.");
        }
    }

    public static void handle(String rawJson) {
        try {
            handlePayload(rawJson);
        } catch (RuntimeException exception) {
            CapitalCraftClientMod.LOGGER.error("Failed to handle CapitalCraft finance payload: {}", rawJson, exception);
            updateScreenStatus("서버 응답 처리 중 오류가 발생했습니다.");
        }
    }

    private static void handlePayload(String rawJson) {
        JsonElement parsed = JsonParser.parseString(rawJson);
        if (!parsed.isJsonObject()) {
            updateScreenStatus("서버 응답을 해석하지 못했습니다.");
            return;
        }

        JsonObject root = parsed.getAsJsonObject();
        String type = string(root, "type", "");
        JsonObject payload = root.has("payload") && root.get("payload").isJsonObject()
            ? root.getAsJsonObject("payload")
            : new JsonObject();

        switch (type) {
            case "S2C_HELLO_ACK" -> {
                handshakeAccepted = bool(payload, "accepted", false);
                updateScreenStatus(handshakeAccepted ? "서버 연결 완료" : "서버 연결 거부");
            }
            case "S2C_BALANCE_RESPONSE" -> {
                if (openFinanceScreen != null) {
                    openFinanceScreen.updateBalance(
                        string(payload, "balance", "0"),
                        string(payload, "currency", "VIL"),
                        string(payload, "accountName", "기본 계좌"),
                        string(payload, "accountStatus", "ACTIVE")
                    );
                }
            }
            case "S2C_TRANSFER_RESULT" -> {
                boolean success = bool(payload, "success", false);
                if (success) {
                    String balance = string(payload, "fromBalance", "0");
                    if (openFinanceScreen != null) {
                        openFinanceScreen.updateTransferResult("송금 완료", balance);
                    }
                } else {
                    updateScreenStatus(string(payload, "message", "송금 실패"));
                }
            }
            case "S2C_BALANCE_UPDATED" -> {
                if (openFinanceScreen != null) {
                    openFinanceScreen.updateBalance(
                        string(payload, "balance", "0"),
                        string(payload, "currency", "VIL"),
                        string(payload, "accountName", "기본 계좌"),
                        string(payload, "accountStatus", "ACTIVE")
                    );
                    openFinanceScreen.setStatus("잔액이 갱신되었습니다.");
                }
            }
            case "S2C_ERROR" -> updateScreenStatus(string(payload, "message", "서버 오류"));
            default -> {
            }
        }
    }

    private static boolean send(String type, String requestId, JsonObject payload) {
        if (Minecraft.getInstance().getConnection() == null) {
            return false;
        }
        try {
            if (!ClientPlayNetworking.canSend(FinancePayload.TYPE)) {
                return false;
            }
        } catch (RuntimeException exception) {
            return false;
        }

        JsonObject root = new JsonObject();
        root.addProperty("protocol", PROTOCOL);
        root.addProperty("type", type);
        root.addProperty("requestId", requestId);
        root.add("payload", payload);
        try {
            ClientPlayNetworking.send(new FinancePayload(GSON.toJson(root)));
            return true;
        } catch (RuntimeException exception) {
            CapitalCraftClientMod.LOGGER.warn("Failed to send CapitalCraft finance packet: {}", type, exception);
            return false;
        }
    }

    private static void updateScreenStatus(String message) {
        if (openFinanceScreen != null) {
            openFinanceScreen.setStatus(message);
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}

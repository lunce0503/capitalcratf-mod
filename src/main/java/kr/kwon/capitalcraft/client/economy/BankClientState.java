package kr.kwon.capitalcraft.client.economy;

import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

public final class BankClientState {
    private static boolean ready;
    private static String amount = "-";
    private static String currency = "VIL";
    private static String playerName = "";

    private BankClientState() {}

    public static void sync(JsonObject payload) {
        if (!payload.has("balance")) return;
        try {
            long balance = Long.parseLong(payload.get("balance").getAsString());
            String nextCurrency =
                    payload.has("currency") ? payload.get("currency").getAsString() : "VIL";
            if (!nextCurrency.matches("[A-Za-z0-9_]{1,16}")) return;
            amount = NumberFormat.getIntegerInstance(Locale.KOREA).format(balance);
            currency = nextCurrency;
            if (payload.has("playerName")) {
                String name = payload.get("playerName").getAsString();
                if (name.matches("[A-Za-z0-9_]{1,16}")) playerName = name;
            }
            ready = true;
        } catch (RuntimeException ignored) {
            // Retain the last valid snapshot rather than showing malformed money.
        }
    }

    public static boolean ready() {
        return ready;
    }

    public static String amount() {
        return amount + " " + currency;
    }

    public static String playerName() {
        return playerName;
    }

    public static void reset() {
        ready = false;
        amount = "-";
        currency = "VIL";
        playerName = "";
    }
}

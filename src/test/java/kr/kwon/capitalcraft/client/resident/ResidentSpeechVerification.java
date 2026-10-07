package kr.kwon.capitalcraft.client.resident;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ResidentSpeechVerification {
    private static int checks;

    public static void main(String[] args) {
        StringSplitter splitter = new StringSplitter((character, style) -> character > 127 ? 9 : 6);
        List<String> samples =
                List.of(
                        "짧은 대사입니다.",
                        "가".repeat(240),
                        "W".repeat(240),
                        "줄바꿈과 공백이 있는 긴 대사입니다. ".repeat(10),
                        "😀".repeat(120));
        for (int width : List.of(160, 320, 427, 854, 1920)) {
            for (int height : List.of(120, 180, 240, 480, 1080)) {
                var metrics = ResidentSpeechLayout.metrics(width, height, 9);
                check(
                        metrics.width() <= width - 16 && metrics.textWidth() > 0,
                        "responsive panel width");
                for (String text : samples) {
                    List<FormattedText> lines =
                            splitter.splitLines(text, metrics.textWidth(), Style.EMPTY);
                    for (var line : lines)
                        check(
                                splitter.stringWidth(line) <= metrics.textWidth(),
                                "native wrapping fits text width");
                    int pageCount =
                            (lines.size() + metrics.linesPerPage() - 1) / metrics.linesPerPage();
                    List<FormattedText> collected = new ArrayList<>();
                    int stableHeight = -1;
                    for (int index = 0; index < pageCount; index++) {
                        var page =
                                ResidentSpeechLayout.page(
                                        lines,
                                        metrics,
                                        (long) ((index + 0.5) * 24000 / pageCount),
                                        24000);
                        check(
                                page.index() == index && page.count() == pageCount,
                                "every page shown in order");
                        check(page.lines().size() <= metrics.linesPerPage(), "page line capacity");
                        check(page.height() <= height - 16, "panel fits viewport height");
                        if (stableHeight != -1)
                            check(
                                    page.height() == stableHeight,
                                    "page changes preserve panel dimensions");
                        stableHeight = page.height();
                        collected.addAll(page.lines());
                        for (double x :
                                List.of(
                                        -2000.0,
                                        0.0,
                                        width / 2.0,
                                        (double) width,
                                        width + 2000.0)) {
                            for (double y :
                                    List.of(
                                            -2000.0,
                                            0.0,
                                            height / 2.0,
                                            (double) height,
                                            height + 2000.0)) {
                                var bounds =
                                        ResidentSpeechLayout.place(
                                                width,
                                                height,
                                                x,
                                                y,
                                                metrics.width(),
                                                page.height(),
                                                List.of());
                                check(
                                        bounds != null
                                                && bounds.x() >= 8
                                                && bounds.y() >= 8
                                                && bounds.x() + bounds.width() <= width - 8
                                                && bounds.y() + bounds.height() <= height - 8,
                                        "all anchors stay within viewport");
                            }
                        }
                    }
                    check(collected.equals(lines), "pagination loses no wrapped lines");
                    check(
                            ResidentSpeechLayout.page(lines, metrics, -100, 24000).index() == 0,
                            "negative time clamped");
                    check(
                            ResidentSpeechLayout.page(lines, metrics, 25000, 24000).index()
                                    == pageCount - 1,
                            "final page retained");
                }
            }
        }
        var metrics = ResidentSpeechLayout.metrics(854, 480, 9);
        var name = new ResidentSpeechLayout.Bounds(400, 200, 60, 14);
        List<ResidentSpeechLayout.Bounds> occupied = new ArrayList<>(List.of(name));
        for (int i = 0; i < 3; i++) {
            var bubble =
                    ResidentSpeechLayout.place(854, 480, 430, 200, metrics.width(), 98, occupied);
            check(
                    bubble != null && occupied.stream().noneMatch(bubble::overlaps),
                    "speakers and name labels do not overlap");
            occupied.add(bubble);
        }
        var blocked = new ResidentSpeechLayout.Bounds(0, 0, 320, 180);
        var bank = new ResidentSpeechLayout.Bounds(140, 54, 180, 60);
        var hotbar = new ResidentSpeechLayout.Bounds(0, 126, 320, 54);
        var narrow = ResidentSpeechLayout.place(320, 180, 160, 90, 120, 54, List.of(bank, hotbar));
        check(
                narrow != null && !narrow.overlaps(bank) && !narrow.overlaps(hotbar),
                "narrow bubble avoids bank and game HUD at large GUI scale");
        var longName = new ResidentSpeechLayout.Bounds(70, 80, 180, 12);
        var corner =
                ResidentSpeechLayout.place(
                        320, 180, 160, 90, 96, 54, List.of(bank, hotbar, longName));
        check(
                corner != null
                        && !corner.overlaps(bank)
                        && !corner.overlaps(hotbar)
                        && !corner.overlaps(longName),
                "corner space found despite a long name and large GUI scale");
        check(
                ResidentSpeechLayout.place(320, 180, 160, 90, 240, 60, List.of(blocked)) == null,
                "crowded viewport skips bubble rather than overlaps");
        check(
                ResidentSpeechLayout.place(320, 180, Double.NaN, 90, 240, 60, List.of()) == null,
                "invalid projection rejected");
        check(
                ResidentSpeechLayout.place(20, 20, 0, 0, 240, 60, List.of()) == null,
                "impossibly small viewport rejected");

        UUID id = UUID.randomUUID();
        JsonObject resident = new JsonObject();
        resident.addProperty("entityUuid", id.toString());
        resident.addProperty("appearance", "seia");
        resident.addProperty("name", "세이아");
        resident.addProperty("speech", "페이지가 여러 개인 대사입니다. ".repeat(8));
        resident.addProperty("speechRemainingMs", 15000);
        JsonArray residents = new JsonArray();
        residents.add(resident);
        JsonObject snapshot = new JsonObject();
        snapshot.add("residents", residents);
        ResidentClientState.sync(snapshot);
        var first = ResidentClientState.speeches().get(id);
        ResidentClientState.sync(snapshot);
        check(
                ResidentClientState.speeches().get(id).started() == first.started(),
                "repeated snapshots do not restart pagination");
        check(
                ResidentClientState.name(id).equals("세이아")
                        && ResidentClientState.speech(id).equals(first.text()),
                "name and speech kept separate");
        ResidentClientState.reset();
        check(ResidentClientState.speeches().isEmpty(), "disconnect clears HUD speakers");
        System.out.println("PASS resident speech layout/pagination: " + checks + " checks");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
        checks++;
    }
}

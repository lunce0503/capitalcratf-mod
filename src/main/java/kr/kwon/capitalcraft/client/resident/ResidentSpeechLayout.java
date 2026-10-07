package kr.kwon.capitalcraft.client.resident;

import java.util.ArrayList;
import java.util.List;

public final class ResidentSpeechLayout {
    public static final int MARGIN = 8;
    public static final int PADDING = 10;
    public static final int GAP = 8;

    private ResidentSpeechLayout() {}

    public record Metrics(int width, int lineHeight, int linesPerPage) {
        public int textWidth() {
            return width - PADDING * 2;
        }
    }

    public record Page<T>(List<T> lines, int index, int count, int height) {}

    public record Bounds(int x, int y, int width, int height) {
        public boolean overlaps(Bounds other) {
            return x < other.x + other.width + GAP
                    && x + width + GAP > other.x
                    && y < other.y + other.height + GAP
                    && y + height + GAP > other.y;
        }
    }

    public static Metrics metrics(int width, int height, int fontHeight) {
        int lineHeight = Math.max(1, fontHeight) + 2;
        int panelHeight = Math.min(110, Math.max(1, height / 3));
        int capacity = Math.max(1, Math.min(6, (panelHeight - PADDING * 2 - 12) / lineHeight));
        return new Metrics(Math.min(240, Math.max(24, width - MARGIN * 2)), lineHeight, capacity);
    }

    public static <T> Page<T> page(List<T> lines, Metrics metrics, long elapsed, long duration) {
        int capacity = metrics.linesPerPage();
        int count = Math.max(1, (lines.size() + capacity - 1) / capacity);
        double progress = Math.clamp((double) elapsed / Math.max(1, duration), 0, 1);
        int index = Math.min(count - 1, (int) (progress * count));
        int from = Math.min(lines.size(), index * capacity);
        int to = Math.min(lines.size(), from + capacity);
        // Page changes must not move the panel or other speakers' panels.
        int height =
                PADDING * 2
                        + Math.min(capacity, lines.size()) * metrics.lineHeight()
                        + (count > 1 ? 12 : 0);
        return new Page<>(List.copyOf(lines.subList(from, to)), index, count, height);
    }

    public static Bounds place(
            int width,
            int height,
            double anchorX,
            double anchorY,
            int panelWidth,
            int panelHeight,
            List<Bounds> occupied) {
        if (width < panelWidth + MARGIN * 2
                || height < panelHeight + MARGIN * 2
                || !Double.isFinite(anchorX)
                || !Double.isFinite(anchorY)) return null;
        int x = (int) Math.round(anchorX - panelWidth / 2.0);
        int above = (int) Math.round(anchorY - panelHeight - 12);
        int below = (int) Math.round(anchorY + 18);
        List<Integer> columns = new ArrayList<>(List.of(x, MARGIN, width - panelWidth - MARGIN));
        List<Integer> rows =
                new ArrayList<>(List.of(above, below, MARGIN, height - panelHeight - MARGIN));
        for (Bounds existing : occupied) {
            columns.add(existing.x - panelWidth - GAP);
            columns.add(existing.x + existing.width + GAP);
            rows.add(existing.y - panelHeight - GAP);
            rows.add(existing.y + existing.height + GAP);
        }
        for (int row : rows.stream().distinct().toList()) {
            for (int column : columns.stream().distinct().toList()) {
                Bounds candidate = clamp(width, height, column, row, panelWidth, panelHeight);
                if (occupied.stream().noneMatch(candidate::overlaps)) return candidate;
            }
        }
        return null;
    }

    private static Bounds clamp(
            int width, int height, int x, int y, int panelWidth, int panelHeight) {
        return new Bounds(
                Math.clamp(x, MARGIN, width - panelWidth - MARGIN),
                Math.clamp(y, MARGIN, height - panelHeight - MARGIN),
                panelWidth,
                panelHeight);
    }
}

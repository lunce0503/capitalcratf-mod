package kr.kwon.capitalcraft.client.resident;

public final class ResidentDialogueInput {
    public static final int MAX_LENGTH = 500;

    private ResidentDialogueInput() {}

    public static boolean validResidentId(String id) {
        return id != null && id.matches("[a-z0-9_-]{1,24}");
    }

    public static String command(String residentId, String rawMessage) {
        if (!validResidentId(residentId)) {
            throw new IllegalArgumentException("대화할 주민을 찾을 수 없습니다.");
        }
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("대화 내용을 입력하세요.");
        }
        String message = rawMessage.strip();
        if (message.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("대화는 500자 이하로 입력하세요.");
        }
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isISOControl(c) || c == '\u00a7') {
                throw new IllegalArgumentException("줄바꿈이나 제어 문자는 사용할 수 없습니다.");
            }
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= message.length() || !Character.isLowSurrogate(message.charAt(++i))) {
                    throw new IllegalArgumentException("올바른 문자를 입력하세요.");
                }
            } else if (Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("올바른 문자를 입력하세요.");
            }
        }
        return "resident talk " + residentId + " " + message;
    }
}

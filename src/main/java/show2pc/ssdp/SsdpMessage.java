package show2pc.ssdp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SsdpMessage {
    private final String startLine;
    private final Map<String, String> headers;

    private SsdpMessage(String startLine, Map<String, String> headers) {
        this.startLine = startLine;
        this.headers = headers;
    }

    public static SsdpMessage parse(String raw) {
        String[] lines = raw.replace("\r", "").split("\n");
        String startLine = lines.length == 0 ? "" : lines[0].trim();
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int index = lines[i].indexOf(':');
            if (index > 0) {
                headers.put(lines[i].substring(0, index).trim().toUpperCase(Locale.ROOT), lines[i].substring(index + 1).trim());
            }
        }
        return new SsdpMessage(startLine, headers);
    }

    public boolean isSearch() {
        return startLine.toUpperCase(Locale.ROOT).startsWith("M-SEARCH");
    }

    public String st() {
        return headers.getOrDefault("ST", "");
    }
}

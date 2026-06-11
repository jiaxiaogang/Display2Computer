package show2pc.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventLog {
    private static final int MAX_EVENTS = 200;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ArrayDeque<String> events = new ArrayDeque<>();

    public synchronized void add(String message) {
        String event = LocalDateTime.now().format(FORMATTER) + " " + message;
        System.out.println(event);
        events.addFirst(event);
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    public synchronized List<String> recent() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}

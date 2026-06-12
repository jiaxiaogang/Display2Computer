package show2pc.player;

import java.time.Duration;

public interface PlaybackController {
    void load(String uri, String metadata);

    void play();

    void pause();

    void stop();

    void seek(Duration position);

    void setVolume(int volume);

    void setMute(boolean mute);

    String currentUri();

    PlayerState state();

    Duration position();

    Duration duration();

    int volume();

    boolean muted();

    default void updateBrowserStatus(Duration position, Duration duration, boolean paused, boolean ended) {
    }

    default BrowserCommand pollBrowserCommand() {
        return BrowserCommand.none();
    }

    final class BrowserCommand {
        private static final BrowserCommand NONE = new BrowserCommand(false, Duration.ZERO, 0);

        private final boolean seek;
        private final Duration seekPosition;
        private final long sequence;

        private BrowserCommand(boolean seek, Duration seekPosition, long sequence) {
            this.seek = seek;
            this.seekPosition = seekPosition;
            this.sequence = sequence;
        }

        public static BrowserCommand none() {
            return NONE;
        }

        public static BrowserCommand seek(Duration position, long sequence) {
            return new BrowserCommand(true, position, sequence);
        }

        public boolean hasSeek() {
            return seek;
        }

        public Duration seekPosition() {
            return seekPosition;
        }

        public long sequence() {
            return sequence;
        }
    }
}

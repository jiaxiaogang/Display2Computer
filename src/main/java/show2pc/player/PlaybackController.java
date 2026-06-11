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
}

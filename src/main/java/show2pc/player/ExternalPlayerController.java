package show2pc.player;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ExternalPlayerController implements PlaybackController {
    private final String playerCommand;
    private final boolean fullscreen;
    private Process process;
    private String currentUri = "";
    private String metadata = "";
    private PlayerState state = PlayerState.STOPPED;
    private int volume = 80;
    private boolean muted;

    public ExternalPlayerController(String playerCommand, boolean fullscreen) {
        this.playerCommand = playerCommand;
        this.fullscreen = fullscreen;
    }

    @Override
    public synchronized void load(String uri, String metadata) {
        this.currentUri = uri == null ? "" : uri;
        this.metadata = metadata == null ? "" : metadata;
        this.state = PlayerState.STOPPED;
        System.out.println("Loaded media URI: " + this.currentUri);
    }

    @Override
    public synchronized void play() {
        if (currentUri.isEmpty()) {
            System.out.println("Play requested with no current URI");
            return;
        }
        stopProcess();
        List<String> command = buildCommand();
        try {
            process = new ProcessBuilder(command).start();
            state = PlayerState.PLAYING;
            System.out.println("Started external player: " + command);
        } catch (IOException e) {
            state = PlayerState.STOPPED;
            System.err.println("Failed to start external player '" + playerCommand + "': " + e.getMessage());
        }
    }

    @Override
    public synchronized void pause() {
        System.out.println("Pause requested; external process mode does not support reliable pause yet");
        state = PlayerState.PAUSED;
    }

    @Override
    public synchronized void stop() {
        stopProcess();
        state = PlayerState.STOPPED;
    }

    @Override
    public synchronized void seek(Duration position) {
        System.out.println("Seek requested to " + position + "; external process mode does not support reliable seek yet");
    }

    @Override
    public synchronized void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        System.out.println("Volume set to " + this.volume + "; external process mode applies this only to reported state");
    }

    @Override
    public synchronized void setMute(boolean mute) {
        this.muted = mute;
        System.out.println("Mute set to " + muted + "; external process mode applies this only to reported state");
    }

    @Override
    public synchronized String currentUri() {
        return currentUri;
    }

    @Override
    public synchronized PlayerState state() {
        if (process != null && !process.isAlive() && state == PlayerState.PLAYING) {
            state = PlayerState.STOPPED;
        }
        return state;
    }

    @Override
    public Duration position() {
        return Duration.ZERO;
    }

    @Override
    public Duration duration() {
        return Duration.ZERO;
    }

    @Override
    public synchronized int volume() {
        return volume;
    }

    @Override
    public synchronized boolean muted() {
        return muted;
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        command.add(playerCommand);
        String player = playerCommand.toLowerCase();
        if (fullscreen && (player.contains("chrome") || player.contains("msedge") || player.contains("edge"))) {
            command.add("--new-window");
            command.add("--start-fullscreen");
        } else if (fullscreen && player.contains("vlc")) {
            command.add("--fullscreen");
        } else if (fullscreen && player.contains("mpv")) {
            command.add("--fs");
        }
        command.add(currentUri);
        return command;
    }

    private void stopProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        process = null;
    }
}

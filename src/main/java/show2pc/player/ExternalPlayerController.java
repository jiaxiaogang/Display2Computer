package show2pc.player;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ExternalPlayerController implements PlaybackController {
    private final String playerCommand;
    private final boolean fullscreen;
    private final int httpPort;
    private Process process;
    private String currentUri = "";
    private String metadata = "";
    private PlayerState state = PlayerState.STOPPED;
    private int volume = 80;
    private boolean muted;

    public ExternalPlayerController(String playerCommand, boolean fullscreen, int httpPort) {
        this.playerCommand = playerCommand;
        this.fullscreen = fullscreen;
        this.httpPort = httpPort;
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
            if (isBrowserPlayer()) {
                keepBrowserPlayerOnTop();
            }
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
        if (player.contains("chrome") || player.contains("msedge") || player.contains("edge")) {
            command.add("--user-data-dir=" + System.getProperty("user.home") + "/.show2pc/browser-profile");
            command.add("--no-first-run");
            command.add("--autoplay-policy=no-user-gesture-required");
            Rectangle bounds = usableScreenBounds();
            command.add("--window-position=" + bounds.x + "," + bounds.y);
            command.add("--window-size=" + bounds.width + "," + bounds.height);
            command.add("--app=" + localPlayerUrl());
        } else {
            if (fullscreen && player.contains("vlc")) {
                command.add("--fullscreen");
            } else if (fullscreen && player.contains("mpv")) {
                command.add("--fs");
            }
            command.add(currentUri);
        }
        return command;
    }

    private String localPlayerUrl() {
        return "http://localhost:" + httpPort + "/player?url=" + URLEncoder.encode(currentUri, StandardCharsets.UTF_8);
    }

    private boolean isBrowserPlayer() {
        String player = playerCommand.toLowerCase();
        return player.contains("chrome") || player.contains("msedge") || player.contains("edge");
    }

    private Rectangle usableScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    private void keepBrowserPlayerOnTop() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        String script = "Start-Sleep -Milliseconds 800;" +
                "Add-Type '[DllImport(\"user32.dll\")] public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint uFlags);' -Name Win32 -Namespace Native;" +
                "$p=Get-Process chrome,msedge -ErrorAction SilentlyContinue|Where-Object{$_.MainWindowTitle -eq 'Display2Computer Player'}|Select-Object -First 1;" +
                "if($p){[Native.Win32]::SetWindowPos($p.MainWindowHandle,[IntPtr](-1),0,0,0,0,0x0001 -bor 0x0002)|Out-Null}";
        try {
            new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start();
        } catch (IOException e) {
            System.err.println("Failed to keep browser player on top: " + e.getMessage());
        }
    }

    private void stopProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        process = null;
    }
}

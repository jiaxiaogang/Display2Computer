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
    private static volatile String macDesktopBounds = "unknown";

    private final String playerCommand;
    private final boolean fullscreen;
    private final int httpPort;
    private Process process;
    private String currentUri = "";
    private String metadata = "";
    private PlayerState state = PlayerState.STOPPED;
    private Duration lastPosition = Duration.ZERO;
    private Duration lastDuration = Duration.ZERO;
    private Duration pendingSeek;
    private long seekSequence;
    private int volume = 80;
    private boolean muted;

    public ExternalPlayerController(String playerCommand, boolean fullscreen, int httpPort) {
        this.playerCommand = playerCommand;
        this.fullscreen = fullscreen;
        this.httpPort = httpPort;
        if (isMac()) {
            Thread boundsLoader = new Thread(() -> macDesktopBounds = readMacDesktopBoundsFromCommand(), "mac-desktop-bounds-loader");
            boundsLoader.setDaemon(true);
            boundsLoader.start();
        }
    }

    @Override
    public synchronized void load(String uri, String metadata) {
        this.currentUri = uri == null ? "" : uri;
        this.metadata = metadata == null ? "" : metadata;
        this.state = PlayerState.STOPPED;
        this.lastPosition = Duration.ZERO;
        this.lastDuration = Duration.ZERO;
        this.pendingSeek = null;
        System.out.println("Loaded media URI: " + this.currentUri);
    }

    @Override
    public synchronized void play() {
        if (currentUri.isEmpty()) {
            System.out.println("Play requested with no current URI");
            return;
        }
        stopProcess();
        if (isMacBrowserPlayer()) {
            closeMacBrowserPlayerWindow();
            openMacBrowserPlayer();
            state = PlayerState.PLAYING;
            return;
        }
        if (isBrowserPlayer()) {
            closeBrowserPlayerWindow();
        }
        List<String> command = buildCommand();
        try {
            process = new ProcessBuilder(command).start();
            if (isBrowserPlayer()) {
                bringBrowserPlayerToForeground();
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
        Duration target = normalize(position);
        pendingSeek = target;
        seekSequence++;
        lastPosition = target;
        System.out.println("Seek requested to " + target);
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
        if (process != null && !process.isAlive() && state == PlayerState.PLAYING && !isMacBrowserPlayer()) {
            state = PlayerState.STOPPED;
        }
        return state;
    }

    @Override
    public synchronized Duration position() {
        return lastPosition;
    }

    @Override
    public synchronized Duration duration() {
        return lastDuration;
    }

    @Override
    public synchronized void updateBrowserStatus(Duration position, Duration duration, boolean paused, boolean ended) {
        lastPosition = normalize(position);
        lastDuration = normalize(duration);
        if (ended) {
            state = PlayerState.STOPPED;
        } else if (paused) {
            state = PlayerState.PAUSED;
        } else {
            state = PlayerState.PLAYING;
        }
    }

    @Override
    public synchronized BrowserCommand pollBrowserCommand() {
        if (pendingSeek == null) {
            return BrowserCommand.none();
        }
        BrowserCommand command = BrowserCommand.seek(pendingSeek, seekSequence);
        pendingSeek = null;
        return command;
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
        return player.contains("chrome") || player.contains("msedge") || player.contains("edge") || player.contains("safari");
    }

    private boolean isMacBrowserPlayer() {
        return isMac() && isBrowserPlayer();
    }

    private void openMacBrowserPlayer() {
        List<String> command = playerCommand.toLowerCase().contains("safari") ? macSafariCommand() : macChromeCommand();
        try {
            process = new ProcessBuilder(command).start();
            System.out.println("Started external player: " + command);
        } catch (IOException e) {
            state = PlayerState.STOPPED;
            System.err.println("Failed to start external player '" + playerCommand + "': " + e.getMessage());
        }
    }

    private List<String> macChromeCommand() {
        System.out.println("macOS desktop bounds: " + macDesktopBounds);
        return List.of(
                "open",
                "-na",
                "Google Chrome",
                "--args",
                "--user-data-dir=" + System.getProperty("user.home") + "/.show2pc/browser-profile",
                "--no-first-run",
                "--autoplay-policy=no-user-gesture-required",
                "--window-size=" + macDesktopWindowSize(),
                "--app=" + localPlayerUrl()
        );
    }

    private String macDesktopWindowSize() {
        String[] parts = macDesktopBounds.split(",");
        if (parts.length != 4) {
            return "1440,900";
        }
        try {
            int left = Integer.parseInt(parts[0].trim());
            int top = Integer.parseInt(parts[1].trim());
            int right = Integer.parseInt(parts[2].trim());
            int bottom = Integer.parseInt(parts[3].trim());
            return (right - left) + "," + (bottom - top);
        } catch (NumberFormatException e) {
            return "1440,900";
        }
    }

    private String readMacDesktopBoundsFromCommand() {
        try {
            Process boundsProcess = new ProcessBuilder("osascript", "-e", "tell application \"Finder\" to get bounds of window of desktop").start();
            String bounds = new String(boundsProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            boundsProcess.waitFor();
            return bounds.isEmpty() ? "unknown" : bounds;
        } catch (IOException e) {
            System.err.println("Failed to get macOS desktop bounds: " + e.getMessage());
            return "unknown";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "unknown";
        }
    }

    private void closeMacBrowserPlayerWindow() {
        if (!playerCommand.toLowerCase().contains("safari")) {
            try {
                new ProcessBuilder("pkill", "-f", System.getProperty("user.home") + "/.show2pc/browser-profile").start().waitFor();
                return;
            } catch (IOException e) {
                System.err.println("Failed to stop macOS Chrome player profile: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        String script = "tell application \"Safari\"\n" +
                "  repeat with w in windows\n" +
                "    try\n" +
                "      if URL of active tab of w starts with \"http://localhost:" + httpPort + "/player\" then close w\n" +
                "    end try\n" +
                "  end repeat\n" +
                "end tell";
        try {
            new ProcessBuilder("osascript", "-e", script).start().waitFor();
        } catch (IOException e) {
            System.err.println("Failed to close macOS browser player window: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<String> macSafariCommand() {
        String script = "set playerUrl to \"" + appleScriptString(localPlayerUrl()) + "\"\n" +
                "tell application \"Safari\"\n" +
                "  activate\n" +
                "  set foundTab to false\n" +
                "  repeat with w in windows\n" +
                "    repeat with t in tabs of w\n" +
                "      if URL of t starts with \"http://localhost:" + httpPort + "/player\" then\n" +
                "        set URL of t to playerUrl\n" +
                "        set current tab of w to t\n" +
                "        set index of w to 1\n" +
                "        set foundTab to true\n" +
                "        exit repeat\n" +
                "      end if\n" +
                "    end repeat\n" +
                "    if foundTab then exit repeat\n" +
                "  end repeat\n" +
                "  if not foundTab then open location playerUrl\n" +
                "end tell";
        return List.of("osascript", "-e", script);
    }

    private String appleScriptString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Rectangle usableScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    private Duration normalize(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }

    private void closeBrowserPlayerWindow() {
        if (!isWindows()) {
            return;
        }
        String script = "$p=Get-Process chrome,msedge -ErrorAction SilentlyContinue|Where-Object{$_.MainWindowTitle -eq 'Display2Computer Player'};" +
                "foreach($w in $p){$w.CloseMainWindow()|Out-Null};" +
                "Start-Sleep -Milliseconds 400";
        try {
            new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start().waitFor();
        } catch (IOException e) {
            System.err.println("Failed to close browser player window: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void bringBrowserPlayerToForeground() {
        if (!isWindows()) {
            return;
        }
        String script = "Add-Type '[DllImport(\"user32.dll\")] public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);[DllImport(\"user32.dll\")] public static extern bool BringWindowToTop(IntPtr hWnd);[DllImport(\"user32.dll\")] public static extern bool SetForegroundWindow(IntPtr hWnd);' -Name Win32 -Namespace Native;" +
                "for($i=0;$i -lt 10;$i++){" +
                "$p=Get-Process chrome,msedge -ErrorAction SilentlyContinue|Where-Object{$_.MainWindowTitle -eq 'Display2Computer Player'}|Select-Object -First 1;" +
                "if($p){[Native.Win32]::ShowWindowAsync($p.MainWindowHandle,9)|Out-Null;[Native.Win32]::BringWindowToTop($p.MainWindowHandle)|Out-Null;[Native.Win32]::SetForegroundWindow($p.MainWindowHandle)|Out-Null;break};" +
                "Start-Sleep -Milliseconds 200}";
        try {
            new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start();
        } catch (IOException e) {
            System.err.println("Failed to bring browser player to foreground: " + e.getMessage());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private void stopProcess() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        process = null;
    }
}

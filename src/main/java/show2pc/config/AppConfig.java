package show2pc.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppConfig {
    private final String friendlyName;
    private final String manufacturer;
    private final String modelName;
    private final int httpPort;
    private final String playerCommand;
    private final boolean fullscreen;
    private final Path dataDirectory;

    public AppConfig(String friendlyName, String manufacturer, String modelName, int httpPort, String playerCommand, boolean fullscreen, Path dataDirectory) {
        this.friendlyName = friendlyName;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.httpPort = httpPort;
        this.playerCommand = playerCommand;
        this.fullscreen = fullscreen;
        this.dataDirectory = dataDirectory;
    }

    public static AppConfig fromSystemProperties() {
        String userHome = System.getProperty("user.home");
        Path dataDirectory = Paths.get(System.getProperty("show2pc.dataDir", userHome + "/.show2pc"));
        return new AppConfig(
                System.getProperty("show2pc.name", defaultFriendlyName()),
                System.getProperty("show2pc.manufacturer", "Display2Computer"),
                System.getProperty("show2pc.modelName", "Display2Computer Media Renderer"),
                Integer.getInteger("show2pc.httpPort", 49152),
                System.getProperty("show2pc.player", defaultPlayerCommand()),
                Boolean.parseBoolean(System.getProperty("show2pc.fullscreen", "false")),
                dataDirectory
        );
    }

    private static String defaultFriendlyName() {
        return "Display2(" + System.getProperty("user.name", "User") + "-" + osType() + ")";
    }

    private static String osType() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            return "Win";
        }
        if (osName.contains("mac")) {
            return "Mac";
        }
        return "Linux";
    }

    private static String defaultPlayerCommand() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            return firstExisting(
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    "/Applications/Safari.app/Contents/MacOS/Safari",
                    "/Applications/VLC.app/Contents/MacOS/VLC",
                    "/opt/homebrew/bin/mpv",
                    "/usr/local/bin/mpv",
                    "vlc"
            );
        }
        if (osName.contains("win")) {
            return firstExisting(
                    "C:/Program Files/Google/Chrome/Application/chrome.exe",
                    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
                    "C:/Program Files/Microsoft/Edge/Application/msedge.exe",
                    "C:/Program Files/VideoLAN/VLC/vlc.exe",
                    "C:/Program Files (x86)/VideoLAN/VLC/vlc.exe",
                    System.getProperty("user.home") + "/scoop/apps/vlc/current/vlc.exe",
                    System.getProperty("user.home") + "/scoop/apps/mpv/current/mpv.exe",
                    "chrome.exe",
                    "msedge.exe",
                    "vlc"
            );
        }
        return firstExisting("/usr/bin/vlc", "/usr/local/bin/vlc", "/usr/bin/mpv", "/usr/local/bin/mpv", "vlc");
    }

    private static String firstExisting(String... candidates) {
        for (String candidate : candidates) {
            if (candidate.contains("/") && Files.exists(Paths.get(candidate))) {
                return candidate;
            }
        }
        return candidates[candidates.length - 1];
    }

    public String friendlyName() {
        return friendlyName;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public String modelName() {
        return modelName;
    }

    public int httpPort() {
        return httpPort;
    }

    public String playerCommand() {
        return playerCommand;
    }

    public boolean fullscreen() {
        return fullscreen;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }
}

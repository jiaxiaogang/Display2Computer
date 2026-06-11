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
    private final Path dataDirectory;

    public AppConfig(String friendlyName, String manufacturer, String modelName, int httpPort, String playerCommand, Path dataDirectory) {
        this.friendlyName = friendlyName;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.httpPort = httpPort;
        this.playerCommand = playerCommand;
        this.dataDirectory = dataDirectory;
    }

    public static AppConfig fromSystemProperties() {
        String userHome = System.getProperty("user.home");
        Path dataDirectory = Paths.get(System.getProperty("show2pc.dataDir", userHome + "/.show2pc"));
        return new AppConfig(
                System.getProperty("show2pc.name", "Show2PC"),
                System.getProperty("show2pc.manufacturer", "Show2PC"),
                System.getProperty("show2pc.modelName", "Show2PC Media Renderer"),
                Integer.getInteger("show2pc.httpPort", 49152),
                System.getProperty("show2pc.player", defaultPlayerCommand()),
                dataDirectory
        );
    }

    private static String defaultPlayerCommand() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            return firstExisting(
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

    public Path dataDirectory() {
        return dataDirectory;
    }
}

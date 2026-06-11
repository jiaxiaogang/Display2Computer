package show2pc.ui;

import show2pc.config.AppConfig;
import show2pc.util.EventLog;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.net.URI;

public class TrayController {
    private final AppConfig config;
    private final EventLog eventLog;
    private final Runnable shutdown;
    private TrayIcon trayIcon;

    public TrayController(AppConfig config, EventLog eventLog, Runnable shutdown) {
        this.config = config;
        this.eventLog = eventLog;
        this.shutdown = shutdown;
    }

    public void install() {
        EventQueue.invokeLater(this::installOnEventThread);
    }

    public void remove() {
        EventQueue.invokeLater(this::removeOnEventThread);
    }

    private void installOnEventThread() {
        if (!SystemTray.isSupported()) {
            eventLog.add("System tray is not supported");
            return;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem("Open Status Page");
        open.addActionListener(event -> openStatusPage());
        MenuItem exit = new MenuItem(isMac() ? "Quit Display2Computer" : "Exit Display2Computer");
        exit.addActionListener(event -> {
            removeOnEventThread();
            shutdown.run();
            System.exit(0);
        });
        menu.add(open);
        menu.addSeparator();
        menu.add(exit);

        trayIcon = new TrayIcon(createImage(), "Display2Computer", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> openStatusPage());

        try {
            SystemTray.getSystemTray().add(trayIcon);
            eventLog.add("System tray icon installed");
        } catch (AWTException e) {
            eventLog.add("Failed to install system tray icon: " + e.getMessage());
        }
    }

    private void removeOnEventThread() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private void openStatusPage() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + config.httpPort() + "/"));
            }
        } catch (Exception e) {
            eventLog.add("Failed to open status page: " + e.getMessage());
        }
    }

    private Image createImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(46, 125, 50));
        graphics.fillOval(1, 1, 14, 14);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString("S", 4, 12);
        graphics.dispose();
        return image;
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}

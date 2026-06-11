package show2pc;

import show2pc.config.AppConfig;
import show2pc.config.DeviceIdentity;
import show2pc.player.ExternalPlayerController;
import show2pc.player.PlaybackController;
import show2pc.services.avtransport.AVTransportService;
import show2pc.services.connection.ConnectionManagerService;
import show2pc.services.rendering.RenderingControlService;
import show2pc.ssdp.SsdpServer;
import show2pc.upnp.UpnpHttpServer;
import show2pc.ui.TrayController;
import show2pc.util.EventLog;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {
        configureMacUiMode();

        AppConfig config = AppConfig.fromSystemProperties();
        DeviceIdentity identity = DeviceIdentity.loadOrCreate(config.dataDirectory());
        EventLog eventLog = new EventLog();
        PlaybackController player = new ExternalPlayerController(config.playerCommand(), config.fullscreen(), config.httpPort());

        UpnpHttpServer httpServer = new UpnpHttpServer(config, identity, player, eventLog);
        httpServer.register("/upnp/control/AVTransport", new AVTransportService(player, eventLog));
        httpServer.register("/upnp/control/RenderingControl", new RenderingControlService(player, eventLog));
        httpServer.register("/upnp/control/ConnectionManager", new ConnectionManagerService(eventLog));
        httpServer.start();

        SsdpServer ssdpServer = new SsdpServer(config, identity, eventLog);
        ssdpServer.start();

        CountDownLatch stopSignal = new CountDownLatch(1);
        final boolean[] stopped = {false};
        Runnable shutdown = () -> {
            synchronized (stopped) {
                if (stopped[0]) {
                    return;
                }
                stopped[0] = true;
            }
            System.out.println("Shutting down Display2Computer");
            ssdpServer.stop();
            httpServer.stop();
            stopSignal.countDown();
        };

        TrayController trayController = new TrayController(config, eventLog, shutdown);
        trayController.install();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            trayController.remove();
            shutdown.run();
        }));

        System.out.println("Display2Computer is running. Open http://localhost:" + config.httpPort() + "/");
        eventLog.add("Display2Computer started with UDN " + identity.udn());
        stopSignal.await();
    }

    private static void configureMacUiMode() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            System.setProperty("java.awt.headless", "false");
            System.setProperty("apple.awt.UIElement", "true");
        }
    }
}

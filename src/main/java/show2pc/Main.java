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
import show2pc.util.EventLog;

import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromSystemProperties();
        DeviceIdentity identity = DeviceIdentity.loadOrCreate(config.dataDirectory());
        EventLog eventLog = new EventLog();
        PlaybackController player = new ExternalPlayerController(config.playerCommand());

        UpnpHttpServer httpServer = new UpnpHttpServer(config, identity, player, eventLog);
        httpServer.register("/upnp/control/AVTransport", new AVTransportService(player, eventLog));
        httpServer.register("/upnp/control/RenderingControl", new RenderingControlService(player, eventLog));
        httpServer.register("/upnp/control/ConnectionManager", new ConnectionManagerService(eventLog));
        httpServer.start();

        SsdpServer ssdpServer = new SsdpServer(config, identity, eventLog);
        ssdpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Show2PC");
            ssdpServer.stop();
            httpServer.stop();
        }));

        System.out.println("Show2PC is running. Open http://localhost:" + config.httpPort() + "/");
        eventLog.add("Show2PC started with UDN " + identity.udn());
        new CountDownLatch(1).await();
    }
}

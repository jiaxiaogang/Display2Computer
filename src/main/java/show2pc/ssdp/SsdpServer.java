package show2pc.ssdp;

import show2pc.config.AppConfig;
import show2pc.config.DeviceIdentity;
import show2pc.util.EventLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SsdpServer {
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int SSDP_PORT = 1900;

    private final AppConfig config;
    private final DeviceIdentity identity;
    private final EventLog eventLog;
    private final NetworkInterfaceSelector interfaceSelector = new NetworkInterfaceSelector();
    private final Set<String> supportedSearchTargets = new HashSet<>(Arrays.asList(
            "ssdp:all",
            "upnp:rootdevice",
            "urn:schemas-upnp-org:device:mediarenderer:1",
            "urn:schemas-upnp-org:service:avtransport:1",
            "urn:schemas-upnp-org:service:renderingcontrol:1",
            "urn:schemas-upnp-org:service:connectionmanager:1"
    ));

    private volatile boolean running;
    private MulticastSocket socket;
    private ScheduledExecutorService scheduler;

    public SsdpServer(AppConfig config, DeviceIdentity identity, EventLog eventLog) {
        this.config = config;
        this.identity = identity;
        this.eventLog = eventLog;
    }

    public void start() throws IOException {
        running = true;
        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        socket = new MulticastSocket(SSDP_PORT);
        socket.setReuseAddress(true);
        for (InetAddress address : interfaceSelector.activeIpv4Addresses()) {
            try {
                socket.joinGroup(new java.net.InetSocketAddress(group, SSDP_PORT), NetworkInterface.getByInetAddress(address));
                System.out.println("Joined SSDP multicast on " + address.getHostAddress());
                eventLog.add("SSDP joined " + address.getHostAddress());
            } catch (IOException e) {
                System.err.println("Failed to join SSDP multicast on " + address.getHostAddress() + ": " + e.getMessage());
            }
        }

        Thread listener = new Thread(this::listen, "ssdp-listener");
        listener.setDaemon(true);
        listener.start();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> sendNotify("ssdp:alive"), 0, TimeUnit.SECONDS);
        scheduler.schedule(() -> sendNotify("ssdp:alive"), 2, TimeUnit.SECONDS);
        scheduler.schedule(() -> sendNotify("ssdp:alive"), 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> sendNotify("ssdp:alive"), 30, 30, TimeUnit.SECONDS);
        eventLog.add("SSDP server started");
    }

    public void stop() {
        running = false;
        sendNotify("ssdp:byebye");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (socket != null) {
            socket.close();
        }
    }

    private void listen() {
        byte[] buffer = new byte[8192];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String raw = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                SsdpMessage message = SsdpMessage.parse(raw);
                String st = message.st();
                if (message.isSearch()) {
                    if (supports(st)) {
                        String respSt = responseSearchTarget(st);
                        eventLog.add("SSDP M-SEARCH " + respSt + " from " + packet.getAddress().getHostAddress());
                        respond(packet, respSt);
                        eventLog.add("SSDP response sent " + respSt + " to " + packet.getAddress().getHostAddress());
                    } else if (!st.isEmpty()) {
                        eventLog.add("SSDP M-SEARCH (unsupported: " + st + ") from " + packet.getAddress().getHostAddress());
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("SSDP receive failed: " + e.getMessage());
                }
            }
        }
    }

    private boolean supports(String st) {
        String normalized = normalizeSearchTarget(st);
        return supportedSearchTargets.contains(normalized) || normalizeSearchTarget(identity.udn()).equals(normalized);
    }

    private String normalizeSearchTarget(String st) {
        return st == null ? "" : st.trim().toLowerCase(Locale.ROOT);
    }

    private String responseSearchTarget(String st) {
        String normalized = normalizeSearchTarget(st);
        if (normalizeSearchTarget(identity.udn()).equals(normalized)) {
            return identity.udn();
        }
        if ("urn:schemas-upnp-org:device:mediarenderer:1".equals(normalized)) {
            return "urn:schemas-upnp-org:device:MediaRenderer:1";
        }
        if ("urn:schemas-upnp-org:service:avtransport:1".equals(normalized)) {
            return "urn:schemas-upnp-org:service:AVTransport:1";
        }
        if ("urn:schemas-upnp-org:service:renderingcontrol:1".equals(normalized)) {
            return "urn:schemas-upnp-org:service:RenderingControl:1";
        }
        if ("urn:schemas-upnp-org:service:connectionmanager:1".equals(normalized)) {
            return "urn:schemas-upnp-org:service:ConnectionManager:1";
        }
        return normalized;
    }

    private void respond(DatagramPacket request, String st) throws IOException {
        String locationIp = chooseLocationAddress(request.getAddress());
        String response = "HTTP/1.1 200 OK\r\n" +
                "CACHE-CONTROL: max-age=1800\r\n" +
                "EXT:\r\n" +
                "LOCATION: http://" + locationIp + ":" + config.httpPort() + "/device.xml\r\n" +
                "SERVER: Java/" + System.getProperty("java.version") + " UPnP/1.0 Display2Computer/0.1\r\n" +
                "ST: " + st + "\r\n" +
                "USN: " + usn(st) + "\r\n" +
                "\r\n";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(bytes, bytes.length, request.getAddress(), request.getPort()));
    }

    private void sendNotify(String nts) {
        try {
            List<InetAddress> addresses = interfaceSelector.activeIpv4Addresses();
            if ("ssdp:alive".equals(nts)) {
                eventLog.add("SSDP NOTIFY alive on " + addresses.size() + " interface(s)");
            }
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            for (InetAddress address : addresses) {
                for (String nt : Arrays.asList(
                        "upnp:rootdevice",
                        identity.udn(),
                        "urn:schemas-upnp-org:device:MediaRenderer:1",
                        "urn:schemas-upnp-org:service:AVTransport:1",
                        "urn:schemas-upnp-org:service:RenderingControl:1",
                        "urn:schemas-upnp-org:service:ConnectionManager:1")) {
                    String message = "NOTIFY * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "CACHE-CONTROL: max-age=1800\r\n" +
                            "LOCATION: http://" + address.getHostAddress() + ":" + config.httpPort() + "/device.xml\r\n" +
                            "NT: " + nt + "\r\n" +
                            "NTS: " + nts + "\r\n" +
                            "SERVER: Java/" + System.getProperty("java.version") + " UPnP/1.0 Display2Computer/0.1\r\n" +
                            "USN: " + usn(nt) + "\r\n\r\n";
                    byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(bytes, bytes.length, group, SSDP_PORT));
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Failed to send SSDP NOTIFY: " + e.getMessage());
            }
        }
    }

    private String chooseLocationAddress(InetAddress requester) throws IOException {
        List<InetAddress> addresses = interfaceSelector.activeIpv4Addresses();
        if (addresses.isEmpty()) {
            return InetAddress.getLocalHost().getHostAddress();
        }
        byte[] requested = requester.getAddress();
        for (InetAddress address : addresses) {
            byte[] candidate = address.getAddress();
            if (candidate.length == requested.length && candidate[0] == requested[0] && candidate[1] == requested[1]) {
                return address.getHostAddress();
            }
        }
        return addresses.get(0).getHostAddress();
    }

    private String usn(String st) {
        if (identity.udn().equals(st) || "ssdp:all".equals(st)) {
            return identity.udn();
        }
        if ("upnp:rootdevice".equals(st)) {
            return identity.udn() + "::upnp:rootdevice";
        }
        return identity.udn() + "::" + st;
    }
}

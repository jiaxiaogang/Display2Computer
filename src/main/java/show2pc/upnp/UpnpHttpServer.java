package show2pc.upnp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import show2pc.config.AppConfig;
import show2pc.config.DeviceIdentity;
import show2pc.player.PlaybackController;
import show2pc.ssdp.NetworkInterfaceSelector;
import show2pc.upnp.SoapActionHandler;
import show2pc.util.EventLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class UpnpHttpServer {
    private final AppConfig config;
    private final DeviceDescription deviceDescription;
    private final PlaybackController player;
    private final EventLog eventLog;
    private final NetworkInterfaceSelector interfaceSelector = new NetworkInterfaceSelector();
    private final Map<String, SoapActionHandler> handlers = new HashMap<>();
    private HttpServer server;

    public UpnpHttpServer(AppConfig config, DeviceIdentity identity, PlaybackController player, EventLog eventLog) {
        this.config = config;
        this.deviceDescription = new DeviceDescription(config, identity);
        this.player = player;
        this.eventLog = eventLog;
    }

    public void register(String path, SoapActionHandler handler) {
        handlers.put(path, handler);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", config.httpPort()), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/play", this::handleManualPlay);
        server.createContext("/device.xml", this::handleDeviceDescription);
        server.createContext("/upnp/scpd/AVTransport.xml", exchange -> handleResource(exchange, "upnp/avtransport-scpd.xml", "text/xml; charset=utf-8"));
        server.createContext("/upnp/scpd/RenderingControl.xml", exchange -> handleResource(exchange, "upnp/rendering-control-scpd.xml", "text/xml; charset=utf-8"));
        server.createContext("/upnp/scpd/ConnectionManager.xml", exchange -> handleResource(exchange, "upnp/connection-manager-scpd.xml", "text/xml; charset=utf-8"));
        server.createContext("/upnp/control/AVTransport", this::handleSoap);
        server.createContext("/upnp/control/RenderingControl", this::handleSoap);
        server.createContext("/upnp/control/ConnectionManager", this::handleSoap);
        server.createContext("/upnp/event/AVTransport", this::handleEventSubscription);
        server.createContext("/upnp/event/RenderingControl", this::handleEventSubscription);
        server.createContext("/upnp/event/ConnectionManager", this::handleEventSubscription);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTP server listening on port " + config.httpPort());
        eventLog.add("HTTP server started on port " + config.httpPort());
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        String html = "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">" +
                "<title>Show2PC</title><style>body{font-family:system-ui;margin:32px;line-height:1.5}input{width:70%;padding:8px}button{padding:8px 12px}pre{background:#111;color:#eee;padding:16px;overflow:auto}</style></head>" +
                "<body><h1>Show2PC</h1><p>DLNA/UPnP 投屏接收服务正在运行。</p>" +
                "<form method=\"post\" action=\"/api/play\"><input name=\"url\" placeholder=\"输入 HLS/MP4 URL 手动测试播放\"><button>播放</button></form>" +
                "<h2>状态</h2><pre id=\"status\">loading...</pre>" +
                "<script>async function load(){const r=await fetch('/api/status');document.getElementById('status').textContent=await r.text()}load();setInterval(load,2000)</script>" +
                "</body></html>";
        send(exchange, 200, "text/html; charset=utf-8", html);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("Device: ").append(config.friendlyName()).append('\n');
        body.append("HTTP Port: ").append(config.httpPort()).append('\n');
        body.append("LAN URLs:\n");
        for (InetAddress address : interfaceSelector.activeIpv4Addresses()) {
            body.append("  http://").append(address.getHostAddress()).append(':').append(config.httpPort()).append("/\n");
        }
        body.append("Player: ").append(config.playerCommand()).append('\n');
        body.append("State: ").append(player.state()).append('\n');
        body.append("Current URI: ").append(player.currentUri()).append('\n');
        body.append("Volume: ").append(player.volume()).append('\n');
        body.append("Muted: ").append(player.muted()).append('\n');
        body.append("\nRecent events:\n");
        for (String event : eventLog.recent()) {
            body.append(event).append('\n');
        }
        send(exchange, 200, "text/plain; charset=utf-8", body.toString());
    }

    private void handleManualPlay(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }
        String body = readBody(exchange);
        String url = parseFormUrl(body);
        player.load(url, "");
        player.play();
        eventLog.add("Manual play " + url);
        exchange.getResponseHeaders().add("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private void handleDeviceDescription(HttpExchange exchange) throws IOException {
        eventLog.add("HTTP GET /device.xml from " + exchange.getRemoteAddress().getAddress().getHostAddress());
        String host = exchange.getRequestHeaders().getFirst("Host");
        String baseUrl = "http://" + (host == null ? "localhost:" + config.httpPort() : host);
        send(exchange, 200, "text/xml; charset=utf-8", deviceDescription.xml(baseUrl));
    }

    private void handleEventSubscription(HttpExchange exchange) throws IOException {
        String path = exchange.getHttpContext().getPath();
        String method = exchange.getRequestMethod();
        eventLog.add("HTTP " + method + " " + path + " from " + exchange.getRemoteAddress().getAddress().getHostAddress());
        if ("SUBSCRIBE".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("SID", "uuid:show2pc-event-subscription");
            exchange.getResponseHeaders().set("TIMEOUT", "Second-1800");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        if ("UNSUBSCRIBE".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        send(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
    }

    private void handleSoap(HttpExchange exchange) throws IOException {
        SoapActionHandler handler = handlers.get(exchange.getHttpContext().getPath());
        if (handler == null) {
            send(exchange, 404, "text/plain; charset=utf-8", "Not Found");
            return;
        }
        String requestBody = readBody(exchange);
        String action = SoapEnvelope.actionFromSoapAction(exchange.getRequestHeaders().getFirst("SOAPACTION"));
        if (action.isEmpty()) {
            action = SoapEnvelope.actionFromBody(requestBody);
        }
        eventLog.add("SOAP " + exchange.getHttpContext().getPath() + "#" + action);
        String response = handler.handle(action, requestBody);
        send(exchange, 200, "text/xml; charset=utf-8", response);
    }

    private void handleResource(HttpExchange exchange, String resource, String contentType) throws IOException {
        eventLog.add("HTTP GET /" + resource + " from " + exchange.getRemoteAddress().getAddress().getHostAddress());
        try (InputStream input = UpnpHttpServer.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                send(exchange, 404, "text/plain; charset=utf-8", "Not Found");
                return;
            }
            byte[] bytes = readAll(input);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }

    private void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
    }

    private byte[] readAll(InputStream input) throws IOException {
        return input.readAllBytes();
    }

    private String parseFormUrl(String body) {
        for (String part : body.split("&")) {
            int index = part.indexOf('=');
            if (index > 0 && part.substring(0, index).equals("url")) {
                return java.net.URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}

package show2pc.services.connection;

import show2pc.upnp.SoapActionHandler;
import show2pc.upnp.SoapEnvelope;
import show2pc.upnp.XmlUtil;
import show2pc.util.EventLog;

public class ConnectionManagerService implements SoapActionHandler {
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1";

    private static final String SINK_PROTOCOL_INFO = String.join(",",
            "http-get:*:video/mp4:*",
            "http-get:*:video/mpeg:*",
            "http-get:*:video/x-matroska:*",
            "http-get:*:application/vnd.apple.mpegurl:*",
            "http-get:*:application/x-mpegURL:*",
            "http-get:*:video/mp2t:*",
            "http-get:*:audio/mpeg:*",
            "http-get:*:audio/mp4:*",
            "http-get:*:image/jpeg:*"
    );

    private final EventLog eventLog;

    public ConnectionManagerService(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @Override
    public String serviceType() {
        return SERVICE_TYPE;
    }

    @Override
    public String handle(String action, String requestBody) {
        eventLog.add("ConnectionManager " + action);
        switch (action) {
            case "GetProtocolInfo":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<Source></Source><Sink>" + XmlUtil.escape(SINK_PROTOCOL_INFO) + "</Sink>");
            case "GetCurrentConnectionIDs":
                return SoapEnvelope.success(SERVICE_TYPE, action, "<ConnectionIDs>0</ConnectionIDs>");
            case "GetCurrentConnectionInfo":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<RcsID>0</RcsID><AVTransportID>0</AVTransportID>" +
                                "<ProtocolInfo></ProtocolInfo><PeerConnectionManager></PeerConnectionManager>" +
                                "<PeerConnectionID>-1</PeerConnectionID><Direction>Input</Direction><Status>OK</Status>");
            default:
                return SoapEnvelope.fault(401, "Invalid Action");
        }
    }
}

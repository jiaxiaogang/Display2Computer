package show2pc.services.rendering;

import show2pc.player.PlaybackController;
import show2pc.upnp.SoapActionHandler;
import show2pc.upnp.SoapEnvelope;
import show2pc.util.EventLog;

public class RenderingControlService implements SoapActionHandler {
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:RenderingControl:1";

    private final PlaybackController player;
    private final EventLog eventLog;

    public RenderingControlService(PlaybackController player, EventLog eventLog) {
        this.player = player;
        this.eventLog = eventLog;
    }

    @Override
    public String serviceType() {
        return SERVICE_TYPE;
    }

    @Override
    public String handle(String action, String requestBody) {
        eventLog.add("RenderingControl " + action);
        switch (action) {
            case "GetVolume":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<CurrentVolume>" + player.volume() + "</CurrentVolume>");
            case "SetVolume":
                player.setVolume(parseInt(SoapEnvelope.value(requestBody, "DesiredVolume"), player.volume()));
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            case "GetMute":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<CurrentMute>" + (player.muted() ? "1" : "0") + "</CurrentMute>");
            case "SetMute":
                player.setMute("1".equals(SoapEnvelope.value(requestBody, "DesiredMute")) || "true".equalsIgnoreCase(SoapEnvelope.value(requestBody, "DesiredMute")));
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            default:
                return SoapEnvelope.fault(401, "Invalid Action");
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

package show2pc.services.avtransport;

import show2pc.player.PlaybackController;
import show2pc.player.PlayerState;
import show2pc.upnp.SoapActionHandler;
import show2pc.upnp.SoapEnvelope;
import show2pc.upnp.XmlUtil;
import show2pc.util.EventLog;

import java.time.Duration;

public class AVTransportService implements SoapActionHandler {
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1";

    private final PlaybackController player;
    private final EventLog eventLog;
    private String metadata = "";

    public AVTransportService(PlaybackController player, EventLog eventLog) {
        this.player = player;
        this.eventLog = eventLog;
    }

    @Override
    public String serviceType() {
        return SERVICE_TYPE;
    }

    @Override
    public synchronized String handle(String action, String requestBody) {
        eventLog.add("AVTransport " + action);
        switch (action) {
            case "SetAVTransportURI":
                return setAvTransportUri(requestBody);
            case "Play":
                player.play();
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            case "Pause":
                player.pause();
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            case "Stop":
                player.stop();
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            case "Seek":
                player.seek(parseRelTime(SoapEnvelope.value(requestBody, "Target")));
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            case "Next":
            case "Previous":
            case "SetPlayMode":
                return SoapEnvelope.success(SERVICE_TYPE, action, "");
            case "GetDeviceCapabilities":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<PlayMedia>NETWORK</PlayMedia><RecMedia>NOT_IMPLEMENTED</RecMedia><RecQualityModes>NOT_IMPLEMENTED</RecQualityModes>");
            case "GetTransportInfo":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<CurrentTransportState>" + transportState() + "</CurrentTransportState>" +
                                "<CurrentTransportStatus>OK</CurrentTransportStatus>" +
                                "<CurrentSpeed>1</CurrentSpeed>");
            case "GetPositionInfo":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<Track>1</Track>" +
                                "<TrackDuration>" + formatDuration(player.duration()) + "</TrackDuration>" +
                                "<TrackMetaData>" + XmlUtil.escape(metadata) + "</TrackMetaData>" +
                                "<TrackURI>" + XmlUtil.escape(player.currentUri()) + "</TrackURI>" +
                                "<RelTime>" + formatDuration(player.position()) + "</RelTime>" +
                                "<AbsTime>" + formatDuration(player.position()) + "</AbsTime>" +
                                "<RelCount>2147483647</RelCount>" +
                                "<AbsCount>2147483647</AbsCount>");
            case "GetMediaInfo":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<NrTracks>1</NrTracks>" +
                                "<MediaDuration>" + formatDuration(player.duration()) + "</MediaDuration>" +
                                "<CurrentURI>" + XmlUtil.escape(player.currentUri()) + "</CurrentURI>" +
                                "<CurrentURIMetaData>" + XmlUtil.escape(metadata) + "</CurrentURIMetaData>" +
                                "<NextURI></NextURI><NextURIMetaData></NextURIMetaData>" +
                                "<PlayMedium>NETWORK</PlayMedium><RecordMedium>NOT_IMPLEMENTED</RecordMedium>" +
                                "<WriteStatus>NOT_IMPLEMENTED</WriteStatus>");
            case "GetTransportSettings":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<PlayMode>NORMAL</PlayMode><RecQualityMode>NOT_IMPLEMENTED</RecQualityMode>");
            case "GetCurrentTransportActions":
                return SoapEnvelope.success(SERVICE_TYPE, action,
                        "<Actions>Play,Pause,Stop,Seek,Next,Previous</Actions>");
            default:
                return SoapEnvelope.fault(401, "Invalid Action");
        }
    }

    private String setAvTransportUri(String requestBody) {
        String uri = SoapEnvelope.value(requestBody, "CurrentURI");
        metadata = SoapEnvelope.value(requestBody, "CurrentURIMetaData");
        player.load(uri, metadata);
        eventLog.add("Set URI " + abbreviate(uri));
        return SoapEnvelope.success(SERVICE_TYPE, "SetAVTransportURI", "");
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 300) {
            return value;
        }
        return value.substring(0, 300) + "...";
    }

    private String transportState() {
        PlayerState state = player.state();
        if (state == PlayerState.PLAYING) {
            return "PLAYING";
        }
        if (state == PlayerState.PAUSED) {
            return "PAUSED_PLAYBACK";
        }
        if (state == PlayerState.TRANSITIONING) {
            return "TRANSITIONING";
        }
        return "STOPPED";
    }

    private static Duration parseRelTime(String value) {
        if (value == null || value.isEmpty()) {
            return Duration.ZERO;
        }
        String[] parts = value.split(":");
        if (parts.length != 3) {
            return Duration.ZERO;
        }
        try {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2].split("\\.")[0]);
            return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        } catch (NumberFormatException e) {
            return Duration.ZERO;
        }
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration == null ? 0 : duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
}

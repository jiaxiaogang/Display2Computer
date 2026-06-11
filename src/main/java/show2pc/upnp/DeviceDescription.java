package show2pc.upnp;

import show2pc.config.AppConfig;
import show2pc.config.DeviceIdentity;

public class DeviceDescription {
    private final AppConfig config;
    private final DeviceIdentity identity;

    public DeviceDescription(AppConfig config, DeviceIdentity identity) {
        this.config = config;
        this.identity = identity;
    }

    public String xml(String baseUrl) {
        return "<?xml version=\"1.0\"?>" +
                "<root xmlns=\"urn:schemas-upnp-org:device-1-0\" xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">" +
                "<specVersion><major>1</major><minor>0</minor></specVersion>" +
                "<device>" +
                "<deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>" +
                "<friendlyName>" + XmlUtil.escape(config.friendlyName()) + "</friendlyName>" +
                "<manufacturer>" + XmlUtil.escape(config.manufacturer()) + "</manufacturer>" +
                "<manufacturerURL>http://localhost/</manufacturerURL>" +
                "<modelDescription>Cross-platform DLNA media renderer</modelDescription>" +
                "<modelName>" + XmlUtil.escape(config.modelName()) + "</modelName>" +
                "<modelNumber>0.1</modelNumber>" +
                "<modelURL>http://localhost/</modelURL>" +
                "<serialNumber>0000001</serialNumber>" +
                "<UDN>" + identity.udn() + "</UDN>" +
                "<dlna:X_DLNADOC>DMR-1.50</dlna:X_DLNADOC>" +
                "<serviceList>" +
                service("urn:schemas-upnp-org:service:AVTransport:1", "urn:upnp-org:serviceId:AVTransport", "/upnp/scpd/AVTransport.xml", "/upnp/control/AVTransport", "/upnp/event/AVTransport") +
                service("urn:schemas-upnp-org:service:RenderingControl:1", "urn:upnp-org:serviceId:RenderingControl", "/upnp/scpd/RenderingControl.xml", "/upnp/control/RenderingControl", "/upnp/event/RenderingControl") +
                service("urn:schemas-upnp-org:service:ConnectionManager:1", "urn:upnp-org:serviceId:ConnectionManager", "/upnp/scpd/ConnectionManager.xml", "/upnp/control/ConnectionManager", "/upnp/event/ConnectionManager") +
                "</serviceList>" +
                "<presentationURL>" + XmlUtil.escape(baseUrl) + "/</presentationURL>" +
                "</device></root>";
    }

    private String service(String serviceType, String serviceId, String scpdUrl, String controlUrl, String eventSubUrl) {
        return "<service>" +
                "<serviceType>" + serviceType + "</serviceType>" +
                "<serviceId>" + serviceId + "</serviceId>" +
                "<SCPDURL>" + scpdUrl + "</SCPDURL>" +
                "<controlURL>" + controlUrl + "</controlURL>" +
                "<eventSubURL>" + eventSubUrl + "</eventSubURL>" +
                "</service>";
    }
}

package show2pc.upnp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SoapEnvelope {
    private static final Pattern ACTION_PATTERN = Pattern.compile("<(?:(?:\\w+):)?(\\w+)[^>]*>");

    private SoapEnvelope() {
    }

    public static String success(String serviceType, String action, String body) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:" + action + "Response xmlns:u=\"" + serviceType + "\">" +
                body +
                "</u:" + action + "Response>" +
                "</s:Body></s:Envelope>";
    }

    public static String fault(int code, String description) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body><s:Fault>" +
                "<faultcode>s:Client</faultcode><faultstring>UPnPError</faultstring>" +
                "<detail><UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">" +
                "<errorCode>" + code + "</errorCode>" +
                "<errorDescription>" + XmlUtil.escape(description) + "</errorDescription>" +
                "</UPnPError></detail>" +
                "</s:Fault></s:Body></s:Envelope>";
    }

    public static String actionFromSoapAction(String soapAction) {
        if (soapAction == null) {
            return "";
        }
        String cleaned = soapAction.replace("\"", "").trim();
        int index = cleaned.lastIndexOf('#');
        return index >= 0 ? cleaned.substring(index + 1) : cleaned;
    }

    public static String value(String xml, String name) {
        Pattern pattern = Pattern.compile("<[^:/<>]*:?" + Pattern.quote(name) + "[^>]*>(.*?)</[^:/<>]*:?" + Pattern.quote(name) + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (!matcher.find()) {
            return "";
        }
        return unescape(matcher.group(1).trim());
    }

    public static String actionFromBody(String xml) {
        Matcher matcher = ACTION_PATTERN.matcher(xml);
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (!tag.equals("Envelope") && !tag.equals("Body")) {
                return tag;
            }
        }
        return "";
    }

    private static String unescape(String value) {
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}

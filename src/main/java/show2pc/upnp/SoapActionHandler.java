package show2pc.upnp;

public interface SoapActionHandler {
    String serviceType();

    String handle(String action, String requestBody);
}

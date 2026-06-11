package show2pc.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DeviceIdentity {
    private final UUID uuid;

    private DeviceIdentity(UUID uuid) {
        this.uuid = uuid;
    }

    public static DeviceIdentity loadOrCreate(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Path uuidFile = dataDirectory.resolve("device.uuid");
        if (Files.exists(uuidFile)) {
            String value = new String(Files.readAllBytes(uuidFile), StandardCharsets.UTF_8).trim();
            return new DeviceIdentity(UUID.fromString(value));
        }

        UUID uuid = UUID.randomUUID();
        Files.write(uuidFile, uuid.toString().getBytes(StandardCharsets.UTF_8));
        return new DeviceIdentity(uuid);
    }

    public UUID uuid() {
        return uuid;
    }

    public String udn() {
        return "uuid:" + uuid;
    }
}

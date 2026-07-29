package imbacrian.retroskinloader.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import imbacrian.retroskinloader.skin.SkinProfile;

/** Binary payload for the RSL:Profile custom-packet channel. */
public final class RslProfilePayload {
    public static final String CHANNEL = "RSL:Profile";
    public static final int VERSION = 1;
    private static final int SET = 0;
    private static final int CLEAR = 1;
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MAX_URL_LENGTH = 2048;

    private RslProfilePayload() {
    }

    public static ProfileUpdate decode(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Empty RSL profile payload");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            int version = input.readUnsignedByte();
            if (version != VERSION) {
                throw new IOException("Unsupported RSL profile protocol version: " + version);
            }
            int operation = input.readUnsignedByte();
            int entityId = input.readInt();
            String username = readBoundedUtf(input, MAX_USERNAME_LENGTH, "username");
            if (operation == CLEAR) {
                requireExhausted(input);
                return ProfileUpdate.clear(entityId, username);
            }
            if (operation != SET) {
                throw new IOException("Unknown RSL profile operation: " + operation);
            }
            String skinUrl = readOptionalUrl(input);
            String capeUrl = readOptionalUrl(input);
            String model = input.readUnsignedByte() == 1 ? "slim" : "default";
            requireExhausted(input);
            SkinProfile profile = new SkinProfile(skinUrl, capeUrl, model);
            if (profile.isEmpty()) {
                throw new IOException("RSL profile update contains no texture URLs");
            }
            return ProfileUpdate.set(entityId, username, profile);
        }
    }

    public static byte[] encodeSet(int entityId, String username, SkinProfile profile) {
        if (profile == null || profile.isEmpty()) {
            throw new IllegalArgumentException("Profile must contain at least one texture URL");
        }
        return encode(entityId, username, SET, profile);
    }

    public static byte[] encodeClear(int entityId, String username) {
        return encode(entityId, username, CLEAR, null);
    }

    private static byte[] encode(int entityId, String username, int operation, SkinProfile profile) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(VERSION);
            output.writeByte(operation);
            output.writeInt(entityId);
            writeBoundedUtf(output, username, MAX_USERNAME_LENGTH, "username");
            if (operation == SET) {
                writeOptionalUrl(output, profile.getSkinUrl());
                writeOptionalUrl(output, profile.getCapeUrl());
                output.writeByte("slim".equals(profile.getModel()) ? 1 : 0);
            }
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode RSL profile payload", exception);
        }
    }

    private static String readOptionalUrl(DataInputStream input) throws IOException {
        String url = readBoundedUtf(input, MAX_URL_LENGTH, "texture URL");
        if (url.isEmpty()) {
            return null;
        }
        if (!isHttpUrl(url)) {
            throw new IOException("RSL profile texture URL must use HTTP(S)");
        }
        return url;
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static String readBoundedUtf(DataInputStream input, int maximumLength, String field) throws IOException {
        String value = input.readUTF();
        if (value.length() > maximumLength) {
            throw new IOException("RSL profile " + field + " is too long");
        }
        return value;
    }

    private static void writeBoundedUtf(DataOutputStream output, String value, int maximumLength, String field) throws IOException {
        String safeValue = value == null ? "" : value;
        if (safeValue.isEmpty() || safeValue.length() > maximumLength) {
            throw new IllegalArgumentException("RSL profile " + field + " is invalid");
        }
        output.writeUTF(safeValue);
    }

    private static void writeOptionalUrl(DataOutputStream output, String value) throws IOException {
        String safeValue = value == null ? "" : value;
        if (!safeValue.isEmpty() && (!isHttpUrl(safeValue) || safeValue.length() > MAX_URL_LENGTH)) {
            throw new IllegalArgumentException("RSL profile texture URL is invalid");
        }
        output.writeUTF(safeValue);
    }

    private static void requireExhausted(DataInputStream input) throws IOException {
        if (input.available() != 0) {
            throw new IOException("Unexpected trailing data in RSL profile payload");
        }
    }

    public static final class ProfileUpdate {
        private final int entityId;
        private final String username;
        private final SkinProfile profile;

        private ProfileUpdate(int entityId, String username, SkinProfile profile) {
            this.entityId = entityId;
            this.username = username;
            this.profile = profile;
        }

        private static ProfileUpdate set(int entityId, String username, SkinProfile profile) {
            return new ProfileUpdate(entityId, username, profile);
        }

        private static ProfileUpdate clear(int entityId, String username) {
            return new ProfileUpdate(entityId, username, null);
        }

        public int getEntityId() {
            return this.entityId;
        }

        public String getUsername() {
            return this.username;
        }

        public SkinProfile getProfile() {
            return this.profile;
        }

        public boolean isClear() {
            return this.profile == null;
        }
    }
}

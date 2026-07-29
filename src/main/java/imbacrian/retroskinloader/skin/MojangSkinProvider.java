package imbacrian.retroskinloader.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import imbacrian.retroskinloader.RetroSkinLoader;

import java.util.UUID;

public final class MojangSkinProvider implements SkinProvider {

    @Override
    public SkinProfile load(String username) {
        try {
            // First, get the UUID from the username
            String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
            RetroSkinLoader.LOGGER.info("Fetching UUID from Mojang for user: {}", username);
            String uuidResponse = HttpSkinClient.get(uuidUrl);
            if (uuidResponse == null) {
                RetroSkinLoader.LOGGER.warn("Mojang UUID lookup failed for user: {}", username);
                return null;
            }

            JsonObject uuidJson = JsonParser.parseString(uuidResponse).getAsJsonObject();
            String uuid = uuidJson.get("id").getAsString();

            // Format UUID with dashes
            String formattedUuid = uuid.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
            );

            // Now get the profile with textures
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + formattedUuid;
            RetroSkinLoader.LOGGER.info("Fetching profile from Mojang for UUID: {}", formattedUuid);
            String profileResponse = HttpSkinClient.get(profileUrl);
            if (profileResponse == null) {
                RetroSkinLoader.LOGGER.warn("Mojang profile lookup failed for UUID: {}", formattedUuid);
                return null;
            }

            JsonObject profileJson = JsonParser.parseString(profileResponse).getAsJsonObject();

            // Parse textures from the properties
            String skinUrl = null;
            String capeUrl = null;
            String model = "default";

            if (profileJson.has("properties")) {
                var properties = profileJson.getAsJsonArray("properties");
                for (var prop : properties) {
                    JsonObject propObj = prop.getAsJsonObject();
                    if ("textures".equals(propObj.get("name").getAsString())) {
                        String value = propObj.get("value").getAsString();
                        String decoded = new String(java.util.Base64.getDecoder().decode(value));
                        JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject();

                        if (textures.has("textures")) {
                            JsonObject texturesObj = textures.getAsJsonObject("textures");

                            if (texturesObj.has("SKIN")) {
                                JsonObject skin = texturesObj.getAsJsonObject("SKIN");
                                skinUrl = skin.get("url").getAsString();
                                if (skin.has("metadata") && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString())) {
                                    model = "slim";
                                }
                                RetroSkinLoader.LOGGER.info("Found Mojang skin URL: {}", skinUrl);
                            }

                            if (texturesObj.has("CAPE")) {
                                JsonObject cape = texturesObj.getAsJsonObject("CAPE");
                                capeUrl = cape.get("url").getAsString();
                                RetroSkinLoader.LOGGER.info("Found Mojang cape URL: {}", capeUrl);
                            }
                        }
                    }
                }
            }

            SkinProfile profile = new SkinProfile(skinUrl, capeUrl, model);
            return profile.isEmpty() ? null : profile;
        } catch (Exception exception) {
            RetroSkinLoader.LOGGER.error("Failed to load skin data from Mojang", exception);
            return null;
        }
    }
}

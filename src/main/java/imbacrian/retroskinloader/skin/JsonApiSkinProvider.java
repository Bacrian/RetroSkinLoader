package imbacrian.retroskinloader.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import imbacrian.retroskinloader.RetroSkinLoader;

public final class JsonApiSkinProvider implements SkinProvider {
    private final String name;
    private final String apiUrl;

    public JsonApiSkinProvider(String name, String apiUrl) {
        this.name = name;
        this.apiUrl = apiUrl;
    }

    @Override
    public SkinProfile load(String username) {
        if (this.apiUrl == null || this.apiUrl.trim().isEmpty()) {
            return null;
        }
        try {
            String url = this.apiUrl.replace("{username}", username);
            RetroSkinLoader.LOGGER.info("Fetching skin data from: {}", url);
            String response = HttpSkinClient.get(url);
            if (response == null) {
                return null;
            }
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String skinUrl = null;
            String model = "default";
            if (json.has("skins")) {
                JsonObject skins = json.getAsJsonObject("skins");
                if (skins.has("default")) {
                    skinUrl = skins.get("default").getAsString();
                }
                if (skins.has("slim")) {
                    skinUrl = skins.get("slim").getAsString();
                    model = "slim";
                }
            }
            String capeUrl = json.has("cape") ? json.get("cape").getAsString() : null;
            SkinProfile profile = new SkinProfile(skinUrl, capeUrl, model);
            return profile.isEmpty() ? null : profile;
        } catch (Exception exception) {
            RetroSkinLoader.LOGGER.error("Failed to load from skin site: " + this.name, exception);
            return null;
        }
    }
}

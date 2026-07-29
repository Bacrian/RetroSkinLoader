package imbacrian.retroskinloader.skin;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import imbacrian.retroskinloader.RetroSkinLoader;

public final class ElyBySkinProvider implements SkinProvider {
    private final String root;

    public ElyBySkinProvider(String root) {
        String configuredRoot = root == null || root.trim().isEmpty() ? "https://skinsystem.ely.by" : root.trim();
        this.root = configuredRoot.endsWith("/") ? configuredRoot.substring(0, configuredRoot.length() - 1) : configuredRoot;
    }

    @Override
    public SkinProfile load(String username) {
        try {
            String url = this.root + "/textures/" + URLEncoder.encode(username, StandardCharsets.UTF_8);
            RetroSkinLoader.LOGGER.info("Fetching Ely.by textures from: {}", url);
            String response = HttpSkinClient.get(url);
            if (response == null) {
                RetroSkinLoader.LOGGER.warn("Ely.by returned null response for user: {}", username);
                return null;
            }
            RetroSkinLoader.LOGGER.info("Ely.by response: {}", response);
            JsonObject textures = JsonParser.parseString(response).getAsJsonObject();
            String skinUrl = null;
            String capeUrl = null;
            String model = "default";
            if (textures.has("SKIN")) {
                JsonObject skin = textures.getAsJsonObject("SKIN");
                skinUrl = skin.get("url").getAsString();
                // Convert HTTP to HTTPS for ElyBy URLs to avoid redirect issues
                if (skinUrl.startsWith("http://ely.by/")) {
                    skinUrl = skinUrl.replace("http://", "https://");
                    RetroSkinLoader.LOGGER.info("Converted ElyBy URL to HTTPS: {}", skinUrl);
                }
                RetroSkinLoader.LOGGER.info("Found skin URL: {}", skinUrl);
                if (skin.has("metadata") && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString())) {
                    model = "slim";
                }
            }
            if (textures.has("CAPE")) {
                capeUrl = textures.getAsJsonObject("CAPE").get("url").getAsString();
                // Convert HTTP to HTTPS for ElyBy URLs to avoid redirect issues
                if (capeUrl.startsWith("http://ely.by/")) {
                    capeUrl = capeUrl.replace("http://", "https://");
                    RetroSkinLoader.LOGGER.info("Converted ElyBy cape URL to HTTPS: {}", capeUrl);
                }
                RetroSkinLoader.LOGGER.info("Found cape URL: {}", capeUrl);
            }
            SkinProfile profile = new SkinProfile(skinUrl, capeUrl, model);
            RetroSkinLoader.LOGGER.info("Created profile: skin={}, cape={}, model={}", skinUrl, capeUrl, model);
            return profile.isEmpty() ? null : profile;
        } catch (Exception exception) {
            RetroSkinLoader.LOGGER.error("Failed to load skin data from Ely.by", exception);
            return null;
        }
    }
}

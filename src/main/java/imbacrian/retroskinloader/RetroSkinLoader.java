package imbacrian.retroskinloader;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import imbacrian.retroskinloader.command.CommandRsl;
import imbacrian.retroskinloader.skin.*;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import turniplabs.halplibe.HalpLibe;
import turniplabs.halplibe.event.defs.CommonEvents;
import turniplabs.halplibe.util.dependency.Key;

public class RetroSkinLoader implements ModInitializer {
	public static final String MOD_ID = HalpLibe.registerMod("retroskinloader", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final File DATA_DIR = new File(".", "RetroSkinLoader");
	private static final File CONFIG_FILE = new File(DATA_DIR, "RSLconfig.json");
	private static final Gson GSON = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();
	private static SkinConfig config;

	@Override
	public void onInitialize() {
		CommandManager.registerCommand(new CommandRsl());
		SkinIdRegistry.load();
		PlayerSkinChoices.load();
		CommonEvents.BEFORE_GAME_START.listen(Key.of(MOD_ID), this::beforeGameStart);
		CommonEvents.AFTER_GAME_START.listen(Key.of(MOD_ID), this::afterGameStart);
		LOGGER.info("RetroSkinLoader initialized.");
	}

	public void beforeGameStart() {
		loadConfig();
	}

	public void afterGameStart() {

	}

	/**
	 * Returns a Mojang session-profile JSON document so BTA can keep its native
	 * parsing of skins, capes and slim models. A null result lets BTA fall back
	 * to its normal Mojang request.
	 */
	public static String loadSessionProfile(String username) {
		LOGGER.info("Loading custom skin profile for player: {}", username);

		// Check player choices first (highest priority)
		String skinId = PlayerSkinChoices.get(username);
		if (skinId != null) {
			SkinProfile chosenProfile = SkinIdRegistry.resolve(skinId);
			if (chosenProfile != null && !chosenProfile.isEmpty()) {
				LOGGER.info("Using player-chosen skin '{}' for player: {}", skinId, username);
				return toSessionProfile(username, chosenProfile);
			}
		}

		SkinProfile serverProfile = new ServerProfileProvider().load(username);
		if (serverProfile != null) {
			LOGGER.info("Using server-provided skin profile for player: {}", username);
			return toSessionProfile(username, serverProfile);
		}
		if (config == null || config.loadlist == null || config.loadlist.isEmpty()) {
			return null;
		}

		try {
			SkinProfile resolvedProfile = null;
			for (SkinSiteProfile site : config.loadlist) {
				SkinProvider provider = createProvider(site);
				SkinProfile profile = provider == null ? null : provider.load(username);
				if (profile != null && !profile.isEmpty()) {
					resolvedProfile = resolvedProfile == null ? profile : resolvedProfile.merge(profile);
					LOGGER.info("Loaded custom skin profile from {}", site.name);
					if (!config.forceLoadAllTextures) {
						break;
					}
				}
			}
			return resolvedProfile == null ? null : toSessionProfile(username, resolvedProfile);
		} catch (Exception e) {
			LOGGER.error("Failed to load custom skin profile for player: " + username, e);
			return null;
		}
	}

	public static boolean applyServerProfile(Player player) {
		SkinProfile profile = new ServerProfileProvider().load(player.username);
		if (profile == null) {
			return false;
		}
		player.skinURL = profile.getSkinUrl();
		player.capeURL = profile.getCapeUrl();
		player.slimModel = "slim".equals(profile.getModel());
		return true;
	}

	private static SkinProvider createProvider(SkinSiteProfile site) {
		if (site == null) {
			return null;
		}
		String type = site.type == null ? "json_api" : site.type.trim().toLowerCase(java.util.Locale.ROOT);
		if ("elyby".equals(type) || "ely_by".equals(type)) {
			return new ElyBySkinProvider(site.root);
		}
		if ("json_api".equals(type) || "custom".equals(type) || "customskinapi".equals(type)) {
			return new JsonApiSkinProvider(site.name, site.apiUrl);
		}
		LOGGER.warn("Unknown skin provider type '{}' for '{}'; skipping it", site.type, site.name);
		return null;
	}

	private static String toSessionProfile(String username, SkinProfile profile) {
		JsonObject textures = new JsonObject();
		if (profile.getSkinUrl() != null) {
			JsonObject skin = new JsonObject();
			skin.addProperty("url", profile.getSkinUrl());
			if ("slim".equals(profile.getModel())) {
				JsonObject metadata = new JsonObject();
				metadata.addProperty("model", "slim");
				skin.add("metadata", metadata);
			}
			textures.add("SKIN", skin);
		}
		if (profile.getCapeUrl() != null) {
			JsonObject cape = new JsonObject();
			cape.addProperty("url", profile.getCapeUrl());
			textures.add("CAPE", cape);
		}

		JsonObject texturesWrapper = new JsonObject();
		texturesWrapper.add("textures", textures);

		JsonObject textureProperty = new JsonObject();
		textureProperty.addProperty("name", "textures");
		textureProperty.addProperty("value", Base64.getEncoder().encodeToString(
			GSON.toJson(texturesWrapper).getBytes(StandardCharsets.UTF_8)
		));
		JsonObject sessionProfile = new JsonObject();
		sessionProfile.addProperty("id", UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""));
		sessionProfile.addProperty("name", username);
		JsonArray properties = new JsonArray();
		properties.add(textureProperty);
		sessionProfile.add("properties", properties);
		return GSON.toJson(sessionProfile);
	}

	private static void loadConfig() {
		try {
			if (!DATA_DIR.exists()) {
				DATA_DIR.mkdirs();
			}

			if (!CONFIG_FILE.exists()) {
				LOGGER.info("Config file not found, creating default config");
				config = new SkinConfig();
				saveConfig();
				return;
			}

			String json = new String(Files.readAllBytes(CONFIG_FILE.toPath()));
			config = GSON.fromJson(json, SkinConfig.class);
			LOGGER.info("Loaded config with {} skin sites", config.loadlist.size());
		} catch (Exception e) {
			LOGGER.error("Failed to load config", e);
			config = new SkinConfig();
		}
	}

	private static void saveConfig() {
		try {
			String json = GSON.toJson(config);
			Files.write(CONFIG_FILE.toPath(), json.getBytes());
			LOGGER.info("Saved config file");
		} catch (Exception e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	private static class SkinConfig {
		public java.util.List<SkinSiteProfile> loadlist = new java.util.ArrayList<>();
		public boolean forceLoadAllTextures = false;

		public SkinConfig() {
			// Ely.by
			SkinSiteProfile elyby = new SkinSiteProfile();
			elyby.name = "ElyByAPI";
			elyby.type = "elyby";
			loadlist.add(elyby);
			// -- Example
			//SkinSiteProfile example = new SkinSiteProfile();
			//example.name = "Example";
			//example.apiUrl = "https://example.com/";
			//loadlist.add(example);
		}
	}

	private static class SkinSiteProfile {
		public String name;
		public String type = "json_api";
		public String apiUrl;
		public String root;
	}
}

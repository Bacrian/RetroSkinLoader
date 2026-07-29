package imbacrian.retroskinloader.skin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class PlayerSkinChoices {
	private static final File FILE = new File(".", "RetroSkinLoader/player_choices.json");
	private static final Gson GSON = new Gson();
	private static Map<String, String> choices = new LinkedHashMap<>();

	private PlayerSkinChoices() {
	}

	public static synchronized void load() {
		try {
			if (!FILE.exists()) {
				return;
			}
			String json = new String(Files.readAllBytes(FILE.toPath()), StandardCharsets.UTF_8);
			choices = GSON.fromJson(json, new TypeToken<LinkedHashMap<String, String>>() {}.getType());
		} catch (Exception exception) {
			choices = new LinkedHashMap<>();
		}
	}

	public static synchronized void set(String username, String skinId) {
		choices.put(key(username), skinId);
		save();
	}

	public static synchronized void clear(String username) {
		choices.remove(key(username));
		save();
	}

	public static synchronized String get(String username) {
		return choices.get(key(username));
	}

	private static void save() {
		try {
			FILE.getParentFile().mkdirs();
			Files.write(FILE.toPath(), GSON.toJson(choices).getBytes(StandardCharsets.UTF_8));
		} catch (Exception ignored) {
		}
	}

	private static String key(String username) {
		return username.toLowerCase(Locale.ROOT);
	}
}

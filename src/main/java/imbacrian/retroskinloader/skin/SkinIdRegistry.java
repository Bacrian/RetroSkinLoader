package imbacrian.retroskinloader.skin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public final class SkinIdRegistry {
	private static final File FILE = new File(".", "RetroSkinLoader/skins_list.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Map<String, SkinProfile> entries = new LinkedHashMap<>();

	private SkinIdRegistry() {
	}

	public static synchronized void load() {
		try {
			if (!FILE.exists()) {
				FILE.getParentFile().mkdirs();
				Map<String, SkinProfile> example = new LinkedHashMap<>();
				example.put("steve_classic", new SkinProfile("https://example.com/steve.png", null, "default"));
				Files.write(FILE.toPath(), GSON.toJson(example).getBytes(StandardCharsets.UTF_8));
				entries = example;
				return;
			}
			String json = new String(Files.readAllBytes(FILE.toPath()), StandardCharsets.UTF_8);
			entries = GSON.fromJson(json, new TypeToken<LinkedHashMap<String, SkinProfile>>() {}.getType());
		} catch (Exception exception) {
			entries = new LinkedHashMap<>();
		}
	}

	public static synchronized void put(String id, SkinProfile profile) {
		entries.put(id, profile);
		save();
	}

	public static synchronized boolean remove(String id) {
		boolean removed = entries.remove(id) != null;
		if (removed) {
			save();
		}
		return removed;
	}

	private static void save() {
		try {
			FILE.getParentFile().mkdirs();
			Files.write(FILE.toPath(), GSON.toJson(entries).getBytes(StandardCharsets.UTF_8));
		} catch (Exception ignored) {
		}
	}

	public static synchronized SkinProfile resolve(String id) {
		return entries.get(id);
	}

	public static synchronized Set<String> availableIds() {
		return entries.keySet();
	}
}

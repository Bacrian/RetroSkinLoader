package imbacrian.retroskinloader.skin;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reserved for the RSL client-server protocol. Server supplied profiles take
 * precedence over public providers, but nothing is stored until a future
 * packet handler explicitly updates this cache.
 */
public final class ServerProfileProvider implements SkinProvider {
    private static final Map<String, SkinProfile> PROFILES = new ConcurrentHashMap<>();

    @Override
    public SkinProfile load(String username) {
        return PROFILES.get(key(username));
    }

    public static void update(String username, SkinProfile profile) {
        if (profile == null || profile.isEmpty()) {
            PROFILES.remove(key(username));
            return;
        }
        PROFILES.put(key(username), profile);
    }

    public static void clear(String username) {
        PROFILES.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }
}

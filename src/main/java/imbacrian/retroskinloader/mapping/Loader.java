package imbacrian.retroskinloader.mapping;

import java.io.IOException;
import java.io.InputStream;

public final class Loader {
    private static final String MAPPING_RESOURCE = "/assets/retroskinloader/mapping.xml";

    private Loader() {
    }

    public static Mappings load(ClassLoader classLoader) {
        InputStream resourceStream = classLoader.getResourceAsStream(MAPPING_RESOURCE.substring(1));
        if (resourceStream == null) {
            resourceStream = Loader.class.getResourceAsStream(MAPPING_RESOURCE);
        }
        if (resourceStream == null) {
            throw new IllegalStateException("Could not find bundled mapping resource " + MAPPING_RESOURCE);
        }

        try (InputStream inputStream = resourceStream) {
            return new Parser().parse(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse bundled mapping resource " + MAPPING_RESOURCE, exception);
        }
    }
}

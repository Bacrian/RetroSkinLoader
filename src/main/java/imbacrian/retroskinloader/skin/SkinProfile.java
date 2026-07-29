package imbacrian.retroskinloader.skin;

public final class SkinProfile {
    private final String skinUrl;
    private final String capeUrl;
    private final String model;

    public SkinProfile(String skinUrl, String capeUrl, String model) {
        this.skinUrl = emptyToNull(skinUrl);
        this.capeUrl = emptyToNull(capeUrl);
        this.model = "slim".equals(model) ? "slim" : "default";
    }

    public String getSkinUrl() {
        return this.skinUrl;
    }

    public String getCapeUrl() {
        return this.capeUrl;
    }

    public String getModel() {
        return this.model;
    }

    public boolean isEmpty() {
        return this.skinUrl == null && this.capeUrl == null;
    }

    public SkinProfile merge(SkinProfile override) {
        if (override == null || override.isEmpty()) {
            return this;
        }
        return new SkinProfile(
            override.skinUrl != null ? override.skinUrl : this.skinUrl,
            override.capeUrl != null ? override.capeUrl : this.capeUrl,
            override.skinUrl != null ? override.model : this.model
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}

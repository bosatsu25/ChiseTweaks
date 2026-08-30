package dev.chise.chisetweaks.integration.masa;

public enum MasaJapaneseUiMode {
    AUTO(0, "Auto"),
    ENABLED(1, "Enabled"),
    DISABLED(2, "Disabled");

    private final int id;
    private final String label;

    MasaJapaneseUiMode(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int id() { return id; }
    public String label() { return label; }

    public static MasaJapaneseUiMode fromId(int id) {
        for (MasaJapaneseUiMode value : values()) {
            if (value.id == id) return value;
        }
        return AUTO;
    }
}

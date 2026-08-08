package dev.chise.chisetweaks.core.definition;

/** User-facing groups used by the configuration and help screens. */
public enum FeatureArea {
    BUILDING("Building", "建築支援", "Small, bounded helpers for deliberate block placement"),
    RENDERING("Rendering", "視認改善", "Visual inspection for large builds and technical facilities");

    private final String displayName;
    private final String japaneseLabel;
    private final String purpose;

    FeatureArea(String displayName, String japaneseLabel, String purpose) {
        this.displayName = displayName;
        this.japaneseLabel = japaneseLabel;
        this.purpose = purpose;
    }

    public String displayName() { return displayName; }
    public String japaneseLabel() { return japaneseLabel; }
    public String purpose() { return purpose; }
}

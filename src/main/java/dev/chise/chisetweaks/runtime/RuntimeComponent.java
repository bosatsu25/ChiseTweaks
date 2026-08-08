package dev.chise.chisetweaks.runtime;

/** Internal runtime service. Unlike a Feature, it does not own user settings. */
public interface RuntimeComponent {
    String getId();
    void init();
    boolean isActive();
}

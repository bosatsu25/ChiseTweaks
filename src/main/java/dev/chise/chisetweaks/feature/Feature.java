package dev.chise.chisetweaks.feature;

/**
 * Base contract for a ChiseTweaks feature.
 *
 * <p>Features that need client-tick work implement {@link TickingFeature}.
 * Event-driven features remain outside the tick dispatcher, which keeps the
 * disabled and idle cost close to zero.</p>
 */
public interface Feature {
    /** Stable identifier used by diagnostics and configuration. */
    String getId();

    /** User-facing feature name. */
    String getName();

    /** Registers callbacks and performs one-time initialization. */
    default void init() {
    }

    /** Returns whether this feature currently participates in runtime work. */
    boolean isEnabled();

    /** Enables or disables the feature. */
    void setEnabled(boolean enabled);
}

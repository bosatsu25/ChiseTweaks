package dev.chise.chisetweaks.feature;

import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;

/** User-facing feature state that must be restored or cleared across sessions. */
public interface SessionAwareFeature extends SessionAwareRuntimeComponent {
}

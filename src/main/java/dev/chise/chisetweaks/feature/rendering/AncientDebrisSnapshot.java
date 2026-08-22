package dev.chise.chisetweaks.feature.rendering;

/** 古代の残骸アナライザー向けの意味名だけを残し、snapshot実装は共通化する。 */
final class AncientDebrisSnapshot extends ThroughWallPositionSnapshot {
    AncientDebrisSnapshot(int capacity) {
        super(capacity);
    }
}

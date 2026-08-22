package dev.chise.chisetweaks.feature.rendering;

/** 溶岩源アナライザー向けの意味名だけを残し、snapshot実装は共通化する。 */
final class LavaSourceSnapshot extends ThroughWallPositionSnapshot {
    LavaSourceSnapshot(int capacity) {
        super(capacity);
    }
}

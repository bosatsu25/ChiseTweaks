package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SyncmaticaRemoveGuardPolicyTest {
    @Test
    void disabledAlwaysWins() {
        var decision = SyncmaticaRemoveGuardPolicy.decide(true, true, true);
        assertEquals(SyncmaticaRemoveGuardPolicy.Decision.DENY_DISABLED, decision);
        assertFalse(decision.allowed());
    }

    @Test
    void shiftRequirementOnlyBlocksWhenShiftIsMissing() {
        assertEquals(
                SyncmaticaRemoveGuardPolicy.Decision.DENY_SHIFT_REQUIRED,
                SyncmaticaRemoveGuardPolicy.decide(false, true, false));
        assertTrue(SyncmaticaRemoveGuardPolicy.decide(false, true, true).allowed());
        assertTrue(SyncmaticaRemoveGuardPolicy.decide(false, false, false).allowed());
    }
}

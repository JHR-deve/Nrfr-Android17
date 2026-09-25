package com.github.nrfr.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class PermissionHandshakeTest {
    @Test public void grantsOnlyOnceForMatchingTokenAndAppUid() {
        AtomicInteger grantedUid = new AtomicInteger(-1);
        PermissionHandshake handshake = new PermissionHandshake("one-time-token",
                grantedUid::set);
        handshake.acceptLine(PermissionHandshake.STATUS_PREFIX + "wrong-token:11000");
        assertEquals(-1, grantedUid.get());
        handshake.acceptLine(PermissionHandshake.STATUS_PREFIX + "one-time-token:11000");
        handshake.acceptLine(PermissionHandshake.STATUS_PREFIX + "one-time-token:11001");
        handshake.throwIfFailed();
        assertEquals(11000, grantedUid.get());
    }

    @Test public void rejectsNonApplicationUid() {
        PermissionHandshake handshake = new PermissionHandshake("one-time-token",
                uid -> fail("Must not delegate to " + uid));
        handshake.acceptLine(PermissionHandshake.STATUS_PREFIX + "one-time-token:2000");
        try {
            handshake.throwIfFailed();
            fail("Expected a rejected UID");
        } catch (IllegalStateException expected) { }
    }

    @Test public void olderAndroidRejectsAnExistingDelegate() {
        PermissionHandshake.ensureLegacyDelegationAvailable(34,
                Collections::emptyList);
        try {
            PermissionHandshake.ensureLegacyDelegationAvailable(34,
                    () -> Collections.singletonList("android.permission.READ_PHONE_STATE"));
            fail("Expected a rejected existing delegate");
        } catch (IllegalStateException expected) { }
    }
}

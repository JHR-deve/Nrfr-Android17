package com.github.nrfr.compat;

import android.content.Context;

/** Narrow Shizuku UserService: no SIM identity, MCC/MNC, UICC or persistent writes. */
public final class CountryOverrideUserService extends ICountryOverrideService.Stub {
    public CountryOverrideUserService() {}

    public CountryOverrideUserService(Context context) {}

    @Override
    public String applyCountryIso(int subId, String iso, String expectedOperatorNumeric) {
        return execute(() -> CarrierConfigBridge.applyTransient(subId, iso,
                expectedOperatorNumeric));
    }

    @Override
    public String restoreCountryIso(int subId, String iso, String expectedOperatorNumeric) {
        return execute(() -> CarrierConfigBridge.clearTransient(subId, iso,
                expectedOperatorNumeric));
    }

    public void destroy() {
        System.exit(0);
    }

    private interface Operation { String run() throws Exception; }

    private static String execute(Operation operation) {
        try {
            return operation.run();
        } catch (Throwable failure) {
            return "ERROR: " + failure.getClass().getSimpleName() + ": "
                    + String.valueOf(failure.getMessage());
        }
    }
}

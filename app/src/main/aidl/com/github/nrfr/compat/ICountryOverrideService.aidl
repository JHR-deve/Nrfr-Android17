package com.github.nrfr.compat;

/** Shell-UID service used only for the carrier-country instrumentation command. */
interface ICountryOverrideService {
    String applyCountryIso(int subId, String iso, String expectedOperatorNumeric);
    String restoreCountryIso(int subId, String iso, String expectedOperatorNumeric);
}

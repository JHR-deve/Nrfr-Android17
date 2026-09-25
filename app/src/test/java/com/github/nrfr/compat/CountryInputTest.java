package com.github.nrfr.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class CountryInputTest {
    @Test public void acceptsOnlyTwoAsciiLetters() {
        assertEquals("jp", CountryOverrideCoordinator.requireIso(" JP "));
        for (String candidate : new String[] {"", "J", "JPN", "J1", "中日", "../../jp"}) {
            try {
                CountryOverrideCoordinator.requireIso(candidate);
                fail("Accepted invalid ISO: " + candidate);
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void operatorNumericMustBeFiveOrSixDigits() {
        assertEquals("46015", CountryOverrideCoordinator.requireNumeric("46015"));
        for (String candidate : new String[] {"", "460", "46015x", "4601500"}) {
            try {
                CountryOverrideCoordinator.requireNumeric(candidate);
                fail("Accepted invalid operator numeric: " + candidate);
            } catch (IllegalStateException expected) { }
        }
    }
}

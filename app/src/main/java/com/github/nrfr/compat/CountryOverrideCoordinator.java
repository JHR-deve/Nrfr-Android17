package com.github.nrfr.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.github.nrfr.BuildConfig;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rikka.shizuku.Shizuku;

/** Serial, country-only operations with per-subscription snapshots and verified restoration. */
public final class CountryOverrideCoordinator {
    private static final String PREFS = "country_restore_v1";
    private static final long BIND_TIMEOUT_SECONDS = 10L;
    private static final Object OPERATION_LOCK = new Object();

    private CountryOverrideCoordinator() {}

    public static boolean hasSnapshot(Context context, int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.contains("iso_" + subId) && prefs.contains("numeric_" + subId);
    }

    /** The value the Restore button will write, or null when it cannot be determined safely. */
    public static String restoreTarget(Context context, int subId, String operatorNumeric) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)
                || operatorNumeric == null || !operatorNumeric.matches("[0-9]{5,6}")) {
            return null;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedIso = prefs.getString("iso_" + subId, null);
        String savedNumeric = prefs.getString("numeric_" + subId, null);
        if ((savedIso == null) != (savedNumeric == null)
                || (savedNumeric != null && !savedNumeric.equals(operatorNumeric))) {
            return null;
        }
        try {
            return resolveRestoreIso(operatorNumeric, savedIso);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String apply(Context context, int subId, String countryIso) throws Exception {
        synchronized (OPERATION_LOCK) {
            requireSubId(subId);
            String target = requireIso(countryIso);
            TelephonyManager phone = phoneFor(context, subId);
            String current = requireIso(phone.getSimCountryIso());
            String numeric = requireNumeric(phone.getSimOperator());
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String originalKey = "iso_" + subId;
            String numericKey = "numeric_" + subId;
            String savedNumeric = prefs.getString(numericKey, null);
            if (prefs.contains(originalKey) != prefs.contains(numericKey)) {
                throw new IllegalStateException("Incomplete country restore snapshot");
            }
            if (savedNumeric != null && !savedNumeric.equals(numeric)) {
                throw new IllegalStateException("SIM identity changed; refusing to overwrite its restore snapshot");
            }
            // The reported ISO may already be spoofed by this or another app. For mainland China
            // MCCs, the unmodified operator numeric gives us the physical SIM's country instead.
            String original = resolveRestoreIso(numeric, current);
            if (!prefs.contains(originalKey) && !prefs.edit()
                    .putString(originalKey, original)
                    .putString(numericKey, numeric)
                    .commit()) {
                throw new IllegalStateException("Could not save the original country before writing");
            }
            String result = withService(context,
                    service -> service.applyCountryIso(subId, target, numeric));
            verifyReportedCountry(phone, target);
            return result;
        }
    }

    public static String restore(Context context, int subId) throws Exception {
        synchronized (OPERATION_LOCK) {
            requireSubId(subId);
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String originalKey = "iso_" + subId;
            String numericKey = "numeric_" + subId;
            String original = prefs.getString(originalKey, null);
            String savedNumeric = prefs.getString(numericKey, null);
            TelephonyManager phone = phoneFor(context, subId);
            String numeric = requireNumeric(phone.getSimOperator());
            if ((original == null) != (savedNumeric == null)) {
                throw new IllegalStateException("Incomplete country restore snapshot");
            }
            if (savedNumeric != null && !savedNumeric.equals(numeric)) {
                throw new IllegalStateException("SIM identity changed; refusing to restore another card's snapshot");
            }
            String target = resolveRestoreIso(numeric, original);
            if (target == null) {
                throw new IllegalStateException("No reliable country for this SIM; nothing was changed");
            }
            String result;
            if (target.equalsIgnoreCase(phone.getSimCountryIso())) {
                result = "SIM country is already " + target;
            } else {
                result = withService(context,
                        service -> service.restoreCountryIso(subId, target, numeric));
                verifyReportedCountry(phone, target);
            }
            if (!prefs.edit().remove(originalKey).remove(numericKey).commit()) {
                throw new IllegalStateException("Country was restored but local snapshot cleanup failed");
            }
            return result;
        }
    }

    private interface ServiceCall { String run(ICountryOverrideService service) throws Exception; }

    private static String withService(Context context, ServiceCall call) throws Exception {
        if (!Shizuku.pingBinder() ||
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException("Shizuku is not running or has not granted permission");
        }
        Context app = context.getApplicationContext();
        CountDownLatch hostReady = new CountDownLatch(1);
        AtomicReference<IBinder> hostBinder = new AtomicReference<>();
        ServiceConnection hostConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                hostBinder.set(binder);
                hostReady.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { hostBinder.set(null); }
        };
        if (!app.bindService(new Intent(app, InstrumentationHostService.class),
                hostConnection, Context.BIND_AUTO_CREATE)) {
            throw new IllegalStateException("Could not start the instrumentation host");
        }
        try {
            if (!hostReady.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS) ||
                    hostBinder.get() == null || !hostBinder.get().pingBinder()) {
                throw new IllegalStateException("Instrumentation host did not become ready");
            }
            CountDownLatch serviceReady = new CountDownLatch(1);
            AtomicReference<ICountryOverrideService> remote = new AtomicReference<>();
            ServiceConnection serviceConnection = new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                    remote.set(ICountryOverrideService.Stub.asInterface(binder));
                    serviceReady.countDown();
                }
                @Override public void onServiceDisconnected(ComponentName name) { remote.set(null); }
            };
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(new ComponentName(
                    BuildConfig.APPLICATION_ID, CountryOverrideUserService.class.getName()))
                    .daemon(false).processNameSuffix("country_override")
                    .debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE);
            boolean bound = false;
            try {
                Shizuku.bindUserService(args, serviceConnection);
                bound = true;
                if (!serviceReady.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS) ||
                        remote.get() == null || !remote.get().asBinder().pingBinder()) {
                    throw new IllegalStateException("Shizuku country service did not become ready");
                }
                String result = call.run(remote.get());
                if (result == null || result.startsWith("ERROR:")) {
                    throw new IllegalStateException(result == null ? "CarrierConfig returned no result" : result);
                }
                return result;
            } finally {
                if (bound) {
                    try { Shizuku.unbindUserService(args, serviceConnection, false); }
                    catch (Throwable ignored) { }
                }
            }
        } finally {
            app.unbindService(hostConnection);
        }
    }

    private static void verifyReportedCountry(TelephonyManager phone, String expected)
            throws InterruptedException {
        for (int attempt = 0; attempt < 8; attempt++) {
            if (expected.equalsIgnoreCase(phone.getSimCountryIso())) return;
            Thread.sleep(250L);
        }
        throw new IllegalStateException("CarrierConfig command completed, but SIM country did not become "
                + expected + "; restore snapshot was retained");
    }

    private static TelephonyManager phoneFor(Context context, int subId) {
        TelephonyManager manager = context.getSystemService(TelephonyManager.class);
        if (manager == null) throw new IllegalStateException("TelephonyManager unavailable");
        return manager.createForSubscriptionId(subId);
    }

    private static void requireSubId(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new IllegalArgumentException("Invalid subscription ID: " + subId);
        }
    }

    static String requireIso(String iso) {
        String normalized = iso == null ? "" : iso.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z]{2}")) {
            throw new IllegalArgumentException("Country ISO must have exactly two letters");
        }
        return normalized;
    }

    static String requireNumeric(String numeric) {
        if (numeric == null || !numeric.matches("[0-9]{5,6}")) {
            throw new IllegalStateException("Real SIM operator numeric is unavailable");
        }
        return numeric;
    }

    /** Only assert a physical country for MCCs verified by this compatibility build. */
    static String resolveRestoreIso(String operatorNumeric, String savedIso) {
        String numeric = requireNumeric(operatorNumeric);
        String mcc = numeric.substring(0, 3);
        if ("460".equals(mcc)) return "cn";
        return savedIso == null ? null : requireIso(savedIso);
    }
}

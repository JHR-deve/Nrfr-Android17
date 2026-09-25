package com.github.nrfr.compat;

/* Derived from Ritel-T/SamsungRegionOverride (MIT); adapted for Nrfr K90 Lab. */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived instrumentation started by the shell-UID Shizuku UserService.
 *
 * <p>Samsung rejects CarrierConfig overrides made directly by the shell UID on recent firmware.
 * Instrumentation lets the app process temporarily adopt the shell phone-state permissions while
 * preserving the app's package identity expected by Samsung's service implementation.</p>
 */
public final class CarrierConfigInstrumentation extends Instrumentation {
    private static final String TAG = "CarrierConfigInstrumentation";
    static final String ARG_ACTION = "action";
    static final String ARG_SUB_ID = "sub_id";
    static final String ARG_COUNTRY_ISO = "country_iso";
    static final String ARG_OPERATOR_NUMERIC = "expected_operator_numeric";
    static final String ACTION_PROBE = "probe";
    static final String ACTION_APPLY_TRANSIENT = "apply_transient";
    static final String ACTION_CLEAR_TRANSIENT = "clear_transient";

    static final String RESULT_MESSAGE_BASE64 = "sro_message_b64";
    static final String RESULT_ERROR_BASE64 = "sro_error_b64";

    /** Bounds the reload wait so a device that never broadcasts still finishes the operation. */
    private static final long SETTLE_CEILING_MILLIS = 6000L;

    private static final String KEY_COUNTRY_ISO = "sim_country_iso_override_string";

    private Bundle arguments;

    @Override
    public void onCreate(Bundle arguments) {
        this.arguments = arguments == null ? Bundle.EMPTY : new Bundle(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        int resultCode = Activity.RESULT_CANCELED;
        String stage = "delegate permissions";
        try {
            requestShellPermissions();

            String action = arguments.getString(ARG_ACTION, ACTION_PROBE);
            stage = action;
            Log.i(TAG, "Executing CarrierConfig action=" + action);
            if (ACTION_PROBE.equals(action)) {
                result.putString("message", inspectRuntime());
            } else {
                int subId = intArgument(arguments.get(ARG_SUB_ID), -1);
                if (subId < 0) {
                    throw new IllegalArgumentException("Invalid subId: " + subId);
                }
                CarrierConfigManager manager = getTargetContext()
                        .getSystemService(CarrierConfigManager.class);
                if (manager == null) {
                    throw new IllegalStateException("CarrierConfigManager is unavailable");
                }
                String expectedNumeric = arguments.getString(ARG_OPERATOR_NUMERIC, "");
                TelephonyManager phone = getTargetContext()
                        .getSystemService(TelephonyManager.class);
                String actualNumeric = phone == null ? "" : phone
                        .createForSubscriptionId(subId).getSimOperator();
                if (!expectedNumeric.matches("[0-9]{5,6}")
                        || !expectedNumeric.equals(actualNumeric)) {
                    throw new IllegalStateException("SIM operator numeric changed before write");
                }

                if (ACTION_APPLY_TRANSIENT.equals(action)) {
                    PersistableBundle values = buildOverrideBundle(arguments);
                    boolean settled = writeAndAwaitReload(manager, subId, values);
                    result.putString("message", "App country: wrote a transient CarrierConfig override"
                            + "\nsubId=" + subId + ", ISO="
                            + values.getString(KEY_COUNTRY_ISO)
                            + (settled ? ""
                            : "\nNote: no carrier config reload broadcast arrived within the timeout;"
                            + " the value was still written."));
                } else if (ACTION_CLEAR_TRANSIENT.equals(action)) {
                    String restoredIso = restoreCountryIso(manager, subId, arguments);
                    result.putString("message", "App country: restored the reported SIM country ISO"
                            + " to " + restoredIso
                            + "\nOther transient and persistent CarrierConfig values were left alone");
                } else {
                    throw new IllegalArgumentException("Unknown action: " + action);
                }
            }
            resultCode = Activity.RESULT_OK;
        } catch (Throwable throwable) {
            if (throwable instanceof InterruptedException) Thread.currentThread().interrupt();
            result.putString("error", describeFailure(stage, throwable));
            Log.e(TAG, "CarrierConfig failed at " + stage, throwable);
        } finally {
            encodeResultForCommandHost(result);
            // Never create a UiAutomation client: permission delegation does not need accessibility.
            // Android 15+ AMS removes this instrumentation's delegated identity during normal
            // finish. The shell host checks for existing delegates before starting on older OSes.
            Log.i(TAG, "Finishing CarrierConfig instrumentation with code=" + resultCode);
            finish(resultCode, result);
        }
    }

    private void requestShellPermissions() throws InterruptedException {
        String token = arguments.getString(PermissionHandshake.ARG_TOKEN, "");
        if (!token.matches("[a-zA-Z0-9-]{32,64}")) {
            throw new IllegalArgumentException("Missing or invalid shell permission handshake token");
        }
        Bundle request = new Bundle();
        request.putString(PermissionHandshake.STATUS_KEY, token + ":" + android.os.Process.myUid());
        Log.i(TAG, "Requesting delegated phone permissions from the shell host");
        sendStatus(0, request);
        Context context = getTargetContext();
        PermissionHandshake.awaitPermissions(
                () -> context.checkSelfPermission(Manifest.permission.MODIFY_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED
                        && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED,
                SystemClock::uptimeMillis, Thread::sleep);
        Log.i(TAG, "Delegated phone permissions confirmed");
    }

    static String describeFailure(String stage, Throwable failure) {
        return stage + ": " + failure.getClass().getName()
                + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
    }

    private static void encodeResultForCommandHost(Bundle result) {
        encodeResultValue(result, "message", RESULT_MESSAGE_BASE64);
        encodeResultValue(result, "error", RESULT_ERROR_BASE64);
    }

    private static void encodeResultValue(Bundle result, String sourceKey, String encodedKey) {
        String value = result.getString(sourceKey);
        if (value == null) {
            return;
        }
        result.putString(encodedKey, Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)));
    }

    private static PersistableBundle buildOverrideBundle(Bundle arguments) {
        String iso = arguments.getString(ARG_COUNTRY_ISO, "")
                .trim().toLowerCase(Locale.ROOT);
        if (!iso.matches("[a-z]{2}")) {
            throw new IllegalArgumentException("Country ISO must be two letters");
        }
        PersistableBundle values = new PersistableBundle();
        values.putString(KEY_COUNTRY_ISO, iso);
        return values;
    }

    static int intArgument(Object value, int fallback) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String restoreCountryIso(CarrierConfigManager manager, int subId,
            Bundle arguments) throws Exception {
        String iso = arguments.getString(ARG_COUNTRY_ISO, "")
                .trim().toLowerCase(Locale.ROOT);
        if (!iso.matches("[a-z]{2}")) {
            throw new IllegalArgumentException("Original country ISO is unavailable");
        }
        PersistableBundle restore = new PersistableBundle();
        restore.putString(KEY_COUNTRY_ISO, iso);
        // Restore this fork's one key without passing null, because null would clear every transient
        // CarrierConfig override on the subscription, including values owned by another tool.
        if (!writeAndAwaitReload(manager, subId, restore)) {
            throw new IllegalStateException(
                    "Real country ISO reload was not confirmed");
        }
        return iso;
    }

    /**
     * Writes the override and does not return until the carrier config reload it triggers has landed.
     *
     * <p>{@code overrideConfig} returns as soon as the bundle is handed over, while the reload it kicks
     * off runs asynchronously. Returning before that reload finishes would leave the caller writing its
     * next layer into an indeterminate state, so this waits for
     * {@code ACTION_CARRIER_CONFIG_CHANGED} — the framework's own signal that the reload completed.</p>
     *
     * <p>Note what this does <em>not</em> do: it does not stop the IMS deregistration. That was the
     * hypothesis it was written for — that the SIM identity write was racing the reload — and it was
     * tested and did not hold. Applying both layers with the reload confirmed complete, and with an
     * extra 2.5s on top, still deregistered IMS on SM-S938B. It is kept because not racing an
     * asynchronous reload you just triggered is correct regardless, and because the negative result is
     * worth keeping reproducible; it is not kept as a fix, and nothing should be worded as though the
     * cost were gone.</p>
     *
     * <p>Waiting on {@code getSimCountryIso} was tried first and is useless here: it reports the new
     * value the instant the bundle is accepted, so it returns before anything has reloaded.</p>
     *
     * <p>Returns whether the reload was observed. A write that landed without a confirming broadcast is
     * still a write that landed, so this reports rather than throws.</p>
     */
    private boolean writeAndAwaitReload(CarrierConfigManager manager, int subId,
            PersistableBundle values) throws Exception {
        CountDownLatch reloaded = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int changed = intent.getIntExtra(
                        CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                if (changed == subId) {
                    reloaded.countDown();
                }
            }
        };
        Context context = getTargetContext();
        boolean observed = false;
        // Registered before the write, or a reload that finishes quickly is missed entirely and every
        // apply pays the full ceiling.
        context.registerReceiver(receiver,
                new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
        try {
            invokeTransientOverride(manager, subId, values);
            observed = reloaded.await(SETTLE_CEILING_MILLIS, TimeUnit.MILLISECONDS);
        } finally {
            try {
                context.unregisterReceiver(receiver);
            } catch (Throwable ignored) {
                // Instrumentation is ending either way; a receiver leak here outlives nothing.
            }
        }
        return observed;
    }

    private static String inspectRuntime() throws Exception {
        Method method = findOverrideMethod();
        return "instrumentationUid=" + android.os.Process.myUid()
                + "\ncarrierConfigMethod=" + signature(method);
    }

    private static void invokeTransientOverride(CarrierConfigManager manager, int subId,
            PersistableBundle values) throws Exception {
        Method method = findOverrideMethod();
        try {
            if (method.getParameterCount() == 3) {
                // The third argument is hard-coded false: this APK has no persistent write path.
                method.invoke(manager, subId, values, false);
            } else {
                method.invoke(manager, subId, values);
            }
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    // This app exists to exercise the platform's test-only override. Instrumentation starts with
    // hidden-API checks disabled and the method is probed at runtime because vendor signatures differ.
    @SuppressLint("BlockedPrivateApi")
    private static Method findOverrideMethod() throws NoSuchMethodException {
        try {
            return CarrierConfigManager.class.getDeclaredMethod(
                    "overrideConfig", int.class, PersistableBundle.class, boolean.class);
        } catch (NoSuchMethodException ignored) {
            return CarrierConfigManager.class.getDeclaredMethod(
                    "overrideConfig", int.class, PersistableBundle.class);
        }
    }

    private static String signature(Method method) {
        StringBuilder result = new StringBuilder(method.getName()).append('(');
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(types[i].getSimpleName());
        }
        return result.append(')').toString();
    }
}

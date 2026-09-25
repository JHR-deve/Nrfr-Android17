# Nrfr K90 Lab (local workspace)

This is an isolated Android development copy of Ackites/Nrfr. It is not
published and is an experimental, device-dependent Android 17 repair.

- Android application ID: `io.github.jhrdeve.nrfrk90`; the upstream app uses
  `com.github.nrfr`, so the two packages can coexist.
- The original hidden-class `persistent=true` shell write path is removed from
  the Android app. This fork writes only `sim_country_iso_override_string` with
  `persistent=false`; it does not alter the SIM card, MCC/MNC, IMSI, or carrier
  name. It records the current country and operator numeric before writing and
  refuses restoration if the operator numeric changed.
- The desktop `nrfr-client` launcher is included only as upstream source and
  still targets the original package. Do not use it with this test copy.
- The phone's currently working country overrides are out of scope for this
  migration and were not changed.
- The transient override may disappear after a reboot, SIM refresh, or OS
  update. The Restore button writes back the country captured before the first
  write by this fork; it does not clear another app's complete CarrierConfig.
- Do not assume universal compatibility. The app fails closed if no SIM, no
  phone permission, no Shizuku permission, a changed SIM operator, or an
  incompatible vendor implementation is detected.

Build with Android SDK 34 and JDK 21 using `:app:assembleDebug`. The output is
`app/build/outputs/apk/debug/app-debug.apk`. Test only after auditing the APK;
the K90's existing working country overrides are not a baseline to erase.

K90/API 37 test status (2026-09-25): both SIM slots completed a JP-to-JP
write/restore cycle. On SIM 1, an actual JP-to-SG change was read back as
`sg,jp`, then restored to `jp,jp`. A second JP-to-SG change persisted after
force-stopping this app; reopening the app restored JP and cleared its restore
snapshot. The default data SIM 2 was not changed to SG. No other phone was
available for runtime testing, so cross-vendor compatibility remains unverified.

This is a debug-signed local test APK, not a release artifact. Do not publish
or distribute it as a universally compatible build.

Static analysis caveat: `lintDebug` currently crashes inside the upstream
Compose lint detectors (`AutoboxingStateCreation` and, after disabling only
that one, `MutableCollectionMutableState`) with AGP 8.7.0-rc01/Kotlin 2.0.0.
This is a lint tool failure, not a clean lint report; the affected checks have
not been silently disabled in the project.

See `NOTICE-K90.md` and `LICENSE` for origin and licensing.

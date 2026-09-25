# Nrfr K90 Lab (local workspace)

This is an isolated Android development copy of Ackites/Nrfr. It is not
published and is not ready to install as an Android 17 fix.

- Android application ID: `io.github.jhrdeve.nrfrk90`; the upstream app uses
  `com.github.nrfr`, so the two packages can coexist.
- The source still contains Nrfr's original `persistent=true` shell write path.
  That path is known to fail on the K90's Android 17 build; do not use this
  copy to change live carrier configuration until the fix is implemented and
  safety-reviewed.
- The desktop `nrfr-client` launcher is included only as upstream source and
  still targets the original package. Do not use it with this test copy.
- The phone's currently working country overrides are out of scope for this
  migration and were not changed.
- Build status: `:app:assembleDebug` currently fails at `compileDebugKotlin`.
  The unmodified upstream checkout fails with the same unresolved hidden
  Android telephony classes (`TelephonyFrameworkInitializer`,
  `ICarrierConfigLoader`, and hidden `SubscriptionManager.getSubId`). This is
  an upstream baseline issue, not proof that the new application ID is wrong.
  No installable APK was produced by this migration.

Next engineering step: replace the hidden-API compile dependency with an
auditable Android 17-compatible carrier-config path, then build, inspect the
APK, and test on the K90 without disturbing the working installation.

See `NOTICE-K90.md` and `LICENSE` for origin and licensing.

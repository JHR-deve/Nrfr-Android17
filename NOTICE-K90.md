# Local derivative notice

This local project is based on [Ackites/Nrfr](https://github.com/Ackites/Nrfr),
upstream commit `c8044fe7d42beea496d66fae43b9683fcf942ce4`, licensed under
Apache License 2.0. The upstream `LICENSE` file is retained unchanged.

This is not an official Nrfr release. The Android 17 work replaces upstream's
hidden-class, persistent CarrierConfig write path with a country-only transient
instrumentation path, adds guarded restore snapshots, and changes the app ID,
name, SIM icon, and theme colors. Modified files are visible in Git history.

The compatibility bridge, permission handshake and instrumentation were adapted
from [Ritel-T/SamsungRegionOverride](https://github.com/Ritel-T/SamsungRegionOverride)
under the MIT license. Its full notice is retained at
`licenses/SamsungRegionOverride-MIT.txt`. The source files identify their origin.

Cross-device support is best effort: this mechanism depends on Android version,
vendor telephony implementation, Shizuku availability, and active SIM state.
The K90 test does not certify another model.

If this derivative is ever distributed, preserve the upstream license and
applicable notices, identify modified files, and review third-party dependency
licenses. The `nrfr-client` desktop launcher still targets upstream Nrfr and is
not part of this local Android test build.

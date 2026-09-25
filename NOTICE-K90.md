# Local derivative notice

This local project is based on [Ackites/Nrfr](https://github.com/Ackites/Nrfr),
upstream commit `c8044fe7d42beea496d66fae43b9683fcf942ce4`, licensed under
Apache License 2.0. The upstream `LICENSE` file is retained unchanged.

Changes in this local derivative so far: a distinct Android application ID and
display name, package-aware carrier-config reads, and an updated instrumentation
test assertion. This is not an official Nrfr release. The Android 17 write-path
repair has **not** yet been ported or validated in this copy.
The current upstream source also fails to compile against the public Android
SDK because it directly references hidden telephony classes; see README-K90.md.

If this derivative is ever distributed, preserve the upstream license and
applicable notices, identify modified files, and review third-party dependency
licenses. The `nrfr-client` desktop launcher still targets upstream Nrfr and is
not part of this local Android test build.

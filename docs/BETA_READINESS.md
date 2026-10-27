# Public-beta scope and validation status

**Candidate:** `v0.2.0-beta.1` / code **10** / `com.mobilefilmlab.camera`
**Scope:** first signed **public-testing beta**, not a stable release or stock-camera parity claim.

The owner reported alpha.9 working well on the primary handset and explicitly authorised the first public beta. This supersedes the earlier alpha-only publication restriction. It does **not** establish that every item in the historical hardware matrix was tested. The [old pre-beta tracker](archive/ALPHA_9_BETA_GATES.md) is retained as history, not silently checked off.

## Accepted release scope

- Primary development/testing target: **Oppo Find X9 Ultra**.
- Broader-device compatibility will expand through beta tester reports and follow-up fixes. No universal support claim.
- Preserve the working alpha.9 capture implementation; beta.1 primarily adds repository/documentation polish, truthful UI renders, AI disclosure and beta-aware release checks.
- Keep all 77 upstream recipes plus three independent Camera2 baselines.
- Expose only advertised camera capabilities; unavailable lenses/processing remain unavailable or visibly fall back.
- Ship with the established release package and certificate, higher versionCode, tests/lint, APK integrity verification and immutable prerelease assets.
- Scope publication approval to the exact tag `v0.2.0-beta.1`. No stable release is authorised by this approval.

## Evidence and open work

| Area | Status |
|---|---|
| Initial owner handset smoke feedback | Accepted: owner reported alpha.9 working well. Not a detailed per-feature validation report. |
| Automated regression/native-graphics tests and lint | Required by both CI and signed-release workflow. See [beta.1 QA evidence](QA_BETA_1.md) and tagged workflow results. |
| APK identity/signature/checksum/alignment | Required before publication; same established certificate and package. |
| Upgrade/data retention | Same identity and increasing code support in-place upgrades. Actual recipe/favourite retention still needs tester confirmation; do not uninstall to upgrade. |
| Every exposed lens, flash, manual control, focus/crop/orientation | Ongoing handset/firmware matrix; no assumption that one smoke test proves every combination. |
| Exact stock-camera colour/processing or native maximum resolution | Not claimed. Public Camera2 baseline and available streams only; optional enlargement is interpolation. |
| Measured per-lens chart calibration | Pending. Identity defaults/user controls are not factory calibration. |
| OEM HDR/Night, fusion and reprocessing ZSL | Experimental, capability-dependent; quality, latency and firmware behaviour remain under testing. |
| Long soak, memory, heat and battery acceptance | Pending broader stress testing; no zero-crash/zero-lag guarantee. |
| Other Android devices | Unverified until reports arrive; use the compatibility issue form. |
| Security review | No independent audit. AI assistance is [explicitly disclosed](AI_DISCLOSURE.md). |

The beta is public **to collect this evidence**, not because all hardware work is complete. Report failures via the [testing guide](TESTING.md). Do not promote to stable, declare proprietary parity or mark pending validation complete without new evidence.

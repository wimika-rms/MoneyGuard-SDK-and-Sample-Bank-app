# Sample Bank App Integration Notes

The sample app is a behavioral reference for Partner SDK consumers. Read [ipc_session_v2.md](/Users/mac/Documents/GitHub/moneyguard-docs/moneyguard-engineering-workspace/moneyguard-architecture/ipc_session_v2.md) before changing login or logout.

- `MoneyGuardResult.Success` can carry `HOST_ATTACHED`, `PARTNER_ONLY`, or `HOST_UNAVAILABLE`. A degraded host state may continue the bank login, but must be visible to the user/telemetry rather than silently treated as full protection.
- `UntrustedInstallationRequires2Fa` must route to verification. Do not call policy APIs with its empty/restricted token.
- A failed registration is not a safe risk result. The sample may make an explicit fail-open navigation decision only after recording the degraded state.
- Use the encrypted `PreferenceManager` for tokens. On logout, call Partner SDK logout as well as clearing sample state so Core removes that caller session.
- Treat account references returned by the bank as opaque `long` values. Never interpret, increment, enumerate, or substitute the bank database row ID for an external reference.
- After changing the Partner SDK, copy all three release AARs from `MoneyGuardPartnerSDK.Android` into `app/libs/`, then run `./gradlew :app:assembleDebug`.
- Every Sample Bank APK build must explicitly provide `SABI_BANK_BASE_URL` as a build-time environment variable (including release builds); for the deployed Sabi simulator use `SABI_BANK_BASE_URL=https://moneyguard-sabi-bank.azurewebsites.net/ ./gradlew :app:assembleRelease`. Do not rely on the localhost fallback for an installable artifact.

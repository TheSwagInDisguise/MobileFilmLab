# Security & Privacy Policy

## 1. Zero Telemetry & Privacy by Design
MobileFilmLab respects user privacy:
* **No Analytics:** No analytics SDKs (Google Analytics, Firebase, Mixpanel, etc.) are included.
* **No Telemetry / Crash Tracking:** No crash reporting services transmit diagnostic data over the network.
* **No Network Permission:** The application does not declare or request `android.permission.INTERNET`.
* **Zero Cloud Uploads:** All captures and custom recipes remain strictly on the local device.

## 2. Android Signing Security
* Release keystores are generated with RSA 4096-bit keys and stored securely as encrypted GitHub Actions repository secrets.
* Key material is never committed to Git, embedded in APK assets, or logged in CI workflow runs.

## 3. Reporting a Vulnerability
If you identify a security issue, please open an issue in the GitHub repository or contact the maintainers with a reproducible report.

# Security Policy

## Supported versions

Only the latest commit on the `main` branch is supported.

## Reporting a vulnerability

Please do not open a public issue for security problems.

Use GitHub's private vulnerability reporting: open the **Security** tab of this repository and choose **Report a vulnerability**. Include the affected component, build flavor (`full`, `demo`, `showcase`), reproduction steps and the impact you observed. Remove real customer data from any evidence you attach.

You can expect an acknowledgement within a few days. Confirmed issues are fixed on `main` and disclosed after a fix is available.

## Scope notes

- The app is local-first: data is stored in a SQLCipher-encrypted Room database and in private app directories; sharing and exports are initiated by the user.
- Release signing keys and `keystore.properties` must never be committed; `keystore.properties.example` contains placeholders only.
- Dependencies are monitored with Dependabot and secret scanning with push protection.

# Security Policy

## Supported versions

Security fixes are provided for the latest actively maintained beta/stable release. Older
versions may not receive fixes.

## Reporting a vulnerability

Do not post exploitable vulnerabilities, private keys, tokens, personal data or proof-of-
concept attacks in public issues.

Before public release, configure a maintainer-controlled security contact or enable GitHub
Private Vulnerability Reporting for this repository. Until then, do not describe the project as
having a staffed security response programme.

## Release integrity

- Production signing keys and passwords must remain outside source control.
- Release APKs must be signed with the maintainer-controlled production key.
- Publish SHA-256 checksums alongside release artifacts.
- Verify the signing certificate fingerprint for every release.
- Do not include debug endpoints, test credentials, secrets or verbose personal-data logs in
  production builds.

## Security review checklist

- Review exported activities, services, receivers and providers.
- Validate all external intents, URLs, provider identifiers and media identifiers.
- Use HTTPS and reject unexpected schemes/hosts.
- Keep Android permissions to the minimum required by implemented features.
- Review backup/data-extraction rules.
- Review dependencies and transitive dependencies for known vulnerabilities.
- Do not embed long-lived privileged API secrets in the APK.
- Do not add hidden admin accounts, remote kill switches, undocumented telemetry or remote
  code execution.

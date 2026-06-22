# Signed Release Pipeline

OmniTune publishes signed release APKs from GitHub Actions when a version tag is pushed.

## Keystore

Generate the release keystore outside the repository:

```bash
keytool -genkeypair \
  -v \
  -keystore omnitune-release.jks \
  -alias omnitune \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Keep the keystore private. Do not commit it, upload it as a release asset, or place it in the repo.

Encode it for GitHub Secrets:

```bash
base64 -w 0 omnitune-release.jks
```

On PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("omnitune-release.jks"))
```

## Required GitHub Secrets

Configure these repository secrets:

- `OMNITUNE_KEYSTORE_BASE64`
- `OMNITUNE_KEYSTORE_PASSWORD`
- `OMNITUNE_KEY_ALIAS`
- `OMNITUNE_KEY_PASSWORD`

The workflow decodes the keystore into `$RUNNER_TEMP/omnitune-release.jks`, builds the release APK, then deletes the temporary keystore in cleanup.

## Triggering A Release

Update `versionName` and `versionCode`, commit the change, then push a version tag:

```bash
git tag v0.6.4
git push origin main
git push origin v0.6.4
```

Tags matching `v*` trigger `.github/workflows/release.yml`.

## Expected Assets

The release workflow uploads:

- `OmniTune-vX.Y.Z-release.apk`
- `OmniTune-vX.Y.Z-release.apk.sha256`

The SHA-256 file is generated with `sha256sum` from the final uploaded APK name.

## Verification

The workflow verifies the APK with Android `apksigner` before upload:

```bash
apksigner verify --verbose OmniTune-vX.Y.Z-release.apk
```

Users can verify the downloaded asset:

```bash
sha256sum -c OmniTune-vX.Y.Z-release.apk.sha256
```

## Signing Key Warning

Android only updates an installed app when the package name and signing certificate match. Users who installed older test builds or APKs signed with a different key may need to uninstall once, then install the new signed release.

## Safety Rules

- Debug builds run on push and pull request without release secrets.
- Signed release builds run only for version tags.
- The release workflow never uploads debug APKs, unsigned APKs, keystores, `.idsig` files, zips, or source bundles as release assets.
- Release builds fail locally unless the required `OMNITUNE_*` signing environment variables are set.

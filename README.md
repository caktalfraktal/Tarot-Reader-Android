# Tarot Reader for Android v1.4

Android source for Tarot Reader v1.4 (build 11), package `com.christian.tarot`. Includes the centered vector sun toggle, original card artwork, card data and reading prompts. Android 6.0 (API 23) or newer is required.

## What is included

Complete Android Java/WebView source, all 78 card images and the card back, launcher icon, Gradle 8.9 wrapper, and a Python build/signing helper. Windows Python sources, build caches, machine settings, signing keys, passwords and the Groq API key are excluded. No license has been assigned; confirm rights to bundled artwork before public redistribution.

The app code and assets match the personal v1.4 project, except that the private `groq-key.txt` asset is omitted. No shuffle, draw, reading, layout or theme logic was changed for this export. Only the Gradle project display name was corrected to TarotReader.

## Build

Install Python 3, JDK 17 or 21, and Android Studio. Install Android SDK Platform 34 and Build-Tools 34.0.0. Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your Android SDK. The initial build needs internet to download Gradle and dependencies.

For AI readings, privately create `android/app/src/main/assets/groq-key.txt` containing your Groq API key. This filename is ignored by Git. You can copy your existing private asset from the original project. Without a key, card draws and card meanings work, but AI reading requests show the existing service-unavailable message.

Run from this folder:

```text
python scripts/build_android.py
```

Use `python3` where appropriate. You may pass `--sdk "PATH_TO_ANDROID_SDK"`. The helper builds, signs, checks the signature and checks APK alignment. Output: `release/TarotReader-Android-v1.4.apk`.

## Signing and recovery

The helper generates a new signing identity automatically when none exists. It saves the key and generated password in `.private-signing/` and reuses them on later builds; you do not need to enter a password. Back up this directory privately.

To update your existing installed personal app, use the original signing files by passing `--key-dir "PATH_TO_PRIVATE_SIGNING_DIRECTORY"`, or copy both files privately into `.private-signing/`. The filenames are `threecoins-release.p12` and `release-password.txt`, with alias `threecoins`, because the original Tarot app used that signing identity too. These are filenames only; no signing material is included in this export.

If the key or password is lost, use:

```text
python scripts/build_android.py --key-dir .private-signing/replacement
```

This generates a new identity. Uninstall the old app before installing an APK signed with the new key; uninstalling removes local app data. Source code and APKs cannot recover a lost private key. A lost Groq API key must be replaced separately through your account.

## GitHub safety

Upload this folder's contents, including `.gitignore`, `.gitattributes` and the Gradle wrapper JAR. Do not upload your existing personal APK: its embedded API key can be extracted. APKs, signing files, private key assets and generated release output are intentionally ignored here. Do not force-add these excluded files.

Ignoring a key file does not remove that key from an APK. Any personal build containing `groq-key.txt` must also remain private. A publicly distributed app with AI readings needs a separate credential design before release.

## Casting behavior

Each draw requests a fresh RANDOM.ORG permutation of 0 through 77, validates its size, range and uniqueness, and uses the first 1, 3 or 10 entries in their returned order. If the request fails, it shuffles a fresh deck using SecureRandom and labels the source. The question does not affect card selection. Cards are upright; the second Celtic Cross card is rotated for layout only. Groq receives the selected card names, spread prompt and question to generate the reading. Closing/backgrounding resets the reading; the theme preference is retained.

## UI smoke checks

With Node.js installed, run `node tests/ui-smoke-test.js`. These mock-DOM checks cover initial reset, theme toggles, card layouts at five widths, query handling and reading callbacks. They do not replace testing on a phone.

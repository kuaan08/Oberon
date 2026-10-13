<div align="center">
  <h1>Oberon Extended Mobile</h1>
  <p>Turn your Android phone or a controller connected to it into an Xbox remote input source.</p>
  <p><strong>Android native · LAN WebSocket · Touch and USB/Bluetooth controllers</strong></p>
</div>

---

Oberon Extended Mobile is an Android companion for the original [Oberon Remote Input](https://github.com/SamsidParty/OberonRemote) application for Xbox. It replaces the **Windows input sender**, not the Xbox-side app.

## Getting started

1. Open [Android build artifacts](https://github.com/kuaan08/Oberon/actions/workflows/oberon-extended-mobile.yml) and download the **Oberon Extended Mobile Android Debug APK** from a successful run. The first version is an experimental alpha build.
2. Install the APK on an Android 8.0 or newer phone. Android may ask permission to install apps from your browser or file manager.
3. Put your phone and Xbox on the **same trusted Wi-Fi network**.
4. Open Oberon Extended Mobile. Tap **START SERVER**.
5. On Xbox, open the original Oberon Remote Input application, and enter the phone's IP address displayed at the top of the mobile screen (port **26401**).
6. Select **Touch controls** to use the screen gamepad, or pair a Bluetooth/USB controller with Android and select **Bluetooth / USB**.

You do not need the Windows application when using the Android sender.

## What's included

- Original Oberon WebSocket protocol: request `0xFA`, response 100 bytes, one active gamepad slot, original Xbox handshake.
- Xbox-green dark theme and an optional light theme.
- Landscape, multi-touch virtual gamepad with two analog sticks, ABXY, D-pad, shoulders, triggers, Start and View.
- Physical controller input through Android's gamepad/joystick APIs (driver and model support vary).
- Local IP display, copy-IP button, server start/stop, Xbox connection status.
- Foreground connection service with a persistent notification while running.
- Adjustable stick deadzone and reset-to-default settings.
- Automated protocol unit tests and Android APK build through GitHub Actions.

## Important limitations

This is an **alpha build**, not a tested replacement for all desktop features. The Android transmitter currently sends **one controller slot**, not four. Physical controllers are read while the app is visible; keeping the network service alive does not guarantee background gamepad capture. Controller rumble is not implemented. Touch controls work best in landscape orientation.

The Xbox receiver must remain installed and running on the Xbox. This Android app cannot use the Xbox-only Windows UWP `InputInjector` API. It sends input to that existing receiver over local Wi-Fi. Using mobile data, port forwarding, or public Wi-Fi is not supported or recommended.

## Building the Android app

Requires Android SDK API 35, JDK 17, and Gradle 8.11.1.

```bash
cd mobile
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK will be created at `mobile/app/build/outputs/apk/debug/app-debug.apk`. This APK is debug-signed for testing; a distributable release would need your own signing key and hardware testing.

## Project layout

The Android app is in [mobile](mobile/). The original Xbox UWP receiver is retained in [Oberon](Oberon/) as protocol/reference source. The legacy Windows desktop code is not included in this Android development branch.

## Credits

**Original Oberon Remote Input:** [SamsidParty](https://github.com/SamsidParty/OberonRemote) and the project's contributors.

**Extended project:** [katakishi0](https://github.com/katakishi0).

The original [license](LICENSE) remains in effect, including author attribution and publicly available modified source. The Windows/Xbox-side implementation remains subject to its platform requirements. Oberon Extended Mobile is an independent Android input sender and is not affiliated with Microsoft.

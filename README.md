<div align="center">
  <h1>Oberon Extended Mobile</h1>
  <p>Your Android phone becomes a remote controller for Xbox, through the original Oberon receiver.</p>
  <p><strong>Native Android · Portrait and Landscape · Bluetooth / USB · Full-screen touch</strong></p>
</div>

---

## Download for Android

Get the current **Alpha 2** APK from [GitHub Actions](https://github.com/kuaan08/Oberon/actions/workflows/oberon-extended-mobile.yml). Open a successful run, then download the **Oberon Extended Mobile Alpha2 Debug APK** artifact. Unzip it, and install the enclosed APK on Android 8.0+.

The build is debug-signed for testing. No separate Windows PC is required for the mobile sender.

## How to connect

1. Put the Android device and Xbox on the same trusted Wi-Fi network.
2. Connect a Bluetooth or USB gamepad to Android if you wish to use a physical controller.
3. Open Oberon Extended Mobile. It starts on the **Bluetooth / USB** dashboard by default.
4. Tap **Iniciar servidor** (Start server).
5. Open the original Oberon Remote Input receiver on the Xbox.
6. Enter the Android IP address displayed by the app, including port `26401`, and connect.

The Xbox receiver is necessary. Android sends inputs over a local WebSocket and cannot inject Xbox input without the receiving application.

## What's new in Alpha 2

The app has a redesigned dashboard that automatically fits **portrait and landscape** screens. The default is Bluetooth/USB, with a simple controller-detection area and a separate connection panel.

**Touch controls now fill the whole screen** instead of appearing as a tiny element in the dashboard. Tap **Abrir controle virtual** to open them, and **Voltar** to return. A separate touch-controller layout is available for portrait and landscape:

- Portrait: tall handheld layout, with two analog sticks, face buttons, D-pad, shoulders and triggers.
- Landscape: full-width layout with independent stick, D-pad, face-button and trigger hit zones.
- Pointer-aware multi-touch; hold sticks and multiple buttons simultaneously.
- Animated press feedback and optional haptic vibration.

**Settings is a full, dedicated page** instead of a modal popup. It includes six themes (Xbox Green, Xbox Light, Midnight, Graphite, Violet and Ocean), animation and vibration preferences, a controller button-size slider, touch orientation, analog deadzone and reset-to-defaults.

The `\\n` text-escape issue from Alpha 1 is resolved: text now wraps as real lines rather than showing literal escape characters.

## Known limitations

This is still an **experimental alpha**, not a complete replacement for the desktop Extended application. The transmitter supports one Xbox controller slot per connection. Real-controller input is read while the Android dashboard is in the foreground. Background service functionality keeps the WebSocket listening, but cannot capture controller input when Android stops sending gamepad events to the app. Rumble is not implemented.

The application has passed automated Android compilation and protocol tests when a successful build is available. This is not proof of compatibility with every controller, Android model or Xbox configuration. Do not expose port `26401` on the public internet.

## Source and credits

- Android application: [mobile/](mobile/)
- Original Xbox receiver for compatibility reference: [Oberon/](Oberon/)
- Developer workflow: [.github/workflows/oberon-extended-mobile.yml](.github/workflows/oberon-extended-mobile.yml)
- Original Oberon Remote Input: [SamsidParty](https://github.com/SamsidParty/OberonRemote) and contributors
- Extended maintenance and branding: [katakishi0](https://github.com/katakishi0)

See [LICENSE](LICENSE) for the original project's attribution and source-disclosure requirements. Oberon Extended Mobile is an independent Android transmitter and is not affiliated with Microsoft.

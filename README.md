<div align="center">
  <h1>Oberon Extended Mobile</h1>
  <p>Android gamepad transmitter for the original Oberon Remote Input Xbox receiver.</p>
  <p><strong>Native Android • Bluetooth / USB • Full-screen touch • Portrait and landscape</strong></p>
</div>

---

## Download the Android APK

This project is in **Alpha 3**. Download the Android debug build from a successful [GitHub Actions run](https://github.com/kuaan08/Oberon/actions/workflows/oberon-extended-mobile.yml). Select **Oberon Extended Mobile Alpha3 Debug APK**, extract the ZIP, and install the enclosed APK on Android 8.0 or newer.

The Android sender does not require the Windows version. The existing original Oberon Xbox receiver is still required.

## Connect your Xbox

1. Put your phone and Xbox on the same trusted local Wi-Fi network.
2. Pair a supported USB or Bluetooth controller with Android, or open the touch controller.
3. Open Oberon Extended Mobile and select **Iniciar servidor**.
4. On Xbox, open the original Oberon Remote Input application.
5. Enter the Android device IP and port **26401** shown in the app and connect.

## Alpha 3 features

**Responsive dashboard.** The home screen works vertically or horizontally. Bluetooth/USB is the default input mode. A separate full-screen touch controller supports portrait and landscape layouts.

**Fully themed interface.** Choose Xbox Green, Xbox Light, Midnight, Graphite, Violet, or Ocean. The help and confirmation dialogs follow the selected Extended theme. Animations and touch vibration are optional.

**Two independent orientation settings.** Set the orientation of the **whole Android app** to Auto, Vertical or Horizontal, and choose Vertical or Horizontal separately for the **virtual touch controller**.

**Editable virtual controls.** Adjust overall controller scale, individual analog-stick size, face-button and D-pad size, and control opacity. Multi-touch lets sticks and face buttons be pressed simultaneously.

**Detailed mode and Diagnostics.** Optional live stats on the home screen, plus a dedicated diagnostics screen showing connected controllers, likely controller family, Android device identifiers, axis capabilities, battery if provided by the driver, connection status, packet counters, and intervals between Xbox requests. **Request interval is not Xbox round-trip latency or ping.** A controller battery displayed as unavailable or unknown does not mean it is empty.

**Experimental compatibility.** Choose a particular Android input device, switch the right stick between Z/RZ and RX/RY mappings, swap A/B buttons, invert stick Y, and toggle TCP_NODELAY to compare local socket behavior. The compatibility options cannot guarantee every generic controller will work. Restart the server after changing TCP_NODELAY.

**Updates.** The Updates screen checks public [GitHub Releases](https://github.com/kuaan08/Oberon/releases), including preview releases, and provides official download links when applicable. If no release has been published, it points to the development builds on GitHub Actions. The app never silently installs APKs.

**Connection service.** The WebSocket server runs in a foreground service with a persistent notification. Android may not deliver hardware controller events while the UI is backgrounded.

## Current limitations

This is a test build, not a release tested with every physical device or Xbox. Only one controller slot is transmitted, physical gamepad input requires Android to send input events to the app, and gamepad rumble is not yet implemented. Real Xbox latency cannot be measured using the current request/response protocol. The TCP_NODELAY toggle only affects the local TCP socket.

Use the server on a trusted local network only. Do not expose port `26401` to the internet.

## Building from source

Install JDK 17, Android SDK API 35 and Gradle 8.11.1, then run:

```bash
cd mobile
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK output: `mobile/app/build/outputs/apk/debug/app-debug.apk`. Debug signing is intended for testing, not production distribution.

## Credits

- Original Oberon Remote Input: [SamsidParty](https://github.com/SamsidParty/OberonRemote) and contributors.
- Extended project maintenance: [katakishi0](https://github.com/katakishi0).
- Android source: [mobile/](mobile/); original Xbox receiver protocol/reference: [Oberon/](Oberon/).

See [LICENSE](LICENSE) for upstream attribution, modification and source-code obligations. Oberon Extended Mobile is independent of Microsoft and is not affiliated with Xbox.

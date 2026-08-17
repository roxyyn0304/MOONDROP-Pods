<div align="center">

<img src="docs/icon.png" width="110" height="110" style="border-radius: 22px;" alt="MOONDROP-Pods Icon"/>

# 🎧 MOONDROP-Pods

### System-level MOONDROP earphone control for HyperOS devices

[![Platform](https://img.shields.io/badge/Platform-Android%2015%2B-green?style=flat-square&logo=android&logoColor=white)](https://www.android.com)
[![Framework](https://img.shields.io/badge/Framework-LSPosed-blueviolet?style=flat-square&logo=android&logoColor=white)](https://github.com/LSPosed/LSPosed)
[![ROM](https://img.shields.io/badge/ROM-HyperOS-orange?style=flat-square&logo=xiaomi&logoColor=white)](https://hyperos.mi.com)
[![Protocol](https://img.shields.io/badge/Protocol-GAIA%20V3-blue?style=flat-square)](https://github.com/roxyyn0304/moondrop-gaia-protocol)
[![License](https://img.shields.io/badge/License-GPL--3.0-red?style=flat-square)](LICENSE)

**English** | **[简体中文](README.md)**

</div>

---

## 📖 About

An Xposed module for **MOONDROP earphones** on Xiaomi HyperOS, based on [OppoPods](https://github.com/1812z/OppoPods).

It deeply integrates the MOONDROP Pudding TWS earphones into HyperOS:

- System-level **ANC / Gain** control with Hyper Island and Fusion Device Center support
- Direct SPP communication based on the [moondrop-gaia-protocol](https://github.com/roxyyn0304/moondrop-gaia-protocol)
  reverse-engineered library (verified via btsnoop captures)

---

## ✨ Features

### 🎧 Earphone Control

| Feature | Description |
|---------|-------------|
| 🎛️ **ANC Control** | Off / Transparency / Noise Cancellation (Adaptive) / Anti-Wind |
| 🎚️ **Gain Control** | High / Medium / Low |
| 🔋 **Battery** | Left / Right / Case battery levels |
| 📶 **ANC Capability** | Auto-detect supported noise cancellation modes |

### 🏝️ HyperOS Integration

| Feature | Description |
|---------|-------------|
| 🏝️ **Hyper Island** | Official Hyper Island or module-built-in Island |
| 🔗 **Fusion Device Center** | Device card control and one-tap transfer |
| ⚙️ **Settings Integration** | State sync in system Bluetooth settings |
| 🎭 **Model Spoofing** | Spoofs a supported Xiaomi earphone model |

### 🪟 Popup & Experience

| Feature | Description |
|---------|-------------|
| 🪟 **Connection Popup** | Bottom card on connect: device name + animation + battery, auto-dismiss |
| 🔋 **Battery Island** | Temporary island with circular progress for L/R/Case, dark mode support |
| 💬 **Persistent Notification** | Earphone card in notification bar (battery + ANC cycle + disconnect) |
| 🚀 **Quick Popup** | Tap notification / Control Center card for a floating control popup |

### 🛠️ Module Capabilities

| Feature | Description |
|---------|-------------|
| 📜 **Bluetooth Log Viewer** | Real-time protocol RX/TX (FF04 frame parsing + Chinese labels + HEX coloring) |
| 🌌 **Dynamic Background** | OS3 shader background effect (auto-enabled on Android 16+) |
| 🔍 **RFCOMM Debug** | Debug page with HEX send for testing |
| 🖼️ **Earphone Images** | Custom earphone / case images and connection animation |

---

## 🚀 Getting Started

### Requirements

- Xiaomi device running **HyperOS** (Android 15+; Hyper Island requires OS3)
- **LSPosed** API version ≥ 101

### Installation

1. Install the APK (download from [Releases](https://github.com/roxyyn0304/MOONDROP-Pods/releases))
2. Enable the module in **LSPosed** and select the recommended scopes:
   - `com.android.bluetooth`
   - `com.milink.service`
   - `com.xiaomi.bluetooth`
   - `com.android.settings`
3. Use the "**Restart Scope**" button in the module (root required)
4. Connect your MOONDROP earphones via Bluetooth

### Build from Source

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚠️ Important

> ### 🚫 Always control your earphones from the module's own UI (ANC / Gain / Battery, etc.)
>
> The earphone interface in the system Bluetooth settings (device card, detail page, etc.)
> shows state injected by this module, which **may not be accurate**.
>
> **Do not change any settings in the system Bluetooth earphone interface** — issues caused by
> changes made there (wrong state, settings not taking effect, etc.) are **not supported**.
>
> Always control the earphones through the **module app / module popup**.

---

## 🙏 Credits

- [OppoPods](https://github.com/1812z/OppoPods) by 1812z — original project
- [Miuix](https://github.com/YuKongA/miuix) — HyperOS-style Compose UI components
- [moondrop-gaia-protocol](https://github.com/roxyyn0304/moondrop-gaia-protocol) — MOONDROP protocol library

## 📄 License

[GPL-3.0](LICENSE)

<div align="center">

*Made with ❤️ for MOONDROP Pudding users*

</div>

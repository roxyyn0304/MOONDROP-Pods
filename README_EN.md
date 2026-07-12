<div align="center">

<img src="https://github.com/user-attachments/assets/e8a3df6b-6e67-485a-ae1c-018ac24e87d4" width="120" height="120" style="border-radius: 24px;" alt="MOONDROP-Pods Icon"/>

# MOONDROP-Pods

**System-level MOONDROP earphone control for HyperOS devices**

[![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android)](https://android.com)
[![LSPosed](https://img.shields.io/badge/Framework-LSPosed-blueviolet?style=flat-square)](https://github.com/LSPosed/LSPosed)
[![HyperOS](https://img.shields.io/badge/ROM-HyperOS-orange?style=flat-square)](https://hyperos.mi.com)


**English** | **[简体中文](README.md)**

</div>


An Xposed module that provides system-level MOONDROP earphone control for Xiaomi HyperOS devices.


### Earphone Features

- **Noise Cancellation Control** — Switch between Off / Transparency / Noise Cancellation modes
- **Gain Control** — Switch between High / Medium / Low gain levels

### HyperOS Integration
- **Hyper Island** — Supports the official Hyper Island or the module's built-in Hyper Island
- **Fusion Device Center** — Supports controls in Fusion Device Center
- **Settings Integration** — Supports controls in system Bluetooth settings
- **Device Transfer** — Supports one-tap multi-device transfer in Fusion Device Center
- **Model Spoofing** — Spoofs a supported Xiaomi earphone model

### Module Features
- **Quick Popup** — Tap the notification or Control Center earphone card to open a floating popup with battery and noise cancellation controls; tap "More" to enter the full page
- **Quick Launch** — From the notification or Control Center earphone card, quickly jump to module settings or system settings

### System Requirements

- Xiaomi device running **HyperOS** (Android 15+) (Hyper Island only supports OS3)
- **LSPosed** API version >= 101

### Usage

1. Install the APK
2. Enable the module in LSPosed and select the recommended scopes
3. Use the one-tap scope restart button in the top-right corner of the app
4. Connect your MOONDROP earphones via Bluetooth

### Credits

- [OppoPods](https://github.com/1812z/OppoPods) by 1812z — original project
- [Miuix](https://github.com/YuKongA/miuix) — HyperOS-style Compose UI components
- [moondrop-gaia-protocol](https://github.com/roxyyn0304/moondrop-gaia-protocol) — MOONDROP protocol library

### License

GPL-3.0

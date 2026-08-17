<div align="center">

# 🎧 MOONDROP-Pods

### 为 HyperOS 设备提供系统级 MOONDROP 耳机控制

[![Platform](https://img.shields.io/badge/Platform-Android%2015%2B-green?style=flat-square&logo=android&logoColor=white)](https://www.android.com)
[![Framework](https://img.shields.io/badge/Framework-LSPosed-blueviolet?style=flat-square&logo=android&logoColor=white)](https://github.com/LSPosed/LSPosed)
[![ROM](https://img.shields.io/badge/ROM-HyperOS%20%2F%20澎湃OS-orange?style=flat-square&logo=xiaomi&logoColor=white)](https://hyperos.mi.com)
[![Protocol](https://img.shields.io/badge/Protocol-GAIA%20V3-blue?style=flat-square)](https://github.com/roxyyn0304/moondrop-gaia-protocol)
[![License](https://img.shields.io/badge/License-GPL--3.0-red?style=flat-square)](LICENSE)

**English** | **简体中文**

</div>

---

## 📖 简介

基于 [OppoPods](https://github.com/1812z/OppoPods) 改造的 **MOONDROP 耳机 Xposed 模块**，
把 MOONDROP Pudding（布丁）TWS 耳机深度接入小米 HyperOS：

- 系统级 **ANC / 增益** 控制，支持超级岛、融合设备中心
- 基于 [moondrop-gaia-protocol](https://github.com/roxyyn0304/moondrop-gaia-protocol)
  逆向协议库（btsnoop 抓包验证）直连耳机 SPP

---

## ✨ 功能一览

### 🎧 耳机控制

| 功能 | 说明 |
|------|------|
| 🎛️ **ANC 降噪控制** | 关闭 / 通透 / 降噪（自适应）/ 抗风噪 |
| 🎚️ **增益控制** | 高 / 中 / 低 三档 |
| 🔋 **实时电量** | 左耳 / 右耳 / 充电盒 三端显示 |
| 📶 **ANC 能力检测** | 自动探测设备可用降噪模式 |

### 🏝️ HyperOS 集成

| 功能 | 说明 |
|------|------|
| 🏝️ **超级岛** | 支持官方超级岛 或 模块内建超级岛 |
| 🔗 **融合设备中心** | 支持设备卡片控制与一键流转 |
| ⚙️ **设置集成** | 系统蓝牙设置页状态同步 |
| 🎭 **型号伪装** | 伪装为受支持的小米耳机型号 |

### 🪟 弹窗与体验

| 功能 | 说明 |
|------|------|
| 🪟 **连接弹窗** | 连接时弹出底部卡片：设备名 + 动画 + 三端电量，自动关闭 |
| 🔋 **电量岛** | 临时电量岛：左耳/右耳/充电盒 圆形进度 + 深色模式适配 |
| 💬 **常驻通知** | 通知栏耳机卡片（电量 + 降噪循环 + 断开） |
| 🚀 **快捷弹窗** | 点击通知/控制中心卡片弹出浮窗，一键直达更多功能 |

### 🛠️ 模块能力

| 功能 | 说明 |
|------|------|
| 📜 **蓝牙日志查看器** | 实时查看协议收发（FF04 帧解析 + 中文标签 + HEX 着色） |
| 🌌 **动态背景** | OS3 着色器背景特效（Android 16+ 自动启用） |
| 🔍 **RFCOMM 调试** | 调试页支持 HEX 发送测试 |
| 🖼️ **耳机图管理** | 自定义耳机/充电盒图片与连接动画 |

---

## 🚀 快速开始

### 环境要求

- 小米设备，运行 **HyperOS**（Android 15+，超级岛仅支持 OS3）
- **LSPosed** API 版本 ≥ 101

### 安装步骤

1. 安装 APK（[Releases](https://github.com/roxyyn0304/MOONDROP-Pods/releases) 页面下载）
2. 在 **LSPosed** 中启用模块，勾选推荐作用域：
   - `com.android.bluetooth`
   - `com.milink.service`
   - `com.xiaomi.bluetooth`
   - `com.android.settings`
3. 使用模块右上角「**一键重启作用域**」（需 root）
4. 蓝牙连接你的 MOONDROP 耳机

### 开发者构建

```bash
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚠️ 重要提醒

> ### 🚫 请使用模块内的界面进行耳机控制（ANC / 增益 / 电量等）
>
> 系统蓝牙设置里的耳机界面（设备卡片、详情页等）所显示的状态是由模块注入的，**显示可能不准确**。
>
> **请不要在系统蓝牙的耳机界面中更改任何设置** —— 在系统蓝牙界面更改引发的任何问题
> （状态错乱、设置不生效等）**不予处理**。
>
> 耳机的一切控制请通过 **模块 App / 模块弹窗** 完成。

---

## 🙏 致谢

- [OppoPods](https://github.com/1812z/OppoPods) by 1812z — 原始项目
- [Miuix](https://github.com/YuKongA/miuix) — HyperOS 风格 Compose UI 组件
- [moondrop-gaia-protocol](https://github.com/roxyyn0304/moondrop-gaia-protocol) — MOONDROP 协议库

## 📄 许可证

[GPL-3.0](LICENSE)

<div align="center">

*Made with ❤️ for MOONDROP Pudding users*

</div>

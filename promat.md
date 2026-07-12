# MOONDROP-Pods 项目说明

## 这是什么

基于 [OppoPods](https://github.com/1812z/OppoPods) Xposed 模块改造的 MOONDROP 耳机适配模块，为小米 HyperOS 设备提供系统级 MOONDROP 耳机控制。

原始项目为 OPPO 耳机设计，本项目将其改造为支持 MOONDROP Pudding (布丁) TWS 耳机。

## 当前状态

已完成从 OPPOPods 到 MOONDROP-Pods 的完整改造：
- 品牌重命名：OPPO → MOONDROP
- 协议替换：OPPO RFCOMM → MOONDROP GAIA V3 SPP
- 包名更新：`moe.chenxy.oppopods` → `moe.chenxy.moondropods`

## 设备信息

| 参数 | 值 |
|------|-----|
| 设备 | MOONDROP Pudding (MD-TWS-056) |
| 芯片 | 杰理 (Jieli) VID=0x05D6 |
| 蓝牙 | SPP (标准串口协议) |
| Vendor ID | 29 (0x001D) |

## 协议格式 (MOONDROP GAIA V3)

```
FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [Feature:1] [Cmd:1] [Payload...]
```

- Len = 1 + 1 + payload.size（feature + cmd + payload）
- 总包长 = 8 + payload.size
- 响应 Feature = 请求 Feature | 0x01 (bit0)

### Feature ID

| Feature | 用途 |
|---------|------|
| 0x00 | 基础查询（固件版本、序列号、设备ID、EQ、配置、设备状态） |
| 0x1E | Gain 增益控制 |
| 0x20 | 编解码器（LDAC/LC3） |
| 0x40 | ANC 降噪控制 |

### ANC 模式

| 值 | 模式 | 状态 |
|----|------|------|
| 0x00 | 关闭 | ✓ 可用 |
| 0x01 | 通透 | ✓ 可用 |
| 0x02 | 降噪 | ✓ 可用 |
| 0x08 | 自适应 | ✗ 暂不可用 |
| 0x10 | 抗风噪 | ✗ 暂不可用 |

### Gain 级别

| 值 | 级别 |
|----|------|
| 0x00 | 高 |
| 0x01 | 中 |
| 0x02 | 低 |

## 功能支持

### 已支持
- ANC 降噪控制（关闭/通透/降噪）
- Gain 增益控制（高/中/低）
- 超级岛集成
- 融合设备中心控制
- 系统蓝牙设置集成

### 暂不支持
- ANC 自适应模式
- ANC 抗风噪模式
- 电量查询（设备无响应）
- 游戏模式
- 空间音频
- EQ 预设
- 双设备连接
- 佩戴检测

## 技术栈

- Kotlin + Jetpack Compose
- Miuix UI 组件库
- LSPosed Xposed 模块
- Bluetooth SPP/RFCOMM

## 项目结构

```
app/src/main/java/moe/chenxy/moondropods/
├── config/          # 配置管理
├── hook/            # Xposed 钩子
├── pods/            # 耳机通信协议
│   ├── Packets.kt           # MOONDROP GAIA 协议定义
│   ├── RfcommController.kt  # RFCOMM 控制器
│   └── DeviceCapabilities.kt # 设备能力检测
├── ui/              # UI 层
├── utils/           # 工具类
├── MainActivity.kt  # 主入口
├── MoondropPodsApp.kt # Application 类
└── PopupActivity.kt # 快捷弹窗 Activity
```

## 构建与测试

```bash
# 编译 APK
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 查看日志
adb logcat | grep MoondropPods
```

## 已知问题

1. 电量查询无响应（设备限制）
2. ANC 自适应/抗风噪子模式暂不可用
3. 部分蓝牙适配器 (MediaTek) RFCOMM 驱动异常
4. SET 响应的 [1][2] 字节含义未知

## 参考资料

- [OppoPods](https://github.com/1812z/OppoPods) — 原始项目
- [moondrop-gaia-protocol](https://github.com/roxyyn0304/moondrop-gaia-protocol) — MOONDROP 协议库
- [SpaceTravel-Protocol](https://github.com/pubglite55/SpaceTravel-Protocol) — Space Travel 抓包数据
- [moondrop-spp-controller](https://github.com/ribentianhuang38-boop/moondrop-spp-controller) — Android SPP 控制实现

## License

GPL-3.0

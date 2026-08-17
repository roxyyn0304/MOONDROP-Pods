# MOONDROP-Pods Transformation Design

## [S1] Problem

Transform the existing OPPOPods Xposed module into MOONDROP-Pods, supporting MOONDROP Pudding (布丁) TWS earphones on Xiaomi HyperOS devices. The current project uses OPPO's RFCOMM protocol; MOONDROP uses a completely different GAIA protocol with Jieli (杰理) chips.

## [S2] Scope

Complete transformation:
1. **Branding**: OPPO → MOONDROP throughout
2. **Package**: `moe.chenxy.oppopods` → `moe.chenxy.moondropods`
3. **Protocol**: OPPO RFCOMM → MOONDROP GAIA SPP
4. **RFCOMM UUID**: OPPO-specific → Standard SPP UUID `00001101-0000-1000-8000-00805F9B34FB`

## [S3] Naming Convention

| Item | Old | New |
|------|-----|-----|
| Root project | `OppoPods` | `MoondropPods` |
| Package | `moe.chenxy.oppopods` | `moe.chenxy.moondropods` |
| App name | `OPPOPods` | `MOONDROP-Pods` |
| Intent prefix | `chen.action.oppopods.*` | `chen.action.moondrop.*` |
| Log tag prefix | `OppoPods-` | `MoondropPods-` |
| Prefs name | `oppopods_settings` | `moondropods_settings` |
| Content provider | `moe.chenxy.oppopods.podimages` | `moe.chenxy.moondropods.podimages` |

## [S4] Protocol Layer

### MOONDROP GAIA Protocol

**Packet format:**
```
FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [Feature:1] [Cmd:1] [Payload...]
```

- Len = 1 + 1 + payload.size (feature + cmd + payload)
- Total packet = 8 + payload.size
- Response Feature = Request Feature | 0x01 (bit0)

**Features:**
| Feature | Purpose |
|---------|---------|
| 0x00 | Basic queries (firmware, serial, device ID, EQ, config, status) |
| 0x1E | Gain control |
| 0x20 | Codec (LDAC/LC3) |
| 0x40 | ANC control |

**ANC Modes:**
| Value | Mode |
|-------|------|
| 0x00 | Off |
| 0x02 | Transparency |
| 0x04 | Noise Cancel |
| 0x08 | Adaptive (unavailable) |
| 0x10 | Wind Noise (unavailable) |

**Gain Levels:**
| Value | Level |
|-------|-------|
| 0x00 | High |
| 0x01 | Medium |
| 0x02 | Low |

**RFCOMM UUID:** `00001101-0000-1000-8000-00805F9B34FB` (standard SPP)
- Standard Serial Port Profile UUID used by MOONDROP Pudding
- Confirmed via btsnoop抓包: SPP connection uses this UUID
- Note: BLE uses different UUIDs (OTA: `0000ae00-0000-1000-8000-00805f9b34fb`), but we use SPP for control

### Files to Replace
- `pods/Packets.kt` → Complete rewrite with MOONDROP GAIA protocol
- `pods/RfcommController.kt` → Rewrite packet handling, keep connection logic structure
- `pods/DeviceCapabilities.kt` → Update for MOONDROP capabilities
- `pods/GameModeImplementation.kt` → May need adjustment

### Files to Keep (with renaming)
- `pods/RfcommLog.kt` → Rename package only
- `pods/BatteryParser.kt` → Need to implement MOONDROP battery parsing (currently no response from device)

## [S5] Files to Modify

### Build Configuration
- `settings.gradle.kts` → rootProject.name = "MoondropPods"
- `app/build.gradle.kts` → namespace, applicationId
- `gradle/libs.versions.toml` → No changes needed

### Android Manifest
- `app/src/main/AndroidManifest.xml` → Package references, intent filters

### Resources
- `app/src/main/res/values/strings.xml` → All string resources
- `app/src/main/res/values-zh-rCN/strings.xml` → Chinese strings
- `app/src/main/res/values/themes.xml` → Theme names
- `app/src/main/resources/META-INF/xposed/java_init.list` → Entry class

### Source Code (Package Rename)
All 53 Kotlin files under `app/src/main/java/moe/chenxy/oppopods/` need package declaration changes.

### Key Classes to Rename
- `OppoPodsApp.kt` → `MoondropPodsApp.kt`
- `OppoPodsAction.kt` → `MoondropAction.kt`
- `OppoPodsPrefsKey.kt` → `MoondropPrefsKey.kt`
- `OppoPackets.kt` → `MoondropPackets.kt`

## [S6] What Stays the Same

- Xposed hook structure (HookEntry, HeadsetStateDispatcher, etc.)
- HyperOS integration (MiLink, MiBluetooth, Settings)
- UI structure (Compose pages, dialogs, components)
- Miuix UI components
- Navigation3
- Focus Island integration
- Device spoofing capability (can still spoof as Xiaomi earphones)

## [S7] Implementation Order

1. **Phase 1: Package Rename** ( mechanical, low risk )
   - Rename directory `moe/chenxy/oppopods` → `moe/chenxy/moondropods`
   - Update all package declarations and imports
   - Update build.gradle.kts namespace/applicationId
   - Update settings.gradle.kts rootProject.name
   - Update AndroidManifest.xml
   - Update META-INF/xposed/java_init.list
   - Update string resources
   - Verify build compiles

2. **Phase 2: Branding** ( string changes )
   - Update all "OPPO" references to "MOONDROP"
   - Update all "OppoPods" references to "MoondropPods"
   - Update intent actions
   - Update log tags
   - Update preferences keys

3. **Phase 3: Protocol** ( functional change )
   - Rewrite `Packets.kt` with MOONDROP GAIA protocol
   - Update `RfcommController.kt` for new packet format
   - Update UUID to standard SPP
   - Implement MOONDROP-specific parsers
   - Update `DeviceCapabilities.kt`

## [S8] Risk Assessment

- **Low risk**: Package rename, branding changes (mechanical)
- **Medium risk**: Protocol rewrite (requires understanding MOONDROP protocol deeply)
- **Unknown**: Battery reporting (MOONDROP may not support it), game mode (may not exist)

## [S9] Testing Strategy

1. Verify Gradle build succeeds after each phase
2. Test RFCOMM connection with actual MOONDROP Pudding device
3. Verify ANC control works
4. Verify Gain control works
5. Verify HyperOS integration (Focus Island, Settings)

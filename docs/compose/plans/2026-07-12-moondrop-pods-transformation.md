# MOONDROP-Pods Transformation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform OPPOPods Xposed module into MOONDROP-Pods, supporting MOONDROP Pudding TWS earphones on Xiaomi HyperOS.

**Architecture:** Keep the existing Xposed module structure, HyperOS integration, and Compose UI. Replace OPPO branding with MOONDROP, and replace the OPPO RFCOMM protocol with MOONDROP GAIA SPP protocol.

**Tech Stack:** Kotlin, Jetpack Compose, Miuix UI, Xposed API, Bluetooth SPP/RFCOMM

## Global Constraints

- Package: `moe.chenxy.moondropods`
- App name: `MOONDROP-Pods`
- RFCOMM UUID: `00001101-0000-1000-8000-00805F9B34FB` (standard SPP)
- MOONDROP GAIA protocol format: `FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [Feature:1] [Cmd:1] [Payload...]`
- Vendor ID: `0x001D` (29)
- ANC modes: 0x00=Off, 0x02=Transparency, 0x04=Noise Cancel
- Gain levels: 0x00=High, 0x01=Medium, 0x02=Low

---

## Phase 1: Package Rename

### Task 1.1: Create new package directory structure

**Covers:** [S3, S5]

**Files:**
- Create: `app/src/main/java/moe/chenxy/moondropods/` (entire directory tree)

**Interfaces:**
- Consumes: None
- Produces: New package directory structure

- [ ] **Step 1: Create new package directory**

```bash
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\config"
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\hook\milink"
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\pods"
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\ui\components"
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\ui\dialogs"
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\ui\pages"
mkdir -p "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\utils\miuiStrongToast\data"
```

- [ ] **Step 2: Copy all Kotlin files to new location**

```bash
# Copy all .kt files, preserving directory structure
xcopy "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\oppopods\*.kt" "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\" /S /Y
```

- [ ] **Step 3: Verify files copied**

Run: `dir "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\" /S /B`
Expected: List of all Kotlin files

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/
git commit -m "chore: create new package directory structure for moondropods"
```

---

### Task 1.2: Update package declarations and imports

**Covers:** [S3, S5]

**Files:**
- Modify: All 53 Kotlin files in `app/src/main/java/moe/chenxy/moondropods/`

**Interfaces:**
- Consumes: Task 1.1 (copied files)
- Produces: Files with correct package declarations

- [ ] **Step 1: Update package declarations in all files**

Use PowerShell to replace `package moe.chenxy.oppopods` with `package moe.chenxy.moondropods` in all .kt files:

```powershell
Get-ChildItem -Path "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods" -Filter "*.kt" -Recurse | ForEach-Object {
    (Get-Content $_.FullName -Raw) -replace 'package moe\.chenxy\.oppopods', 'package moe.chenxy.moondropods' | Set-Content $_.FullName -NoNewline
}
```

- [ ] **Step 2: Update import statements in all files**

```powershell
Get-ChildItem -Path "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods" -Filter "*.kt" -Recurse | ForEach-Object {
    (Get-Content $_.FullName -Raw) -replace 'import moe\.chenxy\.oppopods\.', 'import moe.chenxy.moondropods.' | Set-Content $_.FullName -NoNewline
}
```

- [ ] **Step 3: Verify no remaining oppopods references in package/import**

Run: `rg "moe\.chenxy\.oppopods" "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods"`
Expected: No matches

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/
git commit -m "refactor: update package declarations and imports to moondropods"
```

---

### Task 1.3: Update build configuration

**Covers:** [S3, S5]

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: None
- Produces: Build files pointing to new package

- [ ] **Step 1: Update settings.gradle.kts**

Change rootProject.name from "OppoPods" to "MoondropPods":

```kotlin
// settings.gradle.kts line 26
rootProject.name = "MoondropPods"
```

- [ ] **Step 2: Update app/build.gradle.kts namespace and applicationId**

```kotlin
// app/build.gradle.kts lines 18-19
namespace = "moe.chenxy.moondropods"
applicationId = "moe.chenxy.moondropods"
```

- [ ] **Step 3: Verify build configuration**

Run: `./gradlew tasks --all | head -20`
Expected: Build system recognizes new project name

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts app/build.gradle.kts
git commit -m "build: update namespace and applicationId to moondropods"
```

---

### Task 1.4: Update AndroidManifest.xml

**Covers:** [S3, S5]

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: Task 1.3 (new namespace)
- Produces: Manifest with correct package references

- [ ] **Step 1: Update AndroidManifest.xml**

Replace all `moe.chenxy.oppopods` with `moe.chenxy.moondropods`:

```xml
<!-- Line 13: android:name=".OppoPodsApp" → android:name=".MoondropPodsApp" -->
<!-- Line 57: android:authorities="moe.chenxy.oppopods.podimages" → android:authorities="moe.chenxy.moondropods.podimages" -->
```

- [ ] **Step 2: Verify manifest**

Run: `rg "oppopods" "D:\code\MOONDROP-Pods\app\src\main\AndroidManifest.xml"`
Expected: No matches

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "fix: update AndroidManifest.xml package references"
```

---

### Task 1.5: Update Xposed entry point

**Covers:** [S3, S5]

**Files:**
- Modify: `app/src/main/resources/META-INF/xposed/java_init.list`

**Interfaces:**
- Consumes: Task 1.2 (new package)
- Produces: Correct entry point class

- [ ] **Step 1: Update java_init.list**

Change line 1 from `moe.chenxy.oppopods.hook.HookEntry` to `moe.chenxy.moondropods.hook.HookEntry`

- [ ] **Step 2: Verify entry point**

Run: `cat "D:\code\MOONDROP-Pods\app\src\main\resources\META-INF\xposed\java_init.list"`
Expected: `moe.chenxy.moondropods.hook.HookEntry`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/resources/META-INF/xposed/java_init.list
git commit -m "fix: update Xposed entry point to moondropods package"
```

---

### Task 1.6: Update string resources

**Covers:** [S3, S5]

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Interfaces:**
- Consumes: None
- Produces: Updated string resources

- [ ] **Step 1: Update English strings.xml**

Replace:
- `OPPOPods` → `MOONDROP-Pods`
- `OPPO Earphones for HyperOS` → `MOONDROP Earphones for HyperOS`
- `About OppoPods` → `About MOONDROP-Pods`
- `Waiting for OPPO Earphones Connection...` → `Waiting for MOONDROP Earphones Connection...`

- [ ] **Step 2: Update Chinese strings.xml**

Replace:
- `OPPOPods` → `MOONDROP-Pods`
- `OPPO 耳机适配 HyperOS` → `MOONDROP 耳机适配 HyperOS`
- `关于 OppoPods` → `关于 MOONDROP-Pods`
- `等待 OPPO 耳机连接...` → `等待 MOONDROP 耳机连接...`

- [ ] **Step 3: Verify no remaining OPPO references**

Run: `rg "OPPO|OppoPods" "D:\code\MOONDROP-Pods\app\src\main\res\values"`
Expected: No matches

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml
git commit -m "i18n: update string resources to MOONDROP branding"
```

---

### Task 1.7: Update theme resources

**Covers:** [S3, S5]

**Files:**
- Modify: `app/src/main/res/values/themes.xml`

**Interfaces:**
- Consumes: None
- Produces: Updated theme names

- [ ] **Step 1: Update themes.xml**

Replace:
- `Theme.OppoPods` → `Theme.MoondropPods`
- `Theme.OppoPods.Popup` → `Theme.MoondropPods.Popup`

- [ ] **Step 2: Update AndroidManifest.xml theme references**

Update the theme references in AndroidManifest.xml to use new theme names.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/themes.xml app/src/main/AndroidManifest.xml
git commit -m "ui: update theme names to MoondropPods"
```

---

### Task 1.8: Remove old package directory

**Covers:** [S3]

**Files:**
- Delete: `app/src/main/java/moe/chenxy/oppopods/` (entire directory)

**Interfaces:**
- Consumes: Tasks 1.1-1.7 (all files moved and updated)
- Produces: Clean directory structure

- [ ] **Step 1: Verify new package is complete**

Run: `dir "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods" /S /B | find /c ".kt"`
Expected: 53 files

- [ ] **Step 2: Remove old package directory**

```bash
rmdir /S /Q "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\oppopods"
```

- [ ] **Step 3: Verify old directory removed**

Run: `dir "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\oppopods" 2>&1`
Expected: "The system cannot find the path specified."

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main/java/moe/chenxy/
git commit -m "chore: remove old oppopods package directory"
```

---

### Task 1.9: Verify build compiles

**Covers:** [S3, S5]

**Files:**
- None (verification only)

**Interfaces:**
- Consumes: Tasks 1.1-1.8
- Produces: Successful build

- [ ] **Step 1: Run Gradle build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Fix any compilation errors**

If build fails, fix import errors, missing references, etc.

- [ ] **Step 3: Commit fixes if any**

```bash
git add -A
git commit -m "fix: resolve compilation errors after package rename"
```

---

## Phase 2: Branding

### Task 2.1: Rename key classes

**Covers:** [S3]

**Files:**
- Rename: `OppoPodsApp.kt` → `MoondropPodsApp.kt`
- Rename: `utils/miuiStrongToast/data/OppoPodsAction.kt` → `MoondropAction.kt`
- Rename: `utils/miuiStrongToast/data/OppoPodsPrefsKey.kt` → `MoondropPrefsKey.kt`
- Rename: `pods/Packets.kt` (class OppoPackets → MoondropPackets)

**Interfaces:**
- Consumes: Phase 1 (package rename complete)
- Produces: Renamed classes

- [ ] **Step 1: Rename OppoPodsApp.kt to MoondropPodsApp.kt**

```bash
rename "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\OppoPodsApp.kt" "MoondropPodsApp.kt"
```

Update class name inside file: `class OppoPodsApp` → `class MoondropPodsApp`

- [ ] **Step 2: Rename OppoPodsAction.kt to MoondropAction.kt**

```bash
rename "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\utils\miuiStrongToast\data\OppoPodsAction.kt" "MoondropAction.kt"
```

Update class name: `object OppoPodsAction` → `object MoondropAction`

- [ ] **Step 3: Rename OppoPodsPrefsKey.kt to MoondropPrefsKey.kt**

```bash
rename "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\utils\miuiStrongToast\data\OppoPodsPrefsKey.kt" "MoondropPrefsKey.kt"
```

Update class name: `object OppoPodsPrefsKey` → `object MoondropPrefsKey`

- [ ] **Step 4: Update all references to renamed classes**

Use PowerShell to update all imports and references:

```powershell
Get-ChildItem -Path "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods" -Filter "*.kt" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace 'OppoPodsApp', 'MoondropPodsApp'
    $content = $content -replace 'OppoPodsAction', 'MoondropAction'
    $content = $content -replace 'OppoPodsPrefsKey', 'MoondropPrefsKey'
    Set-Content $_.FullName -Value $content -NoNewline
}
```

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/java/moe/chenxy/moondropods/
git commit -m "refactor: rename OppoPods classes to MoondropPods"
```

---

### Task 2.2: Update intent actions

**Covers:** [S3]

**Files:**
- Modify: `utils/miuiStrongToast/data/MoondropAction.kt`
- Modify: All files referencing intent actions

**Interfaces:**
- Consumes: Task 2.1 (renamed classes)
- Produces: Updated intent actions

- [ ] **Step 1: Update MoondropAction.kt constants**

Replace all `chen.action.oppopods.` with `chen.action.moondrop.`:

```kotlin
const val ACTION_PODS_UI_INIT = "chen.action.moondrop.ui_init"
const val ACTION_PODS_UI_CLOSED = "chen.action.moondrop.ui_closed"
// ... etc for all 40+ action constants
```

- [ ] **Step 2: Update AndroidManifest.xml intent filter**

Replace `chen.action.oppopods.show_pods_ui` with `chen.action.moondrop.show_pods_ui`

- [ ] **Step 3: Update all references in other files**

```powershell
Get-ChildItem -Path "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods" -Filter "*.kt" -Recurse | ForEach-Object {
    (Get-Content $_.FullName -Raw) -replace 'chen\.action\.oppopods\.', 'chen.action.moondrop.' | Set-Content $_.FullName -NoNewline
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: update intent actions from oppopods to moondrop"
```

---

### Task 2.3: Update log tags

**Covers:** [S3]

**Files:**
- Modify: All files with log tags

**Interfaces:**
- Consumes: None
- Produces: Updated log tags

- [ ] **Step 1: Update all log tag prefixes**

Replace `OppoPods-` with `MoondropPods-` in all TAG constants:

```powershell
Get-ChildItem -Path "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods" -Filter "*.kt" -Recurse | ForEach-Object {
    (Get-Content $_.FullName -Raw) -replace '"OppoPods-', '"MoondropPods-' | Set-Content $_.FullName -NoNewline
}
```

- [ ] **Step 2: Update standalone "OppoPods" log strings**

Replace remaining `"OppoPods"` with `"MoondropPods"` in log statements.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor: update log tags to MoondropPods"
```

---

### Task 2.4: Update preferences keys

**Covers:** [S3]

**Files:**
- Modify: `hook/HookEntry.kt`
- Modify: `config/ConfigManager.kt`

**Interfaces:**
- Consumes: None
- Produces: Updated preference keys

- [ ] **Step 1: Update preferences name in HookEntry.kt**

Change `"oppopods_settings"` to `"moondropods_settings"` in `getRemotePreferences()` call.

- [ ] **Step 2: Verify ConfigManager uses correct prefs**

Ensure ConfigManager reads from the correct preferences file.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/hook/HookEntry.kt
git commit -m "refactor: update preferences name to moondropods_settings"
```

---

### Task 2.5: Update content provider authority

**Covers:** [S3]

**Files:**
- Modify: `config/PodImageProvider.kt`
- Modify: `utils/PodImageLoader.kt`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- Consumes: None
- Produces: Updated content provider

- [ ] **Step 1: Update PodImageProvider.kt**

Change authority from `"moe.chenxy.oppopods.podimages"` to `"moe.chenxy.moondropods.podimages"`

- [ ] **Step 2: Update PodImageLoader.kt**

Change `MODULE_PACKAGE` from `"moe.chenxy.oppopods"` to `"moe.chenxy.moondropods"`

- [ ] **Step 3: Verify AndroidManifest.xml**

Ensure `android:authorities` is updated (should be done in Task 1.4).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/config/PodImageProvider.kt
git add app/src/main/java/moe/chenxy/moondropods/utils/PodImageLoader.kt
git commit -m "refactor: update content provider authority to moondropods"
```

---

### Task 2.6: Update UI strings and branding

**Covers:** [S3]

**Files:**
- Modify: `ui/pages/AboutPage.kt`
- Modify: `ui/MainUI.kt`
- Modify: Other UI files with "OPPO" references

**Interfaces:**
- Consumes: None
- Produces: Updated UI text

- [ ] **Step 1: Update AboutPage.kt**

Replace any "OPPO" or "OppoPods" references with "MOONDROP" or "MOONDROP-Pods"

- [ ] **Step 2: Update MainUI.kt**

Check for any branding references and update them.

- [ ] **Step 3: Search for remaining OPPO references**

Run: `rg "OPPO|OppoPods" "D:\code\MOONDROP-Pods\app\src\main\java\moe\chenxy\moondropods\ui"`
Expected: No matches (or only in comments explaining the original project)

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main/java/moe/chenxy/moondropods/ui/
git commit -m "ui: update branding references to MOONDROP"
```

---

## Phase 3: Protocol

### Task 3.1: Rewrite Packets.kt with MOONDROP GAIA protocol

**Covers:** [S4]

**Files:**
- Rewrite: `pods/Packets.kt`

**Interfaces:**
- Consumes: None
- Produces: MOONDROP GAIA protocol packet definitions

- [ ] **Step 1: Create new MoondropPackets.kt**

Replace the entire content of `pods/Packets.kt` with MOONDROP GAIA protocol:

```kotlin
package moe.chenxy.moondropods.pods

/**
 * MOONDROP GAIA V3 protocol packet definitions.
 *
 * Packet format (Big Endian for Len field):
 * Header(FF) + Header2(04) + Len(2B BE) + Seq(1B) + Vendor(1B) + Feature(1B) + Cmd(1B) + Payload
 *
 * Total packet length = 8 + payload.size
 * Response Feature = Request Feature | 0x01 (bit0)
 */

object MoondropPackets {
    const val HEADER_0: Byte = 0xFF.toByte()
    const val HEADER_1: Byte = 0x04
    const val VENDOR_ID: Int = 0x001D // 29 decimal

    /** Build a complete MOONDROP GAIA protocol packet. */
    fun buildPacket(
        feature: Int,
        cmd: Int,
        seq: Int = 0x00,
        payload: ByteArray = byteArrayOf()
    ): ByteArray {
        val payLen = payload.size
        // Len = 1 (feature) + 1 (cmd) + payload.size
        val len = 1 + 1 + payLen
        val packet = ByteArray(8 + payLen) // Header(2) + Len(2) + Seq(1) + Vendor(1) + Feature(1) + Cmd(1) + Payload
        packet[0] = HEADER_0
        packet[1] = HEADER_1
        packet[2] = ((len shr 8) and 0xFF).toByte() // Len high byte
        packet[3] = (len and 0xFF).toByte()          // Len low byte
        packet[4] = seq.toByte()
        packet[5] = VENDOR_ID.toByte()
        packet[6] = feature.toByte()
        packet[7] = cmd.toByte()
        payload.copyInto(packet, 8)
        return packet
    }
}

/** Feature IDs for MOONDROP GAIA protocol */
object GaiaFeature {
    const val BASE: Int = 0x00      // Basic queries
    const val ANC: Int = 0x40       // ANC control
    const val GAIN: Int = 0x1E      // Gain control
    const val CODEC: Int = 0x20     // Codec (LDAC/LC3)
    const val DEVICE_MGMT: Int = 0x1A // Device management
}

/** Command IDs for MOONDROP GAIA protocol */
object GaiaCmd {
    // Base commands (Feature=0x00)
    const val SUPPORTED_COMMANDS: Int = 0x01
    const val FIRMWARE_VERSION: Int = 0x05
    const val HEARTBEAT: Int = 0x07
    const val DEVICE_STATE: Int = 0x0D
    const val CONFIG_QUERY: Int = 0x0C
    const val SERIAL: Int = 0x14
    const val DEVICE_ID: Int = 0x15

    // ANC commands (Feature=0x40)
    const val ANC_QUERY: Int = 0x03
    const val ANC_SET: Int = 0x04
    const val ANC_AVAILABLE: Int = 0x29

    // Gain commands (Feature=0x1E)
    const val GAIN_QUERY: Int = 0x01
    const val GAIN_SET: Int = 0x02

    // Codec commands (Feature=0x20)
    const val LDAC_STATUS: Int = 0x05
    const val LC3_STATUS: Int = 0x01
}

/** ANC mode values */
object AncMode {
    const val OFF: Byte = 0x00
    const val TRANSPARENCY: Byte = 0x01
    const val NOISE_CANCEL: Byte = 0x02
    const val ADAPTIVE: Byte = 0x08
    const val ANTI_WIND: Byte = 0x10
}

/** Gain level values */
object GainLevel {
    const val HIGH: Byte = 0x00
    const val MEDIUM: Byte = 0x01
    const val LOW: Byte = 0x02
}

/** Noise control mode enum for UI */
enum class NoiseControlMode {
    OFF,
    TRANSPARENCY,
    NOISE_CANCELLATION,
    ADAPTIVE,
    ANTI_WIND
}

fun NoiseControlMode.isNoiseCancellation(): Boolean {
    return this == NoiseControlMode.NOISE_CANCELLATION
}

/** Pre-built packets for MOONDROP GAIA protocol */
object GaiaPackets {
    // ANC queries
    val ANC_QUERY: ByteArray = MoondropPackets.buildPacket(
        feature = GaiaFeature.ANC,
        cmd = GaiaCmd.ANC_QUERY
    )

    val ANC_AVAILABLE_QUERY: ByteArray = MoondropPackets.buildPacket(
        feature = GaiaFeature.ANC,
        cmd = GaiaCmd.ANC_AVAILABLE
    )

    // Gain queries
    val GAIN_QUERY: ByteArray = MoondropPackets.buildPacket(
        feature = GaiaFeature.GAIN,
        cmd = GaiaCmd.GAIN_QUERY
    )

    // Device state query
    val DEVICE_STATE_QUERY: ByteArray = MoondropPackets.buildPacket(
        feature = GaiaFeature.BASE,
        cmd = GaiaCmd.DEVICE_STATE,
        payload = byteArrayOf(0x07, 0x00, 0x00, 0x00, 0x04)
    )

    /** Build ANC set packet */
    fun ancSet(mode: Byte): ByteArray = MoondropPackets.buildPacket(
        feature = GaiaFeature.ANC,
        cmd = GaiaCmd.ANC_SET,
        payload = byteArrayOf(mode)
    )

    /** Build Gain set packet */
    fun gainSet(level: Byte): ByteArray = MoondropPackets.buildPacket(
        feature = GaiaFeature.GAIN,
        cmd = GaiaCmd.GAIN_SET,
        payload = byteArrayOf(level)
    )
}

/** Parser for MOONDROP GAIA response packets */
object GaiaResponseParser {
    /** Check if a packet is a response (Feature bit0 = 1) */
    fun isResponse(feature: Int): Boolean = (feature and 0x01) != 0

    /** Get base feature ID from response feature */
    fun baseFeatureId(feature: Int): Int = feature and 0x01.inv()

    /** Parse ANC response */
    fun parseAncResponse(payload: ByteArray): NoiseControlMode? {
        if (payload.isEmpty()) return null
        return when (payload[0]) {
            AncMode.OFF -> NoiseControlMode.OFF
            AncMode.TRANSPARENCY -> NoiseControlMode.TRANSPARENCY
            AncMode.NOISE_CANCEL -> NoiseControlMode.NOISE_CANCELLATION
            AncMode.ADAPTIVE -> NoiseControlMode.ADAPTIVE
            AncMode.ANTI_WIND -> NoiseControlMode.ANTI_WIND
            else -> null
        }
    }

    /** Parse Gain response */
    fun parseGainResponse(payload: ByteArray): GainLevel? {
        if (payload.isEmpty()) return null
        return when (payload[0]) {
            GainLevel.HIGH -> GainLevel.HIGH
            GainLevel.MEDIUM -> GainLevel.MEDIUM
            GainLevel.LOW -> GainLevel.LOW
            else -> null
        }
    }
}
```

- [ ] **Step 2: Verify file compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/pods/Packets.kt
git commit -m "feat: implement MOONDROP GAIA protocol packet definitions"
```

---

### Task 3.2: Update RfcommController.kt for new protocol

**Covers:** [S4]

**Files:**
- Modify: `pods/RfcommController.kt`

**Interfaces:**
- Consumes: Task 3.1 (new protocol definitions)
- Produces: Updated RFCOMM controller

- [ ] **Step 1: Update UUID**

Change `OPPO_RFCOMM_UUID` to standard SPP UUID:

```kotlin
private val MOONDROP_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
```

- [ ] **Step 2: Update handleOppoPacket to handleMoondropPacket**

Rename method and update packet handling to use MOONDROP GAIA protocol:

```kotlin
private fun handleMoondropPacket(packet: ByteArray) {
    Log.v(TAG, "Received: ${packet.toHexString(HexFormat.UpperCase)}")

    // Check if packet is valid GAIA format
    if (packet.size < 8 || packet[0] != GaiaConstants.HEADER_0 || packet[1] != GaiaConstants.HEADER_1) {
        Log.d(TAG, "Invalid GAIA packet header")
        return
    }

    // Parse header
    val len = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
    val seq = packet[4].toInt() and 0xFF
    val vendor = packet[5].toInt() and 0xFF
    val feature = packet[6].toInt() and 0xFF
    val cmd = packet[7].toInt() and 0xFF
    val payload = if (packet.size > 8) packet.copyOfRange(8, packet.size) else byteArrayOf()

    // Check if this is a response
    if (!GaiaResponseParser.isResponse(feature)) {
        Log.d(TAG, "Not a response packet, feature=$feature")
        return
    }

    val baseFeature = GaiaResponseParser.baseFeatureId(feature)

    when (baseFeature) {
        GaiaFeature.ANC -> handleAncResponse(cmd, payload)
        GaiaFeature.GAIN -> handleGainResponse(cmd, payload)
        // Add more handlers as needed
        else -> Log.d(TAG, "Unknown feature: $baseFeature")
    }
}
```

- [ ] **Step 3: Add response handlers**

```kotlin
private fun handleAncResponse(cmd: Int, payload: ByteArray) {
    when (cmd) {
        GaiaCmd.ANC_QUERY -> {
            val mode = GaiaResponseParser.parseAncResponse(payload)
            if (mode != null) {
                Log.d(TAG, "ANC mode: $mode")
                currentAncMode = mode
                changeUIAncStatus(mode)
            }
        }
        GaiaCmd.ANC_SET -> {
            Log.d(TAG, "ANC set confirmed")
        }
    }
}

private fun handleGainResponse(cmd: Int, payload: ByteArray) {
    when (cmd) {
        GaiaCmd.GAIN_QUERY -> {
            val level = GaiaResponseParser.parseGainResponse(payload)
            if (level != null) {
                Log.d(TAG, "Gain level: $level")
                currentGainLevel = level
                changeUIGainStatus(level)
            }
        }
        GaiaCmd.GAIN_SET -> {
            Log.d(TAG, "Gain set confirmed")
        }
    }
}
```

- [ ] **Step 4: Update sendPacketSafe to use new protocol**

Ensure packets are sent using MOONDROP GAIA format.

- [ ] **Step 5: Update connectRfcomm to send initial queries**

Replace OPPO-specific initialization with MOONDROP queries:

```kotlin
// After RFCOMM connected:
delay(300)
// Query ANC status
sendPacketSafe(GaiaPackets.ANC_QUERY, "anc query")
delay(50)
// Query Gain status
sendPacketSafe(GaiaPackets.GAIN_QUERY, "gain query")
delay(50)
// Query device state
sendPacketSafe(GaiaPackets.DEVICE_STATE_QUERY, "device state query")
```

- [ ] **Step 6: Update setANCMode to use new protocol**

```kotlin
fun setAncMode(mode: NoiseControlMode) {
    Log.d(TAG, "setAncMode: $mode")
    val gaiaMode = when (mode) {
        NoiseControlMode.OFF -> AncMode.OFF
        NoiseControlMode.TRANSPARENCY -> AncMode.TRANSPARENCY
        NoiseControlMode.NOISE_CANCELLATION -> AncMode.NOISE_CANCEL
        NoiseControlMode.ADAPTIVE -> AncMode.ADAPTIVE
        NoiseControlMode.ANTI_WIND -> AncMode.ANTI_WIND
    }
    currentAncMode = mode
    changeUIAncStatus(mode)
    CoroutineScope(Dispatchers.IO).launch {
        sendPacketSafe(GaiaPackets.ancSet(gaiaMode), "anc control")
    }
}
```

- [ ] **Step 7: Add gain control methods**

```kotlin
fun setGainLevel(level: GainLevel) {
    Log.d(TAG, "setGainLevel: $level")
    currentGainLevel = level
    changeUIGainStatus(level)
    CoroutineScope(Dispatchers.IO).launch {
        sendPacketSafe(GaiaPackets.gainSet(level.value), "gain control")
    }
}
```

- [ ] **Step 8: Remove OPPO-specific code**

Remove or update:
- `OPPO_RFCOMM_UUID` → `MOONDROP_SPP_UUID`
- OPPO-specific packet handling
- Game mode (if not supported by MOONDROP)

- [ ] **Step 9: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/pods/RfcommController.kt
git commit -m "feat: update RfcommController for MOONDROP GAIA protocol"
```

---

### Task 3.3: Update DeviceCapabilities.kt

**Covers:** [S4]

**Files:**
- Modify: `pods/DeviceCapabilities.kt`

**Interfaces:**
- Consumes: Task 3.1 (new protocol)
- Produces: Updated device capabilities

- [ ] **Step 1: Update device capabilities for MOONDROP**

Replace OPPO-specific capabilities with MOONDROP capabilities:

```kotlin
data class DeviceCapabilities(
    val ancSupported: Boolean = true,
    val gainSupported: Boolean = true,
    val adaptiveSupported: Boolean = false, // Not available yet
    val antiWindSupported: Boolean = false, // Not available yet
    val spatialAudioSupported: Boolean = false,
    val gameModeSupported: Boolean = false,
    val ldacSupported: Boolean = true,
    val lc3Supported: Boolean = true
)

fun detectDeviceCapabilities(deviceName: String): DeviceCapabilities {
    // MOONDROP Pudding supports ANC and Gain
    return DeviceCapabilities(
        ancSupported = true,
        gainSupported = true,
        adaptiveSupported = false,
        antiWindSupported = false,
        spatialAudioSupported = false,
        gameModeSupported = false,
        ldacSupported = true,
        lc3Supported = true
    )
}
```

- [ ] **Step 2: Update references in RfcommController.kt**

Ensure `currentCapabilities()` uses the new detection function.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/moe/chenxy/moondropods/pods/DeviceCapabilities.kt
git commit -m "feat: update DeviceCapabilities for MOONDROP Pudding"
```

---

### Task 3.4: Update UI for new capabilities

**Covers:** [S4]

**Files:**
- Modify: `ui/pages/PodDetailPage.kt`
- Modify: `ui/pages/EarphonesTabPage.kt`
- Modify: `ui/components/AncSwitch.kt`

**Interfaces:**
- Consumes: Task 3.2-3.3 (new protocol and capabilities)
- Produces: Updated UI

- [ ] **Step 1: Update PodDetailPage.kt**

Replace game mode controls with gain controls (if applicable).

- [ ] **Step 2: Update AncSwitch.kt**

Ensure ANC modes match MOONDROP capabilities (Off, Transparency, Noise Cancel).

- [ ] **Step 3: Update EarphonesTabPage.kt**

Update status display for MOONDROP features.

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main/java/moe/chenxy/moondropods/ui/
git commit -m "ui: update UI for MOONDROP device capabilities"
```

---

### Task 3.5: Remove OPPO-specific files

**Covers:** [S4]

**Files:**
- Delete: `pods/GameModeImplementation.kt` (if not needed)
- Delete: Other OPPO-specific files

**Interfaces:**
- Consumes: Tasks 3.1-3.4
- Produces: Clean codebase

- [ ] **Step 1: Identify OPPO-specific files**

Check which files are OPPO-specific and not needed for MOONDROP.

- [ ] **Step 2: Remove or update files**

Remove files that are OPPO-specific and not applicable to MOONDROP.

- [ ] **Step 3: Update references**

Ensure no dangling references to removed files.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: remove OPPO-specific files"
```

---

### Task 3.6: Final build verification

**Covers:** [S4, S5]

**Files:**
- None (verification only)

**Interfaces:**
- Consumes: All previous tasks
- Produces: Working build

- [ ] **Step 1: Run full Gradle build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify APK is generated**

Run: `dir "D:\code\MOONDROP-Pods\app\build\outputs\apk\debug\"`
Expected: APK file exists

- [ ] **Step 3: Test installation (optional)**

Install APK on device and verify it loads in LSPosed.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete MOONDROP-Pods transformation"
```

---

## Summary

This plan transforms OPPOPods into MOONDROP-Pods through three phases:

1. **Phase 1 (Tasks 1.1-1.9):** Package rename - mechanical, low risk
2. **Phase 2 (Tasks 2.1-2.6):** Branding - string changes
3. **Phase 3 (Tasks 3.1-3.6):** Protocol - functional change to MOONDROP GAIA

Total tasks: 21
Estimated time: 4-6 hours for a skilled developer

Key deliverables:
- New package: `moe.chenxy.moondropods`
- MOONDROP GAIA protocol implementation
- Standard SPP UUID support
- Updated UI for MOONDROP capabilities

package moe.chenxy.moondropods.pods

data class DeviceCapabilities(
    val ancSupported: Boolean = true,
    val gainSupported: Boolean = true,
    // MOONDROP Pudding 实测支持自适应/抗风噪 (btsnoop 确认 0x01/0x03)
    val adaptiveSupported: Boolean = true,
    val antiWindSupported: Boolean = true,
    val spatialAudioSupported: Boolean = false,
    val spatialSoundSwitchSupported: Boolean = false,
    val gameModeSupported: Boolean = false,
    val ldacSupported: Boolean = true,
    val lc3Supported: Boolean = true
)

fun detectDeviceCapabilities(
    deviceName: String,
    adaptiveOverride: Int = 0,
    spatialAudioOverride: Int = 0,
    spatialSoundSwitchOverride: Int = 0,
    ancImplementationOverride: Int = 0,
): DeviceCapabilities {
    // Override values: 0 = auto, 1 = force enabled, 2 = force disabled
    fun applyOverride(autoValue: Boolean, override: Int): Boolean = when (override) {
        1 -> true
        2 -> false
        else -> autoValue
    }

    return DeviceCapabilities(
        ancSupported = true,
        gainSupported = true,
        adaptiveSupported = applyOverride(true, adaptiveOverride),
        antiWindSupported = true,
        spatialAudioSupported = applyOverride(false, spatialAudioOverride),
        spatialSoundSwitchSupported = applyOverride(false, spatialSoundSwitchOverride),
        gameModeSupported = false,
        ldacSupported = true,
        lc3Supported = true
    )
}

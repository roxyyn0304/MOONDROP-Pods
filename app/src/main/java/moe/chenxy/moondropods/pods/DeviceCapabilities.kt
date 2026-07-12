package moe.chenxy.moondropods.pods

data class DeviceCapabilities(
    val ancSupported: Boolean = true,
    val gainSupported: Boolean = true,
    val adaptiveSupported: Boolean = false,
    val antiWindSupported: Boolean = false,
    val spatialAudioSupported: Boolean = false,
    val gameModeSupported: Boolean = false,
    val ldacSupported: Boolean = true,
    val lc3Supported: Boolean = true
)

fun detectDeviceCapabilities(deviceName: String): DeviceCapabilities {
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

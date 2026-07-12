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

// ponytail: kept as stub for UI compatibility - MOONDROP doesn't support wear detection
enum class WearState(val value: Int) {
    DISCONNECTED(0x00),
    IN_CASE(0x04),
    REMOVED(0x05),
    WEARING(0x07);
    companion object {
        fun fromValue(value: Int): WearState? = entries.firstOrNull { it.value == value }
    }
}

data class WearStatus(
    val left: WearState? = null,
    val right: WearState? = null,
    val case: WearState? = null
)

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
    fun parseGainResponse(payload: ByteArray): Byte? {
        if (payload.isEmpty()) return null
        return when (payload[0]) {
            GainLevel.HIGH -> GainLevel.HIGH
            GainLevel.MEDIUM -> GainLevel.MEDIUM
            GainLevel.LOW -> GainLevel.LOW
            else -> null
        }
    }
}

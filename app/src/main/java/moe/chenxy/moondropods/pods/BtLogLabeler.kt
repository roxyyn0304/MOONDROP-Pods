package moe.chenxy.moondropods.pods

/**
 * 为 MOONDROP GAIA V3 (FF04) 蓝牙协议包生成可读中文标签。
 *
 * 包格式（与 OPPO 的 AA 帧不同）:
 *   FF 04 [Len:2B BE] [Seq] [Vendor=0x1D] [Feature] [Cmd] [Payload]
 *   packet[0..1] = 帧头, packet[2..3] = Len(大端), packet[4] = Seq,
 *   packet[5] = Vendor, packet[6] = Feature, packet[7] = Cmd, packet[8..] = Payload
 *
 * 响应包 Feature bit0 = 1（请求 Feature | 0x01），例如 0x00 -> 0x01、0x40 -> 0x41。
 */
object BtLogLabeler {

    // ---- Feature 常量 ----
    private const val FEATURE_BASE = 0x00        // 基础查询
    private const val FEATURE_ANC = 0x40         // 降噪控制
    private const val FEATURE_GAIN = 0x1E        // 增益控制
    private const val FEATURE_CODEC = 0x20       // 编解码器 (LC3/LDAC)
    private const val FEATURE_DEVICE_MGMT = 0x1A // 设备管理（电量查询）
    private const val FEATURE_DEVICE_INFO = 0x28 // 设备信息

    /** (feature, cmd) -> 中文标签 */
    private val LABELS: Map<Pair<Int, Int>, String> = mapOf(
        // BASE (0x00)
        (FEATURE_BASE to 0x01) to "支持命令列表",
        (FEATURE_BASE to 0x05) to "查询固件版本",
        (FEATURE_BASE to 0x0C) to "配置查询",
        (FEATURE_BASE to 0x0D) to "查询设备状态",
        (FEATURE_BASE to 0x14) to "查询序列号",
        (FEATURE_BASE to 0x15) to "查询设备ID",
        // ANC (0x40)
        (FEATURE_ANC to 0x03) to "查询降噪状态",
        (FEATURE_ANC to 0x04) to "设置降噪模式",
        (FEATURE_ANC to 0x29) to "查询可用降噪模式",
        // GAIN (0x1E)
        (FEATURE_GAIN to 0x01) to "查询增益",
        (FEATURE_GAIN to 0x02) to "设置增益",
        // CODEC (0x20)
        (FEATURE_CODEC to 0x01) to "查询LC3状态",
        (FEATURE_CODEC to 0x05) to "查询LDAC状态",
        (FEATURE_CODEC to 0x06) to "设置编解码器",
        // DEVICE_MGMT (0x1A)
        (FEATURE_DEVICE_MGMT to 0x01) to "查询电量",
        // DEVICE_INFO (0x28)
        (FEATURE_DEVICE_INFO to 0x01) to "查询设备信息",
        (FEATURE_DEVICE_INFO to 0x03) to "查询设备子类型",
        (FEATURE_DEVICE_INFO to 0x05) to "查询连接设备名",
    )

    /**
     * 为发送的包生成标签。
     * 返回 null 表示未识别的 feature/cmd。
     */
    fun labelSend(packet: ByteArray): String? {
        val (feature, cmd) = extractFeatureCmd(packet) ?: return null
        return LABELS[feature to cmd]
    }

    /**
     * 为接收的包生成标签。
     * 响应包 Feature bit0 = 1，先还原为请求 Feature 再查表，前缀"响应："。
     * 返回 null 表示未识别的 feature/cmd。
     */
    fun labelRecv(packet: ByteArray): String? {
        val (feature, cmd) = extractFeatureCmd(packet) ?: return null
        val baseFeature = feature and 0xFE
        val label = LABELS[baseFeature to cmd] ?: return null
        return "响应：$label"
    }

    /** 从 FF04 包提取 (feature, cmd)，帧头非法或长度不足时返回 null。 */
    private fun extractFeatureCmd(packet: ByteArray): Pair<Int, Int>? {
        if (packet.size < 8) return null
        if (packet[0] != 0xFF.toByte() || packet[1] != 0x04.toByte()) return null
        val feature = packet[6].toInt() and 0xFF
        val cmd = packet[7].toInt() and 0xFF
        return feature to cmd
    }
}

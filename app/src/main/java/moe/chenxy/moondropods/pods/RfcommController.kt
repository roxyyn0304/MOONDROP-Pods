package moe.chenxy.moondropods.pods

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRoute2Info
import android.media.MediaRouter2
import android.media.RouteDiscoveryPreference
import android.os.SystemClock
import moe.chenxy.moondropods.hook.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.chenxy.moondropods.BuildConfig
import moe.chenxy.moondropods.config.ConfigManager
import moe.chenxy.moondropods.utils.MediaControl
import moe.chenxy.moondropods.utils.SystemApisUtils
import moe.chenxy.moondropods.utils.SystemApisUtils.setIconVisibility
import moe.chenxy.moondropods.utils.miuiStrongToast.MiuiStrongToastUtil
import moe.chenxy.moondropods.utils.miuiStrongToast.MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt
import moe.chenxy.moondropods.utils.miuiStrongToast.data.BatteryParams
import moe.chenxy.moondropods.utils.miuiStrongToast.data.MoondropAction
import moe.chenxy.moondropods.utils.miuiStrongToast.data.PodParams
import java.io.IOException
import java.io.InputStream
import android.content.SharedPreferences
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("MissingPermission", "StaticFieldLeak")
object RfcommController {
    private const val TAG = "MoondropPods-RfcommController"
    private const val AUTO_RECONNECT_DELAY_MS = 120_000L
    private const val APP_UI_ACTIVE_TIMEOUT_MS = 75_000L

    // Basic Objects
    private var socket: BluetoothSocket? = null
    private var mContext: Context? = null
    lateinit var mDevice: BluetoothDevice
    private lateinit var mPrefs: SharedPreferences

    private var scanToken: MediaRouter2.ScanToken? = null
    var routes: List<MediaRoute2Info> = listOf()
    private lateinit var mediaRouter: MediaRouter2

    // Status
    private var mShowedConnectedToast = false
    private var isConnected = false
    private var lastTempBatt = 0
    lateinit var currentBatteryParams: BatteryParams
    private var currentAncMode: NoiseControlMode = NoiseControlMode.OFF
    private var currentGainLevel: Byte = GainLevel.HIGH
    private var lastKnownCaseBattery: Int = 0
    private var lastKnownCaseCharging: Boolean = false
    private var cachedDeviceName: String = ""
    private var receiverRegistered = false
    private var routeScanStarted = false
    private var appUiActive = false
    private var appUiActiveUntilMs = 0L

    data class StatusSnapshot(
        val battery: BatteryParams?,
        val anc: Int,
        val transparencyVocalEnhancement: Boolean,
        val address: String?,
        val deviceName: String?,
        val connected: Boolean,
        val connecting: Boolean,
        val reconnectPending: Boolean,
    )

    // RFCOMM jobs
    private var connectionJob: kotlinx.coroutines.Job? = null
    private var reconnectJob: kotlinx.coroutines.Job? = null
    private var readerJob: kotlinx.coroutines.Job? = null
    private val reconnectAttempts = AtomicInteger(0)
    private var reconnectPending = false
    private val MOONDROP_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            handleUIEvent(p1!!)
        }
    }

    private fun ancModeToInt(mode: NoiseControlMode): Int = when (mode) {
        NoiseControlMode.OFF -> 1
        NoiseControlMode.NOISE_CANCELLATION -> 2
        NoiseControlMode.TRANSPARENCY -> 3
        NoiseControlMode.ADAPTIVE -> 4
        NoiseControlMode.ANTI_WIND -> 5
    }

    private fun intToAncMode(status: Int): NoiseControlMode? = when (status) {
        1 -> NoiseControlMode.OFF
        2 -> NoiseControlMode.NOISE_CANCELLATION
        3 -> NoiseControlMode.TRANSPARENCY
        4 -> NoiseControlMode.ADAPTIVE
        5 -> NoiseControlMode.ANTI_WIND
        else -> null
    }

    private fun changeUIAncStatus(mode: NoiseControlMode) {
        val status = ancModeToInt(mode)
        sendAppStatusBroadcast(MoondropAction.ACTION_PODS_ANC_CHANGED) {
            if (::mDevice.isInitialized) this.putExtra("address", mDevice.address)
            this.putExtra("status", status)
        }
        sendExternalPodsStatusBroadcast(MoondropAction.ACTION_PODS_ANC_CHANGED) {
            putExtra("status", status)
        }
    }

    private fun changeUIBatteryStatus(status: BatteryParams) {
        sendAppStatusBroadcast(MoondropAction.ACTION_PODS_BATTERY_CHANGED) {
            if (::mDevice.isInitialized) this.putExtra("address", mDevice.address)
            this.putExtra("status", status)
            putBatteryExtras(status)
        }
        sendExternalPodsStatusBroadcast(MoondropAction.ACTION_PODS_BATTERY_CHANGED) {
            putExtra("status", status)
            putBatteryExtras(status)
        }
    }

    private fun changeUIGainStatus(level: Byte) {
        Log.d(TAG, "Gain status changed: $level")
    }

    fun handleUIEvent(intent: Intent) {
        when (intent.action) {
            MoondropAction.ACTION_PODS_UI_INIT -> {
                markAppUiActive()
                Log.i(TAG, "UI Init")
                changeUIConnectionState(currentConnectionState())
                if (::currentBatteryParams.isInitialized)
                    changeUIBatteryStatus(currentBatteryParams)
                changeUIAncStatus(currentAncMode)
                if (::mDevice.isInitialized && isConnected) {
                    sendAppStatusBroadcast(MoondropAction.ACTION_PODS_CONNECTED) {
                        this.putExtra("address", mDevice.address)
                        this.putExtra("device_name", mDevice.name ?: cachedDeviceName)
                    }
                    sendExternalPodsStatusBroadcast(MoondropAction.ACTION_PODS_CONNECTED) {
                        putExtra("device_name", mDevice.name ?: cachedDeviceName)
                    }
                }
            }
            MoondropAction.ACTION_PODS_UI_CLOSED -> {
                appUiActive = false
                appUiActiveUntilMs = 0L
                Log.i(TAG, "UI Closed")
            }
            MoondropAction.ACTION_ANC_SELECT -> {
                val status = intent.getIntExtra("status", 0)
                val mode = intToAncMode(status)
                if (mode != null) setAncMode(mode)
            }
            MoondropAction.ACTION_REFRESH_STATUS -> {
                queryStatus(immediateReconnect = true)
            }
            MoondropAction.ACTION_CONFIG_CHANGED -> {
                ConfigManager.refreshFromPrefs(mPrefs)
                Log.d(TAG, "Config synced")
            }
            MoondropAction.ACTION_RFCOMM_LOG_CONNECT -> {
                if (!RfcommLog.isEnabled()) {
                    RfcommLog.setEnabled(true, mContext)
                }
            }
            MoondropAction.ACTION_RFCOMM_LOG_DISCONNECT -> {
                RfcommLog.setEnabled(false)
            }
            MoondropAction.ACTION_RFCOMM_LOG_CLEAR -> {
                RfcommLog.clear()
            }
            MoondropAction.ACTION_RFCOMM_DEBUG_SEND -> {
                val hex = intent.getStringExtra("hex").orEmpty()
                sendDebugHex(hex)
            }
        }
    }

    fun currentStatusSnapshot(): StatusSnapshot {
        return StatusSnapshot(
            battery = if (::currentBatteryParams.isInitialized) currentBatteryParams else null,
            anc = ancModeToInt(currentAncMode),
            transparencyVocalEnhancement = false,
            address = if (::mDevice.isInitialized) mDevice.address else null,
            deviceName = if (::mDevice.isInitialized) mDevice.name ?: cachedDeviceName else cachedDeviceName.takeIf { it.isNotEmpty() },
            connected = isConnected && socket != null,
            connecting = connectionJob?.isActive == true,
            reconnectPending = reconnectPending,
        )
    }

    private fun currentConnectionState(): String = when {
        isConnected && socket != null && ::currentBatteryParams.isInitialized -> "connected"
        connectionJob?.isActive == true || reconnectPending -> "connecting"
        isConnected && socket != null -> "connecting"
        else -> "disconnected"
    }

    private fun changeUIConnectionState(state: String) {
        sendAppStatusBroadcast(MoondropAction.ACTION_PODS_CONNECTION_STATE_CHANGED) {
            if (::mDevice.isInitialized) {
                putExtra("address", mDevice.address)
                putExtra("device_name", mDevice.name ?: cachedDeviceName)
            }
            putExtra("state", state)
        }
    }

    private fun sendAppStatusBroadcast(action: String, fill: Intent.() -> Unit = {}) {
        val ctx = mContext ?: return
        if (!isAppUiActive()) return
        Intent(action).apply {
            fill()
            this.`package` = BuildConfig.APPLICATION_ID
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            ctx.sendBroadcast(this)
        }
    }

    private fun isAppUiActive(): Boolean {
        if (!appUiActive) return false
        if (SystemClock.elapsedRealtime() <= appUiActiveUntilMs) return true
        appUiActive = false
        appUiActiveUntilMs = 0L
        Log.d(TAG, "app UI active timeout, stop app status broadcasts")
        return false
    }

    private fun markAppUiActive() {
        appUiActive = true
        appUiActiveUntilMs = SystemClock.elapsedRealtime() + APP_UI_ACTIVE_TIMEOUT_MS
    }

    fun miuiRefreshPayload(battery: BatteryParams?, anc: Int, transparencyVocalEnhancement: Boolean): String {
        val values = MutableList(16) { "" }
        values[0] = miuiBatteryValue(battery?.left)
        values[1] = miuiBatteryValue(battery?.right)
        values[2] = miuiBatteryValue(battery?.case)
        values[7] = miuiAncLevel(anc, transparencyVocalEnhancement)
        values[8] = "true"
        values[11] = "00"
        values[13] = "00"
        values[14] = "00"
        return values.joinToString(",")
    }

    private fun miuiBatteryValue(params: PodParams?): String {
        if (params?.isConnected != true) return "255"
        val value = params.battery.coerceIn(0, 100)
        return (if (params.isCharging) value or 128 else value).toString()
    }

    private fun miuiAncLevel(anc: Int, transparencyVocalEnhancement: Boolean): String {
        return when (anc) {
            2 -> "0102"
            3 -> if (transparencyVocalEnhancement) "0201" else "0200"
            4 -> "0103"
            5 -> "0100"
            else -> "0000"
        }
    }

    private val routeCallback = object : MediaRouter2.RouteCallback() {
        override fun onRoutesUpdated(routes: List<MediaRoute2Info>) {
            Log.v(TAG, "routes updated: $routes")
            this@RfcommController.routes = routes
        }
    }

    private fun startRoutesScan() {
        if (routeScanStarted) return
        val executor = Executor { p0 ->
            CoroutineScope(Dispatchers.IO).launch { p0?.run() }
        }
        val preferredFeature = listOf(MediaRoute2Info.FEATURE_LIVE_AUDIO, MediaRoute2Info.FEATURE_LIVE_VIDEO)
        mediaRouter.registerRouteCallback(executor, routeCallback, RouteDiscoveryPreference.Builder(preferredFeature, true).build())
        scanToken = mediaRouter.requestScan(MediaRouter2.ScanRequest.Builder().build())
        routeScanStarted = true
    }

    private fun stopRoutesScan() {
        scanToken?.let { mediaRouter.cancelScanRequest(it) }
        if (routeScanStarted) {
            mediaRouter.unregisterRouteCallback(routeCallback)
            routeScanStarted = false
        }
    }

    private fun createRfcommSocket(device: BluetoothDevice): BluetoothSocket {
        return device.createRfcommSocketToServiceRecord(MOONDROP_SPP_UUID)
    }

    fun connectPod(context: Context, device: BluetoothDevice, prefs: SharedPreferences, appRequested: Boolean = false) {
        connectionJob?.cancel()
        reconnectJob?.cancel()
        readerJob?.cancel()
        closeSocketOnly()
        mContext = context
        mDevice = device
        mPrefs = prefs
        cachedDeviceName = device.name ?: ""
        if (appRequested) {
            markAppUiActive()
        }
        ConfigManager.refreshFromPrefs(mPrefs)
        Log.d(TAG, "RFCOMM UUID initial: $MOONDROP_SPP_UUID")

        if (!receiverRegistered) {
            context.registerReceiver(broadcastReceiver, IntentFilter().apply {
                this.addAction(MoondropAction.ACTION_ANC_SELECT)
                this.addAction(MoondropAction.ACTION_PODS_UI_INIT)
                this.addAction(MoondropAction.ACTION_PODS_UI_CLOSED)
                this.addAction(MoondropAction.ACTION_REFRESH_STATUS)
                this.addAction(MoondropAction.ACTION_CONFIG_CHANGED)
                this.addAction(MoondropAction.ACTION_RFCOMM_LOG_CONNECT)
                this.addAction(MoondropAction.ACTION_RFCOMM_LOG_DISCONNECT)
                this.addAction(MoondropAction.ACTION_RFCOMM_LOG_CLEAR)
                this.addAction(MoondropAction.ACTION_RFCOMM_DEBUG_SEND)
            }, Context.RECEIVER_EXPORTED)
            receiverRegistered = true
        }

        MediaControl.mContext = mContext
        mediaRouter = MediaRouter2.getInstance(mContext!!)
        startRoutesScan()

        isConnected = true
        changeUIConnectionState("connecting")

        connectRfcomm(initialDelayMs = 500L)
    }

    private fun sendExternalPodsStatusBroadcast(action: String, fill: Intent.() -> Unit = {}) {
        val ctx = mContext ?: return
        listOf("com.milink.service", "com.xiaomi.bluetooth", "com.android.settings").forEach { targetPackage ->
            Intent(action).apply {
                if (::mDevice.isInitialized) {
                    putExtra("address", mDevice.address)
                    putExtra("device_name", mDevice.name ?: cachedDeviceName)
                }
                fill()
                setPackage(targetPackage)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                ctx.sendBroadcast(this)
            }
        }
    }

    private fun Intent.putBatteryExtras(status: BatteryParams) {
        putExtra("left_battery", status.left?.battery ?: 0)
        putExtra("left_charging", status.left?.isCharging == true)
        putExtra("left_connected", status.left?.isConnected == true)
        putExtra("right_battery", status.right?.battery ?: 0)
        putExtra("right_charging", status.right?.isCharging == true)
        putExtra("right_connected", status.right?.isConnected == true)
        putExtra("case_battery", status.case?.battery ?: 0)
        putExtra("case_charging", status.case?.isCharging == true)
        putExtra("case_connected", status.case?.isConnected == true)
    }

    private fun connectRfcomm(initialDelayMs: Long = 0L) {
        connectionJob?.cancel()
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            if (initialDelayMs > 0) delay(initialDelayMs)
            if (!isConnected || !::mDevice.isInitialized) return@launch
            closeSocketOnly()
            try {
                val newSocket = createRfcommSocket(mDevice)
                newSocket.connect()
                socket = newSocket
                reconnectAttempts.set(0)
                reconnectPending = false
                Log.d(TAG, "RFCOMM connected! uuid=$MOONDROP_SPP_UUID")
                RfcommLog.i(mContext, TAG, "connected uuid=$MOONDROP_SPP_UUID")
                changeUIConnectionState("connecting")

                startPacketReader(newSocket.inputStream)

                delay(300)
                // Query ANC status
                sendPacketSafe(GaiaPackets.ANC_QUERY, "anc query")
                delay(50)
                // Query Gain status
                sendPacketSafe(GaiaPackets.GAIN_QUERY, "gain query")
                delay(50)
                // Query device state
                sendPacketSafe(GaiaPackets.DEVICE_STATE_QUERY, "device state query")
            } catch (e: IOException) {
                Log.e(TAG, "RFCOMM connect failed", e)
                changeUIConnectionState("error")
                scheduleReconnect("connect failed")
            }
        }
    }

    private fun scheduleReconnect(reason: String, immediate: Boolean = false) {
        if (!isConnected || !::mDevice.isInitialized || mContext == null) return
        RfcommLog.w(mContext, TAG, "schedule reconnect reason=$reason immediate=$immediate")
        closeSocketOnly()
        reconnectPending = true
        if (immediate) {
            if (connectionJob?.isActive == true) {
                Log.d(TAG, "immediate RFCOMM reconnect skipped: connecting reason=$reason")
                return
            }
            Log.d(TAG, "immediate RFCOMM reconnect reason=$reason")
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectPending = false
            connectRfcomm()
            return
        }
        if (reconnectJob?.isActive == true) {
            Log.d(TAG, "RFCOMM reconnect already scheduled reason=$reason")
            return
        }
        val attempt = reconnectAttempts.incrementAndGet()
        Log.d(TAG, "schedule RFCOMM reconnect reason=$reason attempt=$attempt delay=${AUTO_RECONNECT_DELAY_MS}ms")
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            delay(AUTO_RECONNECT_DELAY_MS)
            reconnectJob = null
            reconnectPending = false
            connectRfcomm()
        }
    }

    private fun reconnectNowForRequest(reason: String) {
        if (socket != null && !reconnectPending) return
        scheduleReconnect(reason, immediate = true)
    }

    private fun closeSocketOnly() {
        readerJob?.cancel()
        readerJob = null
        try {
            socket?.close()
        } catch (_: IOException) {}
        socket = null
    }

    private fun startPacketReader(inputStream: InputStream) {
        readerJob?.cancel()
        readerJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            try {
                while (isConnected) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val packet = buffer.copyOfRange(0, bytesRead)
                        RfcommLog.d(mContext, "RFCOMM/RX", packet.toHexString(HexFormat.UpperCase))
                        handleMoondropPacket(packet)
                    } else if (bytesRead == -1) {
                        Log.d(TAG, "RFCOMM stream ended")
                        RfcommLog.w(mContext, TAG, "stream ended")
                        scheduleReconnect("stream ended")
                        break
                    }
                }
            } catch (e: IOException) {
                if (isConnected) {
                    Log.e(TAG, "RFCOMM read error", e)
                    RfcommLog.e(mContext, TAG, "read error: ${e.message.orEmpty()}")
                    scheduleReconnect("read error")
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun handleMoondropPacket(packet: ByteArray) {
        Log.v(TAG, "Received: ${packet.toHexString(HexFormat.UpperCase)}")

        // Check if packet is valid GAIA format
        if (packet.size < 8 || packet[0] != MoondropPackets.HEADER_0 || packet[1] != MoondropPackets.HEADER_1) {
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

    fun disconnectedPod(context: Context, device: BluetoothDevice) {
        isConnected = false
        connectionJob?.cancel()
        reconnectJob?.cancel()
        readerJob?.cancel()
        reconnectAttempts.set(0)
        reconnectPending = false

        closeSocketOnly()

        mContext?.let {
            stopRoutesScan()
            cancelPodsNotificationByMiuiBt(context, device)
            sendAppStatusBroadcast(MoondropAction.ACTION_PODS_DISCONNECTED) {
                putExtra("address", device.address)
            }
            if (receiverRegistered) {
                it.unregisterReceiver(broadcastReceiver)
                receiverRegistered = false
            }
        }

        mShowedConnectedToast = false
        currentAncMode = NoiseControlMode.OFF
        currentGainLevel = GainLevel.HIGH
        lastKnownCaseBattery = 0
        lastKnownCaseCharging = false
        changeUIConnectionState("disconnected")
        cachedDeviceName = ""
        mContext = null
        MediaControl.mContext = null
    }

    private fun sendPacketSafe(packet: ByteArray, requestReason: String? = null) {
        if (requestReason != null) reconnectNowForRequest(requestReason)
        try {
            val currentSocket = socket ?: run {
                RfcommLog.w(mContext, "RFCOMM/TX", "socket null: ${packet.toHexString(HexFormat.UpperCase)}")
                scheduleReconnect("socket null before send", immediate = requestReason != null)
                return
            }
            RfcommLog.d(mContext, "RFCOMM/TX", packet.toHexString(HexFormat.UpperCase))
            currentSocket.outputStream.write(packet)
            currentSocket.outputStream.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Send packet failed", e)
            RfcommLog.e(mContext, TAG, "send failed: ${e.message.orEmpty()}")
            scheduleReconnect("send error", immediate = requestReason != null)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun sendDebugHex(hex: String) {
        val normalized = hex.filterNot { it.isWhitespace() || it == ':' || it == '-' }
        if (normalized.isEmpty() || normalized.length % 2 != 0 || !normalized.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            RfcommLog.e(mContext, "RFCOMM/DEBUG", "invalid HEX: $hex")
            return
        }
        val packet = normalized.hexToByteArray()
        RfcommLog.i(mContext, "RFCOMM/DEBUG", "send ${packet.size} bytes")
        sendPacketSafe(packet, "rfcomm debug send")
    }

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

    fun setGainLevel(level: Byte) {
        Log.d(TAG, "setGainLevel: $level")
        currentGainLevel = level
        changeUIGainStatus(level)
        CoroutineScope(Dispatchers.IO).launch {
            sendPacketSafe(GaiaPackets.gainSet(level), "gain control")
        }
    }

    fun queryStatus(immediateReconnect: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            val reason = if (immediateReconnect) "status query" else null
            sendPacketSafe(GaiaPackets.ANC_QUERY, reason)
            delay(50)
            sendPacketSafe(GaiaPackets.GAIN_QUERY, reason)
            delay(50)
            sendPacketSafe(GaiaPackets.DEVICE_STATE_QUERY, reason)
        }
    }

    fun disconnectAudio(context: Context, device: BluetoothDevice?) {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        MediaControl.sendPause()

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val method = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        method.invoke(proxy, device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }
            }
            override fun onServiceDisconnected(profile: Int) { }
        }, BluetoothProfile.HEADSET)

        CoroutineScope(Dispatchers.Default).launch {
            delay(500)
            for (route in routes) {
                if (route.type == MediaRoute2Info.TYPE_BUILTIN_SPEAKER) {
                    Log.d(TAG, "found speaker route $route")
                    mediaRouter.transferTo(route)
                }
            }
        }

        setRegularBatteryLevel(lastTempBatt)
    }

    fun connectAudio(context: Context, device: BluetoothDevice?) {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        method.invoke(proxy, device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }
            }
            override fun onServiceDisconnected(profile: Int) { }
        }, BluetoothProfile.HEADSET)

        for (route in routes) {
            if (route.type == MediaRoute2Info.TYPE_BLUETOOTH_A2DP && route.name == device!!.name) {
                Log.d(TAG, "found bt route $route")
                mediaRouter.transferTo(route)
            }
        }

        val statusBarManager = context.getSystemService("statusbar") as StatusBarManager
        statusBarManager.setIconVisibility("wireless_headset", true)
        setRegularBatteryLevel(lastTempBatt)
    }

    fun setRegularBatteryLevel(level: Int) {
        try {
            val service = getObjectField(mContext, "mAdapterService")
            callMethod(service, "setBatteryLevel", mDevice, level, false)
        } catch (e: Exception) {
            Log.e(TAG, "setRegularBatteryLevel failed", e)
        }
    }

    private fun getObjectField(instance: Any?, fieldName: String): Any? {
        if (instance == null) return null
        var cls: Class<*>? = instance.javaClass
        while (cls != null) {
            runCatching {
                return cls.getDeclaredField(fieldName).apply { isAccessible = true }.get(instance)
            }
            cls = cls.superclass
        }
        throw NoSuchFieldException(fieldName)
    }

    private fun callMethod(instance: Any?, methodName: String, vararg args: Any?): Any? {
        if (instance == null) return null
        var cls: Class<*>? = instance.javaClass
        while (cls != null) {
            cls.declaredMethods.firstOrNull { it.name == methodName && it.parameterTypes.size == args.size }?.let {
                it.isAccessible = true
                return it.invoke(instance, *args)
            }
            cls = cls.superclass
        }
        throw NoSuchMethodException(methodName)
    }
}

package moe.chenxy.moondropods.hook

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import com.xzakota.hyper.notification.focus.FocusNotification
import moe.chenxy.moondropods.utils.FocusIslandPrefs
import moe.chenxy.moondropods.utils.FocusIslandUtil
import moe.chenxy.moondropods.utils.PodImageLoader
import moe.chenxy.moondropods.utils.SystemApisUtils
import moe.chenxy.moondropods.utils.SystemApisUtils.cancelAsUser
import moe.chenxy.moondropods.utils.SystemApisUtils.notifyAsUser
import moe.chenxy.moondropods.config.ConfigManager
import moe.chenxy.moondropods.utils.miuiStrongToast.data.BatteryParams
import moe.chenxy.moondropods.R

@SuppressLint("MissingPermission")
object MiBluetoothToastHook : HookContext() {

    override fun onHook() {

        fun deleteIntent(context: Context, bluetoothDevice: BluetoothDevice): PendingIntent? {
            val intent = Intent("com.android.bluetooth.headset.notification.cancle")
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice)
            return PendingIntent.getBroadcast(context, 0, intent, 201326592)
        }

        @SuppressLint("WrongConstant")
        fun createPodsNotification(bluetoothDevice: BluetoothDevice?, context: Context, batteryParams: BatteryParams) {
            val miheadset_notification_Box = context.resources.getIdentifier("miheadset_notification_Box", "string", "com.xiaomi.bluetooth")
            val miheadset_notification_LeftEar = context.resources.getIdentifier("miheadset_notification_LeftEar", "string", "com.xiaomi.bluetooth")
            val miheadset_notification_RightEar = context.resources.getIdentifier("miheadset_notification_RightEar", "string", "com.xiaomi.bluetooth")
            val system_notification_accent_color = context.resources.getIdentifier("system_notification_accent_color", "color", "android")
            if (bluetoothDevice == null) {
                Log.e("MoondropPods", "createPodsNotification: btDevice null")
                return
            }
            try {
                val address: String = bluetoothDevice.address
                var alias: String? = bluetoothDevice.alias
                if (alias?.isEmpty() == true) {
                    alias = bluetoothDevice.name
                }

                val caseBattStr = if (batteryParams.case != null && batteryParams.case!!.isConnected && miheadset_notification_Box != 0)
                    "${context.resources.getString(miheadset_notification_Box)}${batteryParams.case!!.battery}%" +
                            "${if (batteryParams.case!!.isCharging) "⚡ " else " "}\n"
                else ""
                val leftEar = if (batteryParams.left != null && batteryParams.left!!.isConnected && miheadset_notification_LeftEar != 0)
                    "${context.resources.getString(miheadset_notification_LeftEar)}${batteryParams.left!!.battery}%" +
                        (if (batteryParams.left!!.isCharging) "⚡" else "")
                else ""
                val leftToRight = if (batteryParams.left?.isConnected == true && batteryParams.right?.isConnected == true) " " else ""
                val rightEar = if (batteryParams.right != null && batteryParams.right!!.isConnected && miheadset_notification_RightEar != 0)
                    "$leftToRight${context.resources.getString(miheadset_notification_RightEar)}${batteryParams.right!!.battery}%" +
                        (if (batteryParams.right!!.isCharging) "⚡ " else " ")
                else ""

                val contentText: String = caseBattStr + leftEar + rightEar
                val notificationManager = context.getSystemService("notification") as NotificationManager
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        "BTHeadset$address",
                        alias,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        setSound(null, null)
                        setAllowBubbles(true)
                    }
                )

                val moduleContext = context.createPackageContext(
                    "moe.chenxy.moondropods", Context.CONTEXT_IGNORE_SECURITY
                )
                val headsetBitmap = PodImageLoader.loadBoxBitmap(context, prefs, address)
                    ?: BitmapFactory.decodeResource(moduleContext.resources, R.drawable.img_box)
                if (headsetBitmap == null) {
                    Log.e("MoondropPods", "createPodsNotification: headset bitmap null")
                    return
                }
                val headsetIcon = Icon.createWithBitmap(headsetBitmap)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent("chen.action.moondrop.show_pods_ui").apply {
                        setClassName("moe.chenxy.moondropods", "moe.chenxy.moondropods.PopupActivity")
                        putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice)
                        putExtra("bluetoothaddress", bluetoothDevice.address)
                        putExtra("device_name", alias)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val focusExtras = FocusNotification.buildV3 {
                    val logo = createPicture("key_headset", headsetIcon)
                    enableFloat = true
                    ticker = alias ?: ""
                    updatable = true

                    iconTextInfo {
                        animIconInfo {
                            type = 0
                            src = logo
                        }
                        title = alias ?: ""
                        content = contentText
                    }

                    island {
                        islandProperty = 1
                        bigIslandArea {
                            imageTextInfoLeft {
                                type = 1
                                picInfo {
                                    type = 1
                                    pic = logo
                                }
                            }
                            imageTextInfoRight {
                                type = 2
                                textInfo {
                                    title = alias ?: ""
                                    content = contentText
                                }
                            }
                        }
                    }
                }
                // AOD: left/right ear battery merged into aodTitle
                val aodParts = mutableListOf<String>()
                if (batteryParams.left?.isConnected == true)
                    aodParts.add("L ${batteryParams.left!!.battery}%")
                if (batteryParams.right?.isConnected == true)
                    aodParts.add("R ${batteryParams.right!!.battery}%")
                val aodTitle = aodParts.joinToString(" | ")
                try {
                    val json = org.json.JSONObject(focusExtras.getString("miui.focus.param") ?: "{}")
                    val pv2 = json.optJSONObject("param_v2") ?: org.json.JSONObject()
                    pv2.put("aodTitle", aodTitle)
                    pv2.put("aodPic", "key_headset")
                    json.put("param_v2", pv2)
                    focusExtras.putString("miui.focus.param", json.toString())
                } catch (e: Exception) {
                    Log.e("MoondropPods", "Failed to inject AOD params", e)
                }
                notificationManager.notifyAsUser(
                    "BTHeadset$address",
                    10003,
                    Notification.Builder(context, "BTHeadset$address")
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .setWhen(0L)
                        .setTicker(alias)
                        .setContentTitle(alias)
                        .setContentText(contentText)
                        .setContentIntent(pendingIntent)
                        .setDeleteIntent(deleteIntent(context, bluetoothDevice))
                        .setColor(context.getColor(system_notification_accent_color))
                        .apply { focusExtras?.let { addExtras(it) } }
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .build(),
                    SystemApisUtils.getUserAllUserHandle()
                )
            } catch (e: Exception) {
                Log.e("MoondropPods", "Failed to create Pod Notification", e)
            }
        }

        fun cancelNotification(bluetoothDevice: BluetoothDevice, context: Context) {
            try {
                val address = bluetoothDevice.address
                if (address.isNotEmpty()) {
                    val notificationManager = context.getSystemService("notification") as NotificationManager
                    notificationManager.cancelAsUser("BTHeadset$address", 10003, SystemApisUtils.getUserAllUserHandle())
                }
            } catch (e: Exception) {
                Log.e("MoondropPods", "Failed to cancel Pod Notification!", e)
            }
        }

        hookConstructorAfter(findConstructorByParamCount("com.android.bluetooth.ble.app.MiuiBluetoothNotification", 2)) {
            val context = getObjectField(instance, "mContext") as Context

            val broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    when (p1?.action) {
                        "chen.action.moondrop.sendstrongtoast" -> {
                            if (ConfigManager.islandMode() != ConfigManager.ISLAND_MODE_MODULE) {
                                Log.d("MoondropPods", "skip module island mode=${ConfigManager.islandMode()}")
                                return
                            }
                            val batteryParams = p1.getParcelableExtra("batteryParams", BatteryParams::class.java)
                            if (batteryParams == null) {
                                Log.e("MoondropPods", "sendstrongtoast: batteryParams null")
                                return
                            }
                            val address = p1.getStringExtra("address").orEmpty()
                            val deviceName = p1.getStringExtra("device_name")?.takeIf { it.isNotBlank() }
                            FocusIslandUtil.showBatteryIsland(
                                context = context,
                                batteryParams = batteryParams,
                                durationSeconds = FocusIslandPrefs.DEFAULT_TEMPORARY_BATTERY_ISLAND_DURATION_SECONDS,
                                deviceName = deviceName,
                                prefs = prefs,
                                address = address,
                            )
                        }

                        "chen.action.moondrop.updatepodsnotification" -> {
                            val batteryParams = p1.getParcelableExtra("batteryParams", BatteryParams::class.java)
                            val device = p1.getParcelableExtra("device", BluetoothDevice::class.java)
                            if (batteryParams == null || device == null) {
                                Log.e("MoondropPods", "updatepodsnotification: batteryParams=$batteryParams, device=$device")
                                return
                            }
                            createPodsNotification(device, context, batteryParams)
                        }

                        "chen.action.moondrop.cancelpodsnotification" -> {
                            val device = p1.getParcelableExtra("device", BluetoothDevice::class.java)
                            if (device == null) {
                                Log.e("MoondropPods", "cancelpodsnotification: device null")
                                return
                            }
                            cancelNotification(device, context)
                        }
                    }
                }
            }

            val intentFilter = IntentFilter("chen.action.moondrop.sendstrongtoast")
            intentFilter.addAction("chen.action.moondrop.updatepodsnotification")
            intentFilter.addAction("chen.action.moondrop.cancelpodsnotification")
            context.registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        }
    }
}
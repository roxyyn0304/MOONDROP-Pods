package moe.chenxy.moondropods.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.xzakota.hyper.notification.focus.FocusNotification
import moe.chenxy.moondropods.R
import moe.chenxy.moondropods.config.PodImageResource
import moe.chenxy.moondropods.utils.miuiStrongToast.data.BatteryParams

/** 临时电量岛时长选项（秒）与默认值 */
object FocusIslandPrefs {
    val TEMPORARY_BATTERY_ISLAND_DURATION_SECOND_OPTIONS = listOf(1, 3, 4, 5, 10)
    const val DEFAULT_TEMPORARY_BATTERY_ISLAND_DURATION_SECONDS = 4
}

@SuppressLint("WrongConstant", "MissingPermission", "NotificationPermission")
object FocusIslandUtil {
    private const val TAG = "MoondropPods-FocusIsland"
    private const val CHANNEL_ID = "moondrop_focus_island"
    private const val CHANNEL_NAME = "MOONDROP Battery"
    private const val NOTIFICATION_ID = 10086
    private const val MODULE_PACKAGE = "moe.chenxy.moondropods"
    private const val LETTER_BITMAP_SIZE = 96
    private const val MIUIX_BLUE = "#0D84FF"
    private const val IOS_CHARGING_GREEN = "#34C759"

    fun showBatteryIsland(
        context: Context,
        batteryParams: BatteryParams,
        durationSeconds: Int = FocusIslandPrefs.DEFAULT_TEMPORARY_BATTERY_ISLAND_DURATION_SECONDS,
        deviceName: String? = null,
        prefs: SharedPreferences? = null,
        address: String? = null,
    ): Boolean {
        try {
            val islandDurationSeconds = durationSeconds.takeIf {
                it in FocusIslandPrefs.TEMPORARY_BATTERY_ISLAND_DURATION_SECOND_OPTIONS
            } ?: FocusIslandPrefs.DEFAULT_TEMPORARY_BATTERY_ISLAND_DURATION_SECONDS
            val leftConnected = batteryParams.left?.isConnected == true
            val rightConnected = batteryParams.right?.isConnected == true

            // Need at least one ear connected
            if (!leftConnected && !rightConnected) return false

            val leftText = if (leftConnected) "${batteryParams.left!!.battery}" else "-"
            val rightText = if (rightConnected) "${batteryParams.right!!.battery}" else "-"

            // 从模块 APK 加载耳机图片为 Bitmap，避免跨包资源引用问题
            val moduleContext = context.createPackageContext(
                MODULE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY
            )
            // 优先用用户自定义的岛图（经 ContentProvider 跨进程读取）；缺省回退模块内置资源。
            // 使用编译期资源 ID。release 的 resopt 会重命名资源 entry，按字符串
            // getIdentifier("img_left") / getIdentifier("img_right") 会得到 0；编译期引用会随资源表一起重写。
            val leftBitmap = loadCustomIslandBitmap(context, prefs, address, PodImageResource.LEFT)
                ?: BitmapFactory.decodeResource(moduleContext.resources, R.drawable.img_left)
            val rightBitmap = loadCustomIslandBitmap(context, prefs, address, PodImageResource.RIGHT)
                ?: BitmapFactory.decodeResource(moduleContext.resources, R.drawable.img_right)
            val caseBitmap = loadCustomIslandBitmap(context, prefs, address, PodImageResource.BOX)
                ?: BitmapFactory.decodeResource(moduleContext.resources, R.drawable.img_box)

            if (leftBitmap == null || rightBitmap == null || caseBitmap == null) {
                Log.e(TAG, "Failed to decode Focus Island icon bitmaps")
                return false
            }

            // 使用 createWithBitmap 直接嵌入图片数据，SystemUI 无需再访问模块资源。
            val leftIcon = Icon.createWithBitmap(leftBitmap)
            val rightIcon = Icon.createWithBitmap(rightBitmap)
            val caseIcon = Icon.createWithBitmap(caseBitmap)
            val leftSvgLightIcon = Icon.createWithResource(moduleContext, R.drawable.ic_airpods_left_light)
            val leftSvgDarkIcon = Icon.createWithResource(moduleContext, R.drawable.ic_airpods_left_dark)
            val rightSvgLightIcon = Icon.createWithResource(moduleContext, R.drawable.ic_airpods_right_light)
            val rightSvgDarkIcon = Icon.createWithResource(moduleContext, R.drawable.ic_airpods_right_dark)
            val caseSvgLightIcon = Icon.createWithResource(moduleContext, R.drawable.ic_airpods_case_light)
            val caseSvgDarkIcon = Icon.createWithResource(moduleContext, R.drawable.ic_airpods_case_dark)

            val caseText = batteryParams.case
                ?.takeIf { it.isConnected }
                ?.battery
                ?.coerceIn(0, 100)
                ?.let { "$it%" }
                ?: "-"
            val leftBatteryText = if (leftConnected) "$leftText%" else "-"
            val rightBatteryText = if (rightConnected) "$rightText%" else "-"
            val caseTitle = moduleContext.getString(
                R.string.battery_island_case_title,
                caseText
            )
            val earSummary = moduleContext.getString(
                R.string.battery_island_ear_summary,
                leftBatteryText,
                rightBatteryText
            )
            val leftProgress = batteryParams.left
                ?.takeIf { it.isConnected }
                ?.battery
                ?.coerceIn(0, 100)
                ?: 0
            val rightProgress = batteryParams.right
                ?.takeIf { it.isConnected }
                ?.battery
                ?.coerceIn(0, 100)
                ?: 0
            val caseProgress = batteryParams.case
                ?.takeIf { it.isConnected }
                ?.battery
                ?.coerceIn(0, 100)
                ?: 0

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setAllowBubbles(true)
                }
            )

            val extras = FocusNotification.buildV3 {
                val picLeft = createPicture("key_pic_left", leftIcon)
                val picRight = createPicture("key_pic_right", rightIcon)
                val picCase = createPicture("key_pic_case", caseIcon)
                val picLeftActionLight = createPicture("key_pic_left_action_light", leftSvgLightIcon)
                val picLeftActionDark = createPicture("key_pic_left_action_dark", leftSvgDarkIcon)
                val picRightActionLight = createPicture("key_pic_right_action_light", rightSvgLightIcon)
                val picRightActionDark = createPicture("key_pic_right_action_dark", rightSvgDarkIcon)
                val picCaseActionLight = createPicture("key_pic_case_action_light", caseSvgLightIcon)
                val picCaseActionDark = createPicture("key_pic_case_action_dark", caseSvgDarkIcon)

                enableFloat = true
                ticker = "MOONDROP"
                tickerPic = picLeft

                isShowNotification = false
                islandFirstFloat = false
                // 展开态使用焦点通知组件；bigIslandArea 只负责摘要态。
                // 充电盒是头像，应使用 chatInfo.picProfile，而不是 baseInfo.picFunction。
                chatInfo {
                    picProfile = picCase
                    picProfileDark = picCase
                    title = "已连接"
                    content = deviceName ?: "MOONDROP"
                }
                island {
                    islandProperty = 1
                    islandTimeout = islandDurationSeconds
                    bigIslandArea {
                        imageTextInfoLeft {
                            type = 1
                            picInfo {
                                type = 1
                                pic = picLeft
                            }
                            textInfo {
                                title = leftText
                                content = "%"
                            }
                        }
                        imageTextInfoRight {
                            type = 2
                            picInfo {
                                type = 1
                                pic = picRight
                            }
                            textInfo {
                                title = rightText
                                content = "%"
                            }
                        }
                    }
                }

                // 进度按钮属于焦点通知的 actions 数组，不能序列化为 textButton。
                actions {
                    addActionInfo {
                        type = 1
                        actionIcon = picCaseActionLight
                        actionIconDark = picCaseActionDark
                        progressInfo {
                            progress = caseProgress
                            colorProgress = IOS_CHARGING_GREEN
                        }
                    }
                    addActionInfo {
                        type = 1
                        actionIcon = picLeftActionLight
                        actionIconDark = picLeftActionDark
                        progressInfo {
                            progress = leftProgress
                            colorProgress = IOS_CHARGING_GREEN
                        }
                    }
                    addActionInfo {
                        type = 1
                        actionIcon = picRightActionLight
                        actionIconDark = picRightActionDark
                        progressInfo {
                            progress = rightProgress
                            colorProgress = IOS_CHARGING_GREEN
                        }
                    }
                }
            } ?: return false
            // focus-api 1.4 未暴露文档中的两个进度按钮字段，补到最终 JSON，
            // 避免系统按默认动画/颜色渲染成红色。
            setStaticProgressOptions(extras)

            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("MOONDROP")
                .setTicker("MOONDROP")
                .addExtras(extras)
                .build()

            nm.notify(NOTIFICATION_ID, notification)

            Handler(Looper.getMainLooper()).postDelayed({
                try { nm.cancel(NOTIFICATION_ID) } catch (_: Exception) {}
            }, islandDurationSeconds * 1000L)

            Log.d(
                TAG,
                "Focus Island shown: L=$leftText% R=$rightText%, duration=${islandDurationSeconds}s"
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show Focus Island", e)
            return false
        }
    }

    private fun createLetterBitmap(letter: Char, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            LETTER_BITMAP_SIZE,
            LETTER_BITMAP_SIZE,
            Bitmap.Config.ARGB_8888
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metrics = paint.fontMetrics
        val baseline = LETTER_BITMAP_SIZE / 2f - (metrics.ascent + metrics.descent) / 2f
        Canvas(bitmap).drawText(letter.toString(), LETTER_BITMAP_SIZE / 2f, baseline, paint)
        return bitmap
    }

    private fun setStaticProgressOptions(extras: android.os.Bundle) {
        runCatching {
            // 根据文档和系统设计，actions 数组独立序列化在 Bundle 的 miui.focus.actions 中
            val actionsStr = extras.getString("miui.focus.actions")
            if (actionsStr != null) {
                val actions = org.json.JSONArray(actionsStr)
                for (index in 0 until actions.length()) {
                    val action = actions.optJSONObject(index) ?: continue
                    val progressInfo = action.optJSONObject("progressInfo") ?: continue
                    // 参考官方格式: colorProgress是进度条颜色，colorProgressEnd是进度条底色(圆环底色)
                    progressInfo.put("colorProgress", IOS_CHARGING_GREEN)
                    progressInfo.put("colorProgressDark", IOS_CHARGING_GREEN)
                    progressInfo.put("colorProgressEnd", "#1A000000")       // 浅色模式底色
                    progressInfo.put("colorProgressEndDark", "#29FFFFFF")   // 深色模式底色
                    progressInfo.put("isCCW", true)
                    progressInfo.put("isAutoProgress", false)
                }
                extras.putString("miui.focus.actions", actions.toString())
                return
            }

            val root = org.json.JSONObject(extras.getString("miui.focus.param") ?: return)
            val param = root.optJSONObject("param_v2") ?: return
            val actions = param.optJSONArray("actions") ?: return
            for (index in 0 until actions.length()) {
                val action = actions.optJSONObject(index) ?: continue
                val progressInfo = action.optJSONObject("progressInfo") ?: continue
                progressInfo.put("colorProgress", IOS_CHARGING_GREEN)
                progressInfo.put("colorProgressDark", IOS_CHARGING_GREEN)
                progressInfo.put("colorProgressEnd", "#1A000000")
                progressInfo.put("colorProgressEndDark", "#29FFFFFF")
                progressInfo.put("isCCW", true)
                progressInfo.put("isAutoProgress", false)
            }
            extras.putString("miui.focus.param", root.toString())
        }.onFailure { Log.w(TAG, "Failed to configure progress button JSON", it) }
    }

    /**
     * 读取用户导入的自定义岛图（经 ContentProvider 跨进程），未导入时返回 null 由调用方回退内置资源。
     * LEFT/RIGHT 无自定义时回退到 BOX 图片，与模块详情页展示一致。
     */
    private fun loadCustomIslandBitmap(
        context: Context,
        prefs: SharedPreferences?,
        address: String?,
        resource: PodImageResource,
    ): Bitmap? {
        if (prefs == null || address.isNullOrBlank()) return null
        return runCatching {
            when (resource) {
                PodImageResource.LEFT -> PodImageLoader.loadIslandLeftBitmap(context, prefs, address)
                PodImageResource.RIGHT -> PodImageLoader.loadIslandRightBitmap(context, prefs, address)
                PodImageResource.BOX -> PodImageLoader.loadBoxBitmap(context, prefs, address)
            }
        }.getOrNull()
    }
}

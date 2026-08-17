package moe.chenxy.moondropods

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import moe.chenxy.moondropods.ui.AppTheme
import moe.chenxy.moondropods.utils.miuiStrongToast.data.BatteryParams
import moe.chenxy.moondropods.utils.miuiStrongToast.data.MoondropAction
import moe.chenxy.moondropods.utils.miuiStrongToast.data.PodParams


/** 连接弹窗自动关闭时长选项（秒）与默认值 */
private val CONNECTION_POPUP_DISMISS_SECOND_OPTIONS = listOf(3, 5, 8, 10, 15, 30)
private const val DEFAULT_CONNECTION_POPUP_DISMISS_SECONDS = 8

class ConnectionPopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val initialBatteryParams = intent.getParcelableExtra("status", BatteryParams::class.java)
            ?: runCatching { intent.getParcelableExtra<BatteryParams>("status") }.getOrNull()
            ?: BatteryParams()
        val initialDeviceName = intent.getStringExtra("device_name").orEmpty()
        val autoDismissSeconds = intent.getIntExtra(
            "dismiss_seconds",
            DEFAULT_CONNECTION_POPUP_DISMISS_SECONDS
        ).takeIf { it in CONNECTION_POPUP_DISMISS_SECOND_OPTIONS }
            ?: DEFAULT_CONNECTION_POPUP_DISMISS_SECONDS

        setContent {
            AppTheme {
                ConnectionPopupContent(
                    initialDeviceName = initialDeviceName,
                    initialBatteryParams = initialBatteryParams,
                    autoDismissSeconds = autoDismissSeconds,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ConnectionPopupContent(
    initialDeviceName: String,
    initialBatteryParams: BatteryParams,
    autoDismissSeconds: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deviceName = remember { mutableStateOf(initialDeviceName) }
    val batteryParams = remember { mutableStateOf(initialBatteryParams) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    MoondropAction.ACTION_PODS_CONNECTED -> {
                        deviceName.value = intent.getStringExtra("device_name") ?: deviceName.value
                    }
                    MoondropAction.ACTION_PODS_BATTERY_CHANGED -> {
                        val status = intent.getParcelableExtra("status", BatteryParams::class.java)
                            ?: runCatching { intent.getParcelableExtra<BatteryParams>("status") }.getOrNull()
                        status?.let { batteryParams.value = it }
                    }
                    MoondropAction.ACTION_PODS_DISCONNECTED -> {
                        onDismiss()
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(MoondropAction.ACTION_PODS_CONNECTED)
            addAction(MoondropAction.ACTION_PODS_BATTERY_CHANGED)
            addAction(MoondropAction.ACTION_PODS_DISCONNECTED)
        }, Context.RECEIVER_EXPORTED)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(autoDismissSeconds) {
        delay(autoDismissSeconds * 1000L)
        onDismiss()
    }

    ConnectionPopupCard(
        deviceName = deviceName.value.ifEmpty { stringResource(R.string.app_name) },
        batteryParams = batteryParams.value,
        onDismiss = onDismiss
    )
}

@Composable
private fun ConnectionPopupCard(
    deviceName: String,
    batteryParams: BatteryParams,
    onDismiss: () -> Unit
) {
    val videoBackgroundColor = Color(0xFFFBFBFB)
    val containerColor = videoBackgroundColor
    val textColor = Color(0xFF111111)
    val secondaryTextColor = Color(0xFF333333)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .clip(RoundedCornerShape(55.dp))
                .background(containerColor)
        ) {
            CloseButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 22.dp, end = 22.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 29.dp, bottom = 31.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = deviceName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 75.dp),
                    style = TextStyle(
                        color = textColor,
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(38.dp))
                ConnectionPodImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(960f / 312f)
                )
                Spacer(modifier = Modifier.height(22.dp))

                BatterySummary(
                    batteryParams = batteryParams,
                    textColor = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(52.dp))
                DoneButton(
                    onClick = onDismiss,
                    textColor = textColor,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
            }
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(27.dp)
            .clip(CircleShape)
            .background(Color(0xFFF1F1F1))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(9.dp)) {
            val strokeWidth = 2.8.dp.toPx()
            drawLine(
                color = Color(0xFF8B8B8B),
                start = Offset(1.dp.toPx(), 1.dp.toPx()),
                end = Offset(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF8B8B8B),
                start = Offset(size.width - 1.dp.toPx(), 1.dp.toPx()),
                end = Offset(1.dp.toPx(), size.height - 1.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ConnectionPodImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFFFBFBFB)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_box),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 8.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
    }
}

@Composable
private fun BatterySummary(
    batteryParams: BatteryParams,
    textColor: Color
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidth = 110.dp
        val leftCenter = maxWidth * 0.25f
        val caseCenter = maxWidth * 0.75f

        Column(
            modifier = Modifier
                .width(columnWidth)
                .offset(x = leftCenter - columnWidth / 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BatteryLine(
                label = stringResource(R.string.connection_popup_left_ear),
                pod = batteryParams.left,
                textColor = textColor
            )
            BatteryLine(
                label = stringResource(R.string.connection_popup_right_ear),
                pod = batteryParams.right,
                textColor = textColor
            )
        }
        Box(
            modifier = Modifier
                .width(columnWidth)
                .offset(x = caseCenter - columnWidth / 2),
            contentAlignment = Alignment.TopCenter
        ) {
            BatteryLine(
                label = stringResource(R.string.connection_popup_case),
                pod = batteryParams.case,
                textColor = textColor
            )
        }
    }
}

@Composable
private fun BatteryLine(
    label: String,
    pod: PodParams?,
    textColor: Color
) {
    val isConnected = pod?.isConnected == true
    val level = pod?.battery?.coerceIn(0, 100) ?: 0
    val levelText = if (isConnected) "$level%" else "-"

    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = label,
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
        Image(
            painter = painterResource(getBatteryIconRes(level, pod?.isCharging == true)),
            contentDescription = "$label $levelText",
            modifier = Modifier.size(width = 28.dp, height = 17.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        BasicText(
            text = levelText,
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit, textColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF2F2F2))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = stringResource(R.string.done),
            style = TextStyle(
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        )
    }
}

private fun getBatteryIconRes(level: Int, isCharging: Boolean): Int {
    val index = when {
        level <= 10 -> 1
        level <= 20 -> 2
        level <= 30 -> 3
        level <= 40 -> 4
        level <= 50 -> 5
        level <= 60 -> 6
        level <= 70 -> 7
        level <= 80 -> 8
        level <= 90 -> 9
        else -> 10
    }
    return if (isCharging) {
        when (index) {
            1 -> R.drawable.charge_1
            2 -> R.drawable.charge_2
            3 -> R.drawable.charge_3
            4 -> R.drawable.charge_4
            5 -> R.drawable.charge_5
            6 -> R.drawable.charge_6
            7 -> R.drawable.charge_7
            8 -> R.drawable.charge_8
            9 -> R.drawable.charge_9
            else -> R.drawable.charge_10
        }
    } else {
        when (index) {
            1 -> R.drawable.common_1
            2 -> R.drawable.common_2
            3 -> R.drawable.common_3
            4 -> R.drawable.common_4
            5 -> R.drawable.common_5
            6 -> R.drawable.common_6
            7 -> R.drawable.common_7
            8 -> R.drawable.common_8
            9 -> R.drawable.common_9
            else -> R.drawable.common_10
        }
    }
}


package moe.chenxy.moondropods.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.moondropods.pods.BtLogEntry
import moe.chenxy.moondropods.pods.BtLogStore
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DebugLogPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClear: () -> Unit = {},
) {
    val entries by BtLogStore.entries.collectAsState()
    val listState = rememberLazyListState()
    var expandedEntries by remember { mutableStateOf(setOf<Int>()) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().scrollEndHaptic().overScrollVertical(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
            start = 12.dp,
            end = 12.dp
        ),
        overscrollEffect = null,
    ) {
        item {
            Card {
                ArrowPreference(
                    title = "清空日志",
                    onClick = onClear
                )
            }
        }

        if (entries.isEmpty()) {
            item {
                Text(
                    text = "暂无日志",
                    modifier = Modifier.padding(top = 24.dp).padding(horizontal = 12.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        items(entries.size) { index ->
            val entry = entries.reversed()[index]
            val isExpanded = index in expandedEntries
            BtLogEntryItem(
                entry = entry,
                expanded = isExpanded,
                onToggleExpand = {
                    expandedEntries = if (isExpanded) expandedEntries - index
                    else expandedEntries + index
                }
            )
        }
    }
}

@Composable
private fun BtLogEntryItem(
    entry: BtLogEntry,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier.padding(top = 8.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onToggleExpand
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (entry.isSend) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (entry.isSend) "发送"
                            else "接收",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    if (entry.label != null) {
                        Text(
                            text = "  ${entry.label}",
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
                Text(
                    text = entry.timeFormatted(),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatHexAnnotated(entry.hex),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * FF04 格式 hex 着色:
 *   [0..5] FF 04 + Len + Seq + Vendor 灰色
 *   [6..7] Feature + Cmd 蓝色
 *   [8..]  Payload 橙色
 */
private fun formatHexAnnotated(hex: String): AnnotatedString {
    val bytes = hex.chunked(2)
    if (bytes.isEmpty()) return AnnotatedString(hex)
    val formatted = bytes.joinToString(" ")

    return buildAnnotatedString {
        append(formatted)

        fun colorBytes(startByte: Int, endByte: Int, color: Color) {
            val startChar = startByte * 3
            val endChar = minOf(endByte * 3 + 2, formatted.length)
            if (startChar < formatted.length) {
                addStyle(SpanStyle(color = color), startChar, endChar)
            }
        }

        val dimmed = Color(0xFF888888)
        val cmdColor = Color(0xFF64B5F6)
        val payloadColor = Color(0xFFFFB74D)

        colorBytes(0, 5, dimmed)             // FF 04 + Len + Seq + Vendor
        colorBytes(6, 7, cmdColor)           // Feature + Cmd
        if (bytes.size > 8) {
            colorBytes(8, bytes.size - 1, payloadColor) // Payload
        }
    }
}

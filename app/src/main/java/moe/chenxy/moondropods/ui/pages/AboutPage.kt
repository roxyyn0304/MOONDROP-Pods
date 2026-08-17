package moe.chenxy.moondropods.ui.pages

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.chenxy.moondropods.R
import moe.chenxy.moondropods.ui.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported

@Composable
fun AboutPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current

    // OS3 动态背景：需要 RuntimeShader 支持 + Android 16 (SDK 36) 及以上，纯自动启用
    val effectBackground = remember {
        runCatching { isRuntimeShaderSupported() }.getOrDefault(false) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    BgEffectBackground(
        dynamicBackground = effectBackground,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
                start = 12.dp,
                end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    BasicComponent(
                        title = "MOONDROP-Pods",
                        summary = "https://github.com/roxyyn0304/MOONDROP-Pods",
                        onClick = {
                            Intent(Intent.ACTION_VIEW).apply {
                                this.data = Uri.parse("https://github.com/roxyyn0304/MOONDROP-Pods")
                                context.startActivity(this)
                            }
                        }
                    )
                    BasicComponent(
                        title = stringResource(R.string.based_on),
                        summary = "OppoPods by 1812z"
                    )
                    BasicComponent(
                        title = "Original OppoPods",
                        summary = "https://github.com/1812z/OppoPods",
                        onClick = {
                            Intent(Intent.ACTION_VIEW).apply {
                                this.data = Uri.parse("https://github.com/1812z/OppoPods")
                                context.startActivity(this)
                            }
                        }
                    )
                }
            }
        }
    }
}

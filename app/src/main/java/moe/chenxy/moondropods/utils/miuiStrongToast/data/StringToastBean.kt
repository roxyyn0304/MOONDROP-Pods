package moe.chenxy.moondropods.utils.miuiStrongToast.data

import kotlinx.serialization.Serializable

@Serializable
data class StringToastBean(
    var left: Left? = null,
    var right: Right? = null
)

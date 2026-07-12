package moe.chenxy.moondropods.utils.miuiStrongToast.data

import kotlinx.serialization.Serializable

@Serializable
data class Right(
    var iconParams: IconParams? = null,
    var textParams: TextParams? = null
)

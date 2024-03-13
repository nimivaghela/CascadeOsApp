package com.app.cascadeos.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class CoolTvAppsModel(
    @DrawableRes val buttonImage: Int,
    @StringRes val name: Int,
    var isSelected: Boolean = false,
)
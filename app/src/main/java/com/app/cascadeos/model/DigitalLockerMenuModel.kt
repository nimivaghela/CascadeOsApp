package com.app.cascadeos.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DigitalLockerMenuModel(
    @StringRes val menuItemName: Int,
    @DrawableRes val image: Int? = null,
    val mediaType: MediaType,
    var isSelected: Boolean = false,
)
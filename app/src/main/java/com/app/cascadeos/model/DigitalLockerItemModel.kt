package com.app.cascadeos.model

import androidx.annotation.DrawableRes

data class DigitalLockerItemModel(
    @DrawableRes val thumbnail: Int,
    val webUrl: String? = null,
    val mediaType: MediaType,
    val hasViewButton: HasViewButton,
    val itemName: String = "",
)
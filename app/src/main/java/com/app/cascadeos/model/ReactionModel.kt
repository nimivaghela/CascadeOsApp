package com.app.cascadeos.model

import androidx.annotation.DrawableRes

data class ReactionModel(
    @DrawableRes val profileImage: Int,
    val name: String,
    val message: String,
)
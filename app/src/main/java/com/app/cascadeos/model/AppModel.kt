package com.app.cascadeos.model

data class AppModel(
    val id: Int,
    val appName: String,
    var appUrl: String? = null,
    val icon: Int,
    var isMulticastApp: Boolean = true,
    var isVideoCallKeyBoard: Boolean = true,
    var isPhoneSystemApp: Boolean = false,
    var isVideoGameApp: Boolean = false,
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppModel

        if (appName != other.appName) return false

        return true
    }

    override fun hashCode(): Int {
        return appName.hashCode()
    }
}
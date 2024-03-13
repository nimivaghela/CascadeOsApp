package com.app.cascadeos.model

data class AppDetailModel(
    val name: String,
    var startX: Int,
    var width: Int,
    var webUrl: String = "",
    var gravity: Int,
    var height: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppDetailModel

        if (name != other.name) return false
        if (webUrl != other.webUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + webUrl.hashCode()
        return result
    }
}



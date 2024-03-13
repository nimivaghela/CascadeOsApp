package com.app.cascadeos.model

data class MulticastModel<T>(
    val title: String,
    val app: T? = null,

    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MulticastModel<*>

        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }
}

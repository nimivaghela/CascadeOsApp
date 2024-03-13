package com.app.cascadeos.interfaces

import com.app.cascadeos.model.AppDetailModel

interface SnapToGridListener {
    fun moveView(app: AppDetailModel)
}
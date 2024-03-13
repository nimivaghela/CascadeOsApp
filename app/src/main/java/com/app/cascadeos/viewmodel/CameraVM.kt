package com.app.cascadeos.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel

class CameraVM : ViewModel() {

    var isVideo = false
    var isVideoStarting = false
    var baseTime = SystemClock.elapsedRealtime()

}
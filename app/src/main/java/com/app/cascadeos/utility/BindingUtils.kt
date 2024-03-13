package com.app.cascadeos.utility

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton


@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

@BindingAdapter("visibleInvisible")
fun showInVisible(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("stateUpdateFb")
fun stateUpdateFb(floatingActionButton: FloatingActionButton, isChecked: Boolean) {
    if (isChecked) {
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        floatingActionButton.supportImageTintList = ColorStateList.valueOf(Color.BLACK)

    } else {
        floatingActionButton.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        floatingActionButton.supportImageTintList = ColorStateList.valueOf(Color.WHITE)
    }

}


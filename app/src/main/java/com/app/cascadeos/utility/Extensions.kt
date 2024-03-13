package com.app.cascadeos.utility

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.load
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.app.cascadeos.BuildConfig
import com.app.cascadeos.R
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


fun Context.showToast(message: String?) {
    Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show()
}

fun Int.dpToPx(context: Context): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics)
}

fun Float.pxToDp(context: Context): Float {
    val screenPixelDensity = context.resources.displayMetrics.density
    return this / screenPixelDensity
}

fun Intent.start(context: Context, isSingleTop: Boolean = true) {
    if (isSingleTop) {
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    context.startActivity(this)
}

fun Context.isPortrait(): Boolean {
    return this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

}

fun String.logError(tag: Class<Any>) {
    if (BuildConfig.DEBUG) {
        Log.e(tag.simpleName + " ###", this)
    }
}

fun getURLFromDrawable(id: Int): String {
    return Uri.parse("android.resource://${BuildConfig.APPLICATION_ID}/$id").toString()
}

fun Activity.hideKeyboard() {
    val im: InputMethodManager = getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    im.hideSoftInputFromWindow(
        currentFocus?.windowToken, 0
    )
}

/*@SuppressLint("CheckResult")
fun ImageView.loadImageProfile(
    url: Uri?,
    placeHolder: Int
) {
    Glide
        .with(this.context)
        .load(url)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(placeHolder)
        .error(placeHolder)
        .dontAnimate()
        .into(this)
}*/
fun ImageView.loadImage(
    imageUrl: String = "",
    @DrawableRes image: Int? = null,
    @DrawableRes placeHolder: Int? = null,
    cornerRadius: Float = 0F,
    loadCircleCrop: Boolean = false,
    shouldCrossFade: Boolean = false,
    crossFadeDurationMillis: Int = 0,
    allowCaching: Boolean = false,
    onStart: ((request: ImageRequest) -> Unit),
    onSuccess: ((request: ImageRequest, result: SuccessResult) -> Unit),
    onError: ((request: ImageRequest, error: ErrorResult) -> Unit),
) {
    this.load(imageUrl.ifEmpty { image }) {
        diskCachePolicy(if (allowCaching) CachePolicy.ENABLED else CachePolicy.DISABLED)
        if (placeHolder != null) {
            this.placeholder(placeHolder)
        }
        if (cornerRadius > 0) {
            transformations(RoundedCornersTransformation(cornerRadius))
        }
        if (loadCircleCrop) {
            transformations(CircleCropTransformation())
        }
        if (shouldCrossFade) {
            crossfade(true)
            crossfade(crossFadeDurationMillis)
        }
        listener(onStart = onStart, onSuccess = onSuccess, onError = onError, onCancel = {
            Log.e("TAG", "loadImage: cancel")
        })
    }
}


fun Bitmap.saveImageExternal(
    name: String, context: Context,
    relativePath: String = context.getString(
        R.string.app_name
    ),
): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val value = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$relativePath")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = resolver.insert(collection, value)
        if (item != null) {
            resolver.openOutputStream(item).use {
                this@saveImageExternal.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            value.clear()
            value.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(item, value, null, null)
        }
        item
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), relativePath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, name)
        try {
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            this@saveImageExternal.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        file.toUri()
    }
}


fun View.showSnackBar(message: String) {
    val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_SHORT)
    val params = snackBar.view.layoutParams as FrameLayout.LayoutParams
    params.gravity = Gravity.CENTER
    snackBar.view.layoutParams = params
    snackBar.view.setBackgroundColor(Color.TRANSPARENT)
    val tv = snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    tv.setTextColor(ContextCompat.getColor(this.context, R.color.color_green_frame))
    tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
    snackBar.show()
}

package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutStaticWebBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.viewmodel.MainVM


class WebApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle, private val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>,
) : WindowApp<LayoutStaticWebBinding>(
    context, lifecycle, R.layout.layout_static_web, mBinding, configurationLiveData
) {
    override fun getDetailsModel() = appDetailModel
    override fun appList() = appList

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun start() {
        super.start()
        binding.layoutWeb.apply {
            loadUrl(appDetailModel.webUrl)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val requestUrl = request.url.toString()
                    return !URLUtil.isNetworkUrl(requestUrl)
                }
            }
        }


        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutContent.layoutTransition =
            layoutTransition


        /* binding.imgResizeLeft.setOnTouchListener(object : OnTouchListener{

             var x : Float = 0f
             var y : Float = 0f
             var width : Int  = 0
             var height : Int? = null
             var px : Float = 0f


             override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                 when(event?.action){
                     MotionEvent.ACTION_DOWN ->{
                         // Get the starting X and Y coordinates of the view
                         x = binding.root.x
                         y =  binding.root.y
                         // Get the current width and height of the view
                         width =  binding.mainCard.width
                         height =  binding.root.height

                         px = event.rawX
                     }

                    MotionEvent.ACTION_MOVE ->{
                        // Calculate the new X and Y coordinates of the view
                        val newX = event.rawX
                        val newY = event.rawY

                        "Transition : $newX & $newY".logError(javaClass)

                        val newWidth = px - event.rawX + width
                        val newHeight = event.rawY
                         "Width $newWidth".logError(javaClass)

                        // Set the new width and height of the view
                        binding.root.translationX = newX
                        binding.root.layoutParams.width = newWidth.toInt()
                        binding.root.layoutParams.height = newHeight.toInt()
                        binding.root.requestLayout()
                    }
                 }

                 return true
             }

         })*/


        /* binding.layoutMain
             .setOnTouchListener(object : View.OnTouchListener {
                 var x = 0f
                 var y = 0f
                 override fun onTouch(v: View?, motionEvent: MotionEvent?): Boolean {
                     when (motionEvent?.action) {
                         MotionEvent.ACTION_DOWN -> {
                             "Action Dwn".logError(javaClass)
                             //  enableKeyboard()
                            // binding.root.bringToFront()
                             x = motionEvent.x
                             y = motionEvent.y

                         }

                         MotionEvent.ACTION_UP -> {
                             if (abs(x - motionEvent.x) < SCROLL_THRESHOLD || abs(y - motionEvent.y) < SCROLL_THRESHOLD) {
                                 if (binding.actionBar.actionBar.visibility == View.GONE) {
                                     binding.actionBar.actionBar.visibility = View.VISIBLE
                                 } else {
                                     binding.actionBar.actionBar.visibility = View.GONE
                                 }
                             }

                         }
                         *//*MotionEvent.ACTION_OUTSIDE -> {
                            "Action Out".logError(javaClass)
                           // disableKeyboard()

                        }*//*
                        else -> {

                        }
                    }
                    return false
                }
            })*/
    }

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)

        if (appDetailModel.name == context.getString(R.string.txt_link) || appDetailModel.name == context.getString(R.string.txt_entertain)) {
            MainVM.coolTvAppLivedata.postValue(appDetailModel.name)
        }
    }

    fun top() {
        binding.root.bringToFront()
    }

}
package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutBuyVideoBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.ui.PurchaseActivity
import com.app.cascadeos.viewmodel.MainVM.Companion.coolTvAppLivedata

class BuyVideoApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    configurationLiveData: MutableLiveData<Configuration?>,
) : WindowApp<LayoutBuyVideoBinding>(
    context, lifecycle, R.layout.layout_buy_video, mBinding, configurationLiveData
) {
    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList


    private var isBuyNowClicked = false

    override fun start() {
        super.start()
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition
        binding.layoutContent.layoutTransition = layoutTransition
        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_full_screen).visibility = View.GONE

        binding.btnBuyNow.setOnClickListener {
            playTone(R.raw.cooltv_apps)
            PurchaseActivity.start(context)
            isBuyNowClicked = true
            close()
        }
        binding.tvVideoDetail.movementMethod = ScrollingMovementMethod()

        setBlinkAnimationToCard()
    }

    private fun playTone(tone: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(context, tone)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
        if(!isBuyNowClicked){
            coolTvAppLivedata.postValue(appDetailModel.name)
        }else{
            coolTvAppLivedata.postValue(appDetailModel.name+"1")
        }

    }
    fun top() {
        binding.root.bringToFront()
    }

    private fun setBlinkAnimationToCard() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.blink_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        binding.imgHighlightView.backgroundTintList = context.getColorStateList(R.color.color_clickVideo_blow)
        binding.imgHighlightView.visibility = View.VISIBLE
        binding.imgHighlightView.startAnimation(animation)
    }
}
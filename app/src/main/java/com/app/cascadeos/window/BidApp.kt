package com.app.cascadeos.window

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.media.MediaPlayer
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutBidBinding
import com.app.cascadeos.interfaces.BidListener
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.ListConstants.getBidList
import com.app.cascadeos.utility.hideKeyboard
import com.app.cascadeos.utility.showToast
import com.app.cascadeos.viewmodel.MainVM

class BidApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>,
    private var onBidSent: (() -> Unit),
) : WindowApp<LayoutBidBinding>(
    context, lifecycle, R.layout.layout_bid, mBinding, configurationLiveData
), BidListener {
    companion object {
        var myBidAmount: String = "0"
    }

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    override fun start() {
        super.start()
        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_full_screen).visibility = View.GONE
        binding.bidAmount = "$90 Million"

        binding.imageView2.setOnClickListener {
            playTone(R.raw.cooltv_apps)
            if (binding.outlinedTextField.text.toString().trim().isEmpty()) {
                context.showToast(context.getString(R.string.please_enter_amount))
            } else {
                myBidAmount = binding.outlinedTextField.text.toString().trim()
                binding.bidAmount = "$$myBidAmount Million"
                binding.outlinedTextField.text.clear()
                (context as MainActivity).hideKeyboard()
                onBidSent()
            }

        }


        binding.layoutMain.viewTreeObserver.addOnGlobalLayoutListener {
//            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight: Int = mBinding.root.rootView.height

            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val heightDifference = (screenHeight - r.bottom - r.top) - (screenHeight - binding.root.height)

                if (heightDifference > 10) {
                    binding.layoutContent.setPadding(0, 0, 0, heightDifference)

                } else {
                    binding.layoutContent.setPadding(0, 0, 0, 0)
                }

            } else {/* val transitionY =  binding.root.translationY - ((binding.root.translationY + binding.root.height) - r.height())

                 if(binding.root.translationY >1 && transitionY <= 330) {
                     "TransitionY  $transitionY".logError(javaClass)
                     binding.root.translationY = transitionY
                 }else {
                     var transitionMarginY = binding.root.translationY.toInt()
                     "TransitionY $transitionMarginY".logError(javaClass)
                     when (transitionMarginY) {
                         in 1..context.resources.getInteger(R.integer.b_start) -> {
                             transitionMarginY = context.resources.getInteger(R.integer.b_start)
                         }

                         in context.resources.getInteger(R.integer.b_start)..context.resources.getInteger(
                             R.integer.c_start
                         ),
                         -> {
                             transitionMarginY = context.resources.getInteger(R.integer.c_start)
                         }

                         else ->{
                             transitionMarginY = 0
                         }
                     }

                     binding.root.translationY = transitionMarginY.toFloat()

                 }*/

            }
        }
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
        MainVM.coolTvAppLivedata.postValue(appDetailModel.name)
        myBidAmount = "0"
    }

    override fun onBidMade(bidPosition: Int) {
        if (bidPosition != -1) {
            if (Integer.parseInt(getBidList()[bidPosition].amount) < Integer.parseInt(myBidAmount)) {
                binding.bidAmount = "$$myBidAmount Million"
            } else {
                binding.bidAmount = "$${getBidList()[bidPosition].amount} Million"
            }
        }else {
            binding.bidAmount = "$90 Million"
        }
    }

}
package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.app.cascadeos.R
import com.app.cascadeos.adapter.ReactionAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutInteractAppBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.ReactionModel
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.AVTAR_URL
import com.app.cascadeos.utility.ListConstants
import com.app.cascadeos.utility.hideKeyboard
import com.app.cascadeos.utility.showToast
import com.app.cascadeos.viewmodel.MainVM

class InteractApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    configurationLiveData: MutableLiveData<Configuration?>,
) : WindowApp<LayoutInteractAppBinding>(
    context, lifecycle, R.layout.layout_interact_app, mBinding, configurationLiveData
) {
    private lateinit var interactAdapter: ReactionAdapter
    private var reactionList = ArrayList<ReactionModel>()

    private var exoPlayer: ExoPlayer? = null

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    override fun start() {
        super.start()
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition
        binding.layoutContent.layoutTransition = layoutTransition
        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_full_screen).visibility = View.GONE

        exoPlayer = ExoPlayer.Builder(context).build().also { ePlayer ->
            binding.exoPlayerView.player = ePlayer
            val mediaItem = MediaItem.fromUri(AVTAR_URL)
            ePlayer.setMediaItem(mediaItem)
            ePlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            ePlayer.prepare()
            ePlayer.play()
        }

        exoPlayer?.addListener(@UnstableApi object : Player.Listener {
            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (playbackState == ExoPlayer.STATE_BUFFERING || playbackState == ExoPlayer.STATE_IDLE) {
                    binding.progressBarVideoLoading.visibility = View.VISIBLE
                } else if (playbackState == ExoPlayer.STATE_READY) {
                    binding.progressBarVideoLoading.visibility = View.GONE
                }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            reactionList = ListConstants.getReactionList()
            interactAdapter = ReactionAdapter(context, reactionList)
            binding.rvReaction.adapter = interactAdapter

            binding.interactWidgetGroup.visibility = View.VISIBLE
        }, 2000)

        binding.clickListener = View.OnClickListener {
            when (it.id) {
                R.id.fabChat -> {
                    binding.interactWidgetGroup.visibility = View.VISIBLE
                    binding.fabChat.visibility = View.GONE
                }

                R.id.imgCloseReactions -> {
                    binding.interactWidgetGroup.visibility = View.GONE
                    binding.fabChat.visibility = View.VISIBLE
                }

                R.id.imgReact -> {
                    //show gif animation here
                }

                R.id.imgSend -> {
                    if (binding.edtReaction.text.toString().trim().isEmpty()) {
                        context.showToast(context.getString(R.string.please_enter_message))
                    } else {
                        interactAdapter.addMessage(
                            ReactionModel(
                                profileImage = R.drawable.img_interact_profile,
                                name = context.getString(R.string.simmy_jackson),
                                message = binding.edtReaction.text.toString().trim()
                            )
                        )
                        binding.rvReaction.scrollToPosition(interactAdapter.itemCount - 1)
                        binding.edtReaction.text?.clear()
                        binding.edtReaction.clearFocus()
                    }
                    (context as MainActivity).hideKeyboard()
                }
            }
        }
    }

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
        MainVM.coolTvAppLivedata.postValue(appDetailModel.name)
    }
}
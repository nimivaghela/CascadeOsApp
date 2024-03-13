package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutPhoneSystemBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.interfaces.SnapToGridListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.ui.fragment.DialerFragment


class PhoneSystemApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle, val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    val isResume: Boolean = false,
    override val snapToGridListener: SnapToGridListener,
    private val configurationLiveData: MutableLiveData<Configuration?>,
    private val appClickLiveData: MutableLiveData<View>,
    private var onMulticastAppClick: ((
        startX: Int,
        width: Int,
        height: Int,
        itemModel: AppModel,
    ) -> Unit),
) : WindowApp<LayoutPhoneSystemBinding>(context, lifecycle, R.layout.layout_phone_system, mBinding, null) {

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    private val configObserver = Observer<Configuration?> {

        if (isObserverActive) {
            it?.let {
                if (it.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    close()
                }
            }
        }
        isObserverActive = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        super.start()

        val dialerFragment = DialerFragment(appClickLiveData, onMulticastAppClick, screen)
        val ft: FragmentTransaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, dialerFragment)
        ft.commit()


        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutContent.layoutTransition = layoutTransition

        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_full_screen).visibility = View.GONE

        if (isResume) {
            dialerFragment.resumeCalling()
        }

        configurationLiveData.observe((context as MainActivity), configObserver)
    }

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
        configurationLiveData.removeObserver(configObserver)
    }


}


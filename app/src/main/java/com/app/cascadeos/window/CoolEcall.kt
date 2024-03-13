package com.app.cascadeos.window

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutCoolEcallBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.ui.fragment.CoolEcallListFragment
import com.app.cascadeos.viewmodel.MainVM

class CoolEcall(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>,
) : WindowApp<LayoutCoolEcallBinding>(
    context,
    lifecycle,
    R.layout.layout_cool_ecall,
    mBinding,
    configurationLiveData
) {

    // private lateinit var navController: NavController

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    override fun start() {
        super.start()
        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_full_screen).visibility =
            View.GONE
        val coolECallListFragment = CoolEcallListFragment()
        val ft: FragmentTransaction =
            (context as AppCompatActivity).supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, coolECallListFragment)
        ft.commit()
    }

    override fun close() {


        val currentFragment = (context as MainActivity).supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is CoolEcallListFragment) {
            super.close()
            MainVM.coolTvAppLivedata.postValue(appDetailModel.name)
            closeWindowListener.closeWindow(appDetailModel)
        } else {
            //  Toast.makeText(context,"In else",Toast.LENGTH_SHORT).show()
            context.supportFragmentManager.popBackStack()
        }
        /*  if (currentFragment is CoolEcallListFragment){
              closeWindowListener.closeWindow(appDetailModel)
          }else{
              context.supportFragmentManager.popBackStack()
          }*/

    }

}
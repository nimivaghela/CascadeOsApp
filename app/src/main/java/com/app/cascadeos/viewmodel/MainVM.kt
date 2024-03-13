package com.app.cascadeos.viewmodel

import android.content.res.Configuration
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.MulticastModel
import com.app.cascadeos.window.BidApp
import com.app.cascadeos.window.BuyVideoApp
import com.app.cascadeos.window.CoolEcall
import com.app.cascadeos.window.WebApp

class MainVM : ViewModel() {

    val appList = ArrayList<AppDetailModel>(0)
    val multicastList = ArrayList<MulticastModel<Any>>(0)
    val gameMulticastList = ArrayList<MulticastModel<Any>>(0)
    val phoneSystemAppList = ArrayList<MulticastModel<Any>>(0)
    val videoCallWithKeyboardAppList = ArrayList<MulticastModel<Any>>(0)
    var multicastCurrentIndex = 0
    var gameMulticastCurrentIndex = 0
    var phoneSystemCurrentIndex = 0
    var videoCallWithKeyboardAppCurrentIndex = 0
    var configurationLiveData: MutableLiveData<Configuration?> = MutableLiveData()

    var keyTextLiveData: MutableLiveData<String> = MutableLiveData()
    var isKeyboardOpen: MutableLiveData<Boolean> = MutableLiveData()

    var clickVideoApp: BuyVideoApp? = null
    var linkApp: WebApp? = null
    var entertainApp: WebApp? = null
    var coolEcall: CoolEcall? = null
    var bidApp: BidApp? = null

    companion object {
        var coolTvAppLivedata: MutableLiveData<String?> = MutableLiveData()
    }


    fun removeMulticast(app: AppDetailModel) {
        val index = multicastList.indexOf(MulticastModel(title = app.name))
        if (index >= 0) {
            multicastList.removeAt(index)
        }
    }

    fun removeGameMulticast(app: AppDetailModel) {
        val index = gameMulticastList.indexOf(MulticastModel(title = app.name))
        if (index >= 0) {
            gameMulticastList.removeAt(index)
        }
    }

    fun getOpenApp(name: String): AppDetailModel? {
        return appList.find { it.name == name }
    }

}
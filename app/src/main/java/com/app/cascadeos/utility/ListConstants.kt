package com.app.cascadeos.utility

import android.content.Context
import com.app.cascadeos.R
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.model.Bid
import com.app.cascadeos.model.CoolTvAppsModel
import com.app.cascadeos.model.ReactionModel

object ListConstants {

    fun getMulticastAppList(context: Context): ArrayList<AppModel> {
        val appList = ArrayList<AppModel>()
        appList.add(
            AppModel(
                id = R.id.fb, appName = context.getString(R.string.facebook), icon = R.drawable.ic_fb_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.twitter, appName = context.getString(R.string.twitter), icon = R.drawable.ic_twitter_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_discovery, appName = context.getString(R.string.discovery), icon = R.drawable.ic_discovery_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_amazon, appName = context.getString(R.string.amazon), icon = R.drawable.ic_amazon_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.linkedin, appName = context.getString(R.string.linkedin), icon = R.drawable.ic_linkdin
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_zoom, appName = context.getString(R.string.txt_zoom), icon = R.drawable.icon_zoom
            )
        )
        return appList
    }

    fun getPhoneSystemAppList(context: Context): ArrayList<AppModel> {
        val appList = ArrayList<AppModel>()
        appList.add(
            AppModel(
                id = R.id.app_messages, appName = context.getString(R.string.messages), icon = R.drawable.ic_message_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_whatsapp, appName = context.getString(R.string.whatsapp), icon = R.drawable.ic_whatsapp_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.instagram, appName = context.getString(R.string.instagram), icon = R.drawable.ic_instagram
            )
        )
        appList.add(
            AppModel(
                id = R.id.fb, appName = context.getString(R.string.facebook), icon = R.drawable.ic_fb_mini
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_bumble, appName = context.getString(R.string.bumble), icon = R.drawable.ic_bumble
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_zoom, appName = context.getString(R.string.txt_zoom), icon = R.drawable.icon_zoom
            )
        )
        return appList
    }

    fun getGameAppsList(context: Context): ArrayList<AppModel> {
        val appList = ArrayList<AppModel>()
        appList.add(
            AppModel(
                id = R.id.fb, appName = context.getString(R.string.facebook), icon = R.drawable.ic_fb
            )
        )
        appList.add(
            AppModel(
                id = R.id.twitter, appName = context.getString(R.string.twitter), icon = R.drawable.ic_twitter
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_discovery, appName = context.getString(R.string.discovery), icon = R.drawable.ic_discovery
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_amazon, appName = context.getString(R.string.amazon), icon = R.drawable.ic_amazon
            )
        )
        appList.add(
            AppModel(
                id = R.id.linkedin, appName = context.getString(R.string.linkedin), icon = R.drawable.ic_linkdin
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_zoom, appName = context.getString(R.string.txt_zoom), icon = R.drawable.icon_zoom
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_whatsapp, appName = context.getString(R.string.whatsapp), icon = R.drawable.ic_whatsapp
            )
        )

        appList.add(
            AppModel(
                id = R.id.app_indeed, appName = context.getString(R.string.txt_indeed), icon = R.drawable.icon_app_indeed
            )
        )

        appList.add(
            AppModel(
                id = R.id.instagram, appName = context.getString(R.string.instagram), icon = R.drawable.ic_instagram
            )
        )

        return appList
    }

    fun getKeyboardAppsList(context: Context): ArrayList<AppModel> {
        val appList = ArrayList<AppModel>()

        appList.add(
            AppModel(
                id = R.id.app_whatsapp,
                appName = context.getString(R.string.whatsapp),
                icon = R.drawable.ic_whatsapp,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_amazon,
                appName = context.getString(R.string.amazon),
                icon = R.drawable.ic_amazon,
            )
        )
        appList.add(
            AppModel(
                id = R.id.twitter,
                appName = context.getString(R.string.twitter),
                icon = R.drawable.ic_twitter,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_snap_chat,
                appName = context.getString(R.string.snapchat),
                icon = R.drawable.ic_snapchat,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_urban_spoon,
                appName = context.getString(R.string.urban_spoon),
                icon = R.drawable.ic_urbanspoon,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_bumble,
                appName = context.getString(R.string.bumble),
                icon = R.drawable.ic_bumble,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_pandora_radio,
                appName = context.getString(R.string.pandora_radio),
                icon = R.drawable.ic_pandora,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_air_bnb,
                appName = context.getString(R.string.txt_airbnb),
                icon = R.drawable.icon_airbnb,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_yelp,
                appName = context.getString(R.string.yelp),
                icon = R.drawable.ic_yelp,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_google_meet,
                appName = context.getString(R.string.txt_google_meet),
                icon = R.drawable.icon_app_google_meet,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_espn,
                appName = context.getString(R.string.espn_score_center),
                icon = R.drawable.ic_espn,
            )
        )
        appList.add(
            AppModel(
                id = R.id.app_netflix, appName = context.getString(R.string.txt_netflix), icon = R.drawable.icon_netflix
            )
        )
        return appList
    }

    fun getCoolTvAppsList(): ArrayList<CoolTvAppsModel> {
        val appList = ArrayList<CoolTvAppsModel>()
        appList.add(CoolTvAppsModel(buttonImage = R.drawable.icon_doller_full, name = R.string.txt_click_video_shop))
        appList.add(CoolTvAppsModel(buttonImage = R.drawable.icon_link_full, name = R.string.txt_link))
        appList.add(CoolTvAppsModel(buttonImage = R.drawable.icon_entertain_full, name = R.string.txt_entertain))
        appList.add(CoolTvAppsModel(buttonImage = R.drawable.icon_call_full, name = R.string.txt_cool_Ecall))
        appList.add(CoolTvAppsModel(buttonImage = R.drawable.icon_interact_full, name = R.string.txt_interact))
        appList.add(CoolTvAppsModel(buttonImage = R.drawable.icon_bid_full, name = R.string.txt_bid))

        return appList
    }


    fun getReactionList(): ArrayList<ReactionModel> {
        val list = ArrayList<ReactionModel>()
        list.add(ReactionModel(profileImage = R.drawable.ic_reaction_1, name = "Stena Johns", message = "Looking Awesome"))
        list.add(
            ReactionModel(
                profileImage = R.drawable.ic_reaction_2, name = "Andrew Marks", message = "Hey, How Are You !"
            )
        )
        list.add(
            ReactionModel(
                profileImage = R.drawable.ic_reaction_3, name = "Kate Villesion", message = "Waoooo ! Amazing"
            )
        )
        list.add(ReactionModel(profileImage = R.drawable.ic_reaction_1, name = "Andrew Jons", message = "I am Good !"))
        list.add(ReactionModel(profileImage = R.drawable.ic_reaction_1, name = "John Smith", message = "Amazing App 😍"))

        return list
    }

    fun getBidList(): ArrayList<Bid> {
        val list = ArrayList<Bid>()
        list.add(Bid(name = "Stena Johns", amount = "90"))
        list.add(Bid(name = "Andrew Marks", amount = "95"))
        list.add(Bid(name = "Kate Villesion", amount = "107"))
        list.add(Bid(name = "John Smith", amount = "140"))
        list.add(Bid(name = "Stena Johns", amount = "150"))
        list.add(Bid(name = "Andrew Marks", amount = "160"))
        list.add(Bid(name = "John Smith", amount = "190"))
        list.add(Bid(name = "Stena Johns", amount = "200"))
        list.add(Bid(name = "Andrew Marks", amount = "245"))
        list.add(Bid(name = "Kate Villesion", amount = "250"))
        list.add(Bid(name = "John Smith", amount = "268"))
        list.add(Bid(name = "Stena Johns", amount = "280"))
        list.add(Bid(name = "Kate Villesion", amount = "300"))
        list.add(Bid(name = "Alex", amount = "400"))
        return list
    }

    fun getBidTimes(): ArrayList<String> {
        val list = arrayListOf<String>()
        list.add("00:19")
        list.add("00:21")
        list.add("00:26")
        list.add("00:32")
        list.add("00:43")
        list.add("00:50")
        list.add("00:58")
        list.add("01:00")
        list.add("01:25")
        list.add("01:30")
        list.add("01:45")
        list.add("02:20")
        list.add("02:55")
        list.add("03:05")
        return list
    }
}
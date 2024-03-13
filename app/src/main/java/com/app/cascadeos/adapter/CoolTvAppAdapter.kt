package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ItemCooltvAppBinding
import com.app.cascadeos.model.CoolTvAppsModel

class CoolTvAppAdapter(
    val context: Context,
    private var appList: ArrayList<CoolTvAppsModel>,
    private var onAppClick: ((itemModel: CoolTvAppsModel, position: Int) -> Unit),
) : RecyclerView.Adapter<CoolTvAppAdapter.CoolTvAppViewHolder>() {

    inner class CoolTvAppViewHolder(val binding: ItemCooltvAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoolTvAppViewHolder {
        return CoolTvAppViewHolder(
            ItemCooltvAppBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun getItemCount() = appList.size

    override fun onBindViewHolder(holder: CoolTvAppViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                tvCoolTvApp.text = context.getString(appList[position].name)
                imgCoolTvApp.setImageResource(appList[position].buttonImage)
                imgCoolTvApp.setOnClickListener {
                    if (!appList[absoluteAdapterPosition].isSelected) {
                        onAppClick(appList[absoluteAdapterPosition], absoluteAdapterPosition)
                    }
                }
                when (position) {
                    0 -> {
                        imgCoolTvApp.setImageResource(if (appList[position].isSelected) R.drawable.icon_doller_pressed else R.drawable.icon_doller_full)
                    }

                    1 -> {
                        imgCoolTvApp.setImageResource(if (appList[position].isSelected) R.drawable.icon_link_pressed else R.drawable.icon_link_full)
                    }

                    2 -> {
                        imgCoolTvApp.setImageResource(if (appList[position].isSelected) R.drawable.icon_entertain_pressed else R.drawable.icon_entertain_full)
                    }

                    3 -> {
                        imgCoolTvApp.setImageResource(if (appList[position].isSelected) R.drawable.icon_call_pressed else R.drawable.icon_call_full)
                    }

                    4 -> {
                        imgCoolTvApp.setImageResource(if (appList[position].isSelected) R.drawable.icon_interact_pressed else R.drawable.icon_interact_full)
                    }

                    5 -> {
                        imgCoolTvApp.setImageResource(if (appList[position].isSelected) R.drawable.icon_bid_pressed else R.drawable.icon_bid_full)
                    }
                }

                /*if (position == 0 && appList[position].isSelected) {
                    imgCoolTvApp.setImageResource(R.drawable.icon_dollar_pressed)
                } else if (position == 0 && !appList[position].isSelected) {
                    imgCoolTvApp.setImageResource(R.drawable.icon_doller_full)
                }*/
            }
        }
    }
}

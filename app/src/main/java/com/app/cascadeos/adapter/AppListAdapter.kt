package com.app.cascadeos.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ItemAppIconBinding
import com.app.cascadeos.model.AppModel
import java.util.ArrayList

class AppListAdapter(
    private val mDataList: ArrayList<AppModel>,
    private val mListener: ItemClickListener,
    private val isGames: Boolean = false,
    private val gameUrl: String = ""
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedIndex = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AppIconListHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_app_icon,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppIconListHolder) {
            holder.mBinding.apply {
                with(mDataList[position]) {
                    tvAppName.text = appName
                    ivAppIcon.setImageResource(icon)
                    layoutMain.setOnClickListener {
                        mListener.onAppIconClick(appUrl)
                        selectedIndex = holder.absoluteAdapterPosition
                        notifyDataSetChanged()
                    }

                    if (isGames) {
                        if (selectedIndex == -1 && gameUrl != "") {
                            if (gameUrl == appUrl) {
                                selectedIndex = holder.absoluteAdapterPosition
                                ivAppIcon.setBackgroundResource(R.drawable.bg_selected_border_shape)
                            } else {
                                ivAppIcon.background = null
                            }
                        }
                        if (selectedIndex == holder.absoluteAdapterPosition) {
                            ivAppIcon.setBackgroundResource(R.drawable.bg_selected_border_shape)
                        } else {
                            ivAppIcon.background = null
                        }
                    }
                }
            }
        }
    }

    inner class AppIconListHolder(itemView: ItemAppIconBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val mBinding = itemView
    }


    interface ItemClickListener {
        fun onAppIconClick(appUrl: String?)
    }
}
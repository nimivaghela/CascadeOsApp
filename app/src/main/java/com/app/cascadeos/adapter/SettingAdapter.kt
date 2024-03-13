package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil.getBinding
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.databinding.ItemSettingBinding
import com.app.cascadeos.model.SettingModel

class SettingAdapter(val context: Context,
                     var onSettingClickListener: SettingItemClicked  , val list: List<SettingModel>):
    RecyclerView.Adapter<SettingAdapter.SettingViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.bindTo(position)
    }

    inner class SettingViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root)  {
        private var mBinding = getBinding<ItemSettingBinding>(itemView)

        fun bindTo(pos: Int) {
            mBinding?.executePendingBindings()
            var upperSectionModel = list[pos]
            mBinding?.tvSettingItem?.text = upperSectionModel.name
            mBinding?.imgSetting?.setImageResource(upperSectionModel.image)
            mBinding?.tvSettingDescribtion?.text = upperSectionModel.shortDescription

            mBinding?.clSetting?.setOnClickListener {
                onSettingClickListener.onSettingItemClicked(pos, upperSectionModel)
            }
           /* mBinding.btnHomeUpperSectionVolunteer.setOnClickListener {
                upperSectionClickListener.onHomeUpperSectionItemClicked(pos,upperSectionModel)
            }*/
        }
    }

    interface SettingItemClicked {
        fun onSettingItemClicked(position: Int, settingModel: SettingModel)
    }
}
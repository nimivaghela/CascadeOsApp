package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil.getBinding
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.databinding.ItemCooltvAppBinding
import com.app.cascadeos.databinding.ItemEcallListBinding
import com.app.cascadeos.databinding.ItemSettingBinding
import com.app.cascadeos.model.CoolEcallModel
import com.app.cascadeos.model.SettingModel

class CoolEcallAdapter(val context: Context,
                       var onCoolEcallClickListener: CoolEcallItemClicked, val list: List<CoolEcallModel>):
    RecyclerView.Adapter<CoolEcallAdapter.CoolEcallViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoolEcallViewHolder {
        val binding = ItemEcallListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoolEcallViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: CoolEcallViewHolder, position: Int) {
        holder.bindTo(position)
    }

    inner class CoolEcallViewHolder(private val binding: ItemEcallListBinding) :
        RecyclerView.ViewHolder(binding.root)  {
        private var mBinding = getBinding<ItemEcallListBinding>(itemView)

        fun bindTo(pos: Int) {
            mBinding?.executePendingBindings()
            var upperSectionModel = list[pos]
            mBinding?.tvCoolECallName?.text = upperSectionModel.name
            mBinding?.imgCoolEcall?.setImageResource(upperSectionModel.image)
            mBinding?.tvUserNumber?.text = upperSectionModel.phoneNumber


            mBinding?.clCoolEcallList?.setOnClickListener {
                onCoolEcallClickListener.onCoolEcallItemClicked(absoluteAdapterPosition, upperSectionModel)
            }
           /* mBinding.btnHomeUpperSectionVolunteer.setOnClickListener {
                upperSectionClickListener.onHomeUpperSectionItemClicked(pos,upperSectionModel)
            }*/
        }
    }

    interface CoolEcallItemClicked {
        fun onCoolEcallItemClicked(position: Int, coolEcallModel: CoolEcallModel)
    }
}
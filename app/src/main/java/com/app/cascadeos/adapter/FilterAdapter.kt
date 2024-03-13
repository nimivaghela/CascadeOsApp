package com.app.cascadeos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.LayoutItemFilterBinding
import com.app.cascadeos.model.FilterModel
import com.otaliastudios.cameraview.filter.Filters

class FilterAdapter :  RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

    private val dataList = ArrayList<FilterModel>()

    init {

        Filters.values().forEachIndexed { index, filters ->
         val filter = FilterModel(id = index, name = filters.name, filter = filters.newInstance())
         dataList.add(filter)
        }

    }

    class ViewHolder(private val itemBinding: LayoutItemFilterBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(filterModel: FilterModel) {
            //itemBinding.cameraView.filter = filterModel.filter
            itemBinding.executePendingBindings()
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding: LayoutItemFilterBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.layout_item_filter,
            parent,
            false
        )
        return ViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
       return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }
}
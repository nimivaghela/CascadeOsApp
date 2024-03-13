package com.app.cascadeos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ItemHomeScreenAppBinding
import com.app.cascadeos.model.AppModel

class HomeScreenAppListAdapter(
    private val dataList: ArrayList<AppModel>,
    var onLongClick: ((itemModel: AppModel, position: Int) -> Unit),
) :
    RecyclerView.Adapter<HomeScreenAppListAdapter.ViewHolder>() {
    val appClickLiveData = MutableLiveData<View>()

    class ViewHolder(
        private val appClickLiveData: MutableLiveData<View>,
        val itemBinding: ItemHomeScreenAppBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(appModel: AppModel) {
            itemBinding.app.text = appModel.appName
            itemBinding.app.setCompoundDrawablesWithIntrinsicBounds(0, appModel.icon, 0, 0)
            itemBinding.app.setOnClickListener {
                appClickLiveData.value = it
            }
            itemBinding.app.id = appModel.id
            itemBinding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding: ItemHomeScreenAppBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_home_screen_app,
            parent,
            false
        )
        return ViewHolder(appClickLiveData, itemBinding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemBinding.app.setOnLongClickListener {
            onLongClick(dataList[holder.bindingAdapterPosition], holder.bindingAdapterPosition)
            true
        }
        holder.bind(dataList[position])
    }

    fun addItem(item: AppModel, position: Int): ArrayList<AppModel> {
        for (i in dataList.indices) {
            if (dataList[i].appName == item.appName) {
                dataList.removeAt(i)
                notifyItemRemoved(i)
                break
            }
        }
        dataList.add(position, item)
        notifyItemInserted(position)
        return dataList
    }

    fun removeItemFromList(item: AppModel): ArrayList<AppModel> {
        val indexRemoved = dataList.indexOf(item)
        dataList.removeAt(indexRemoved)
        notifyItemRemoved(indexRemoved)
        return dataList
    }

}
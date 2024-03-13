package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ItemMulticastAppBinding
import com.app.cascadeos.model.AppModel

class MulticastAppAdapter(
    val context: Context,
    private var appList: ArrayList<AppModel>,
    private var onAppClick: ((itemModel: AppModel, view: View) -> Unit),
    private var onRemove: ((appList: ArrayList<AppModel>) -> Unit),
) : RecyclerView.Adapter<MulticastAppAdapter.MulticastAppViewHolder>() {

    inner class MulticastAppViewHolder(val binding: ItemMulticastAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MulticastAppViewHolder {
        return MulticastAppViewHolder(
            ItemMulticastAppBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = appList.size

    override fun onBindViewHolder(holder: MulticastAppViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                tvMulticastApp.text = appList[position].appName
                imgIconMulticastApp.id = appList[position].id
                imgIconMulticastApp.setImageResource(appList[position].icon)
                imgIconMulticastApp.setOnClickListener {
                    if (imgAppIconClose.visibility == View.VISIBLE) {
                        removeThisApp(binding, absoluteAdapterPosition)
                    } else {
                        onAppClick(appList[absoluteAdapterPosition], it)
                    }
                }
                imgIconMulticastApp.setOnLongClickListener {
                    if (imgAppIconClose.visibility == View.VISIBLE) {
                        imgAppIconClose.visibility = View.INVISIBLE
                    } else {
                        imgAppIconClose.visibility = View.VISIBLE
                    }
                    true
                }
                imgAppIconClose.setOnClickListener {
                    removeThisApp(binding, absoluteAdapterPosition)
                }

            }
        }
    }

    private fun removeThisApp(binding: ItemMulticastAppBinding, absoluteAdapterPosition: Int) {
        if (binding.imgAppIconClose.visibility == View.VISIBLE) {
            val alertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.txt_remove_app_title)
                .setPositiveButton(context.getString(R.string.txt_yes)) { _, _ ->
                    yesClicked(
                        binding,
                        absoluteAdapterPosition
                    )
                }
                .setNegativeButton(context.getString(R.string.txt_no)) { _, _ ->noClicked(binding) }
                .create()
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(context, R.color.teal_200))
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(context, R.color.teal_200))
            }
            alertDialog.show()
        }
    }

    private fun noClicked(binding: ItemMulticastAppBinding) {
        binding.imgAppIconClose.visibility = View.INVISIBLE
    }


    private fun yesClicked(binding: ItemMulticastAppBinding, position: Int) {
        binding.imgAppIconClose.visibility = View.INVISIBLE
        appList.removeAt(position)
        notifyDataSetChanged()
        onRemove(appList)
    }

    fun addItem(itemModel: AppModel, position: Int): ArrayList<AppModel> {
        for (i in appList.indices) {
            if (appList[i].appName == itemModel.appName) {
                appList.removeAt(i)
                notifyItemRemoved(i)
                break
            }
        }
        appList.add(position, itemModel)
        notifyItemInserted(position)
        return appList
    }
}
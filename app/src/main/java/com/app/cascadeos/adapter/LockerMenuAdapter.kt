package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ItemDigitalLockerOptionsBinding
import com.app.cascadeos.model.DigitalLockerMenuModel
import com.app.cascadeos.utility.loadImage

class LockerMenuAdapter(
    val context: Context,
    private var menuItemList: ArrayList<DigitalLockerMenuModel>,
    private var onMenuItemClick: ((itemModel: DigitalLockerMenuModel) -> Unit),
) : RecyclerView.Adapter<LockerMenuAdapter.LockerMenuViewHolder>() {
    var selectedItemPosition: Int = 0

    init {
        for (i in menuItemList.indices) {
            if (menuItemList[i].isSelected) {
                selectedItemPosition = i
                onMenuItemClick(menuItemList[selectedItemPosition])
            }
        }
    }

    class LockerMenuViewHolder(val binding: ItemDigitalLockerOptionsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockerMenuViewHolder {
        return LockerMenuViewHolder(ItemDigitalLockerOptionsBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun getItemCount() = menuItemList.size

    override fun onBindViewHolder(holder: LockerMenuViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                imgMenuOption.loadImage(
                    image = menuItemList[bindingAdapterPosition].image,
                    onStart = {},
                    onSuccess = { _, _ -> },
                    onError = { _, _ -> })
                imgMenuOption.background = if (selectedItemPosition == bindingAdapterPosition) {
                    ContextCompat.getDrawable(context, R.drawable.ic_locker_app_selector)
                } else null
                imgMenuOption.setOnClickListener {
                    onMenuItemClick(menuItemList[position])
                    notifyItemChanged(bindingAdapterPosition)
                    notifyItemChanged(selectedItemPosition)
                    selectedItemPosition = bindingAdapterPosition
                }
            }
        }
    }
}
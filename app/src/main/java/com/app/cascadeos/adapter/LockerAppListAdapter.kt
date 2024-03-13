package com.app.cascadeos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.adapter.LockerAppListAdapter.Const.hasButton
import com.app.cascadeos.adapter.LockerAppListAdapter.Const.noButton
import com.app.cascadeos.databinding.ItemLockerRecyclerviewBinding
import com.app.cascadeos.databinding.ItemLockerWithoutButtonsBinding
import com.app.cascadeos.model.DigitalLockerItemModel
import com.app.cascadeos.model.HasViewButton
import com.app.cascadeos.model.MediaType
import com.app.cascadeos.utility.loadImage

class LockerAppListAdapter(
    val context: Context,
    private var appList: ArrayList<DigitalLockerItemModel>,
    private var mClickListener: ClickListener,
    private var onPlayClick: ((itemModel: DigitalLockerItemModel) -> Unit),
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private object Const {
        const val hasButton = 0 // random unique value
        const val noButton = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == hasButton) {
            AppViewHolderWithButton(ItemLockerRecyclerviewBinding.inflate(LayoutInflater.from(context), parent, false))
        } else {
            AppViewHolderWithoutButtons(ItemLockerWithoutButtonsBinding.inflate(LayoutInflater.from(context), parent, false))
        }
    }

    override fun getItemCount() = appList.size

    override fun getItemViewType(position: Int): Int {
        return if (appList[position].hasViewButton == HasViewButton.TRUE) hasButton else noButton
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == hasButton) {
            (holder as AppViewHolderWithButton).bind(appList[position])
        } else {
            (holder as AppViewHolderWithoutButtons).bind(appList[position])
        }
    }

    fun addItems(list: ArrayList<DigitalLockerItemModel>) {
        if (appList.isNotEmpty()) {
            appList.clear()
        }
        appList.addAll(list)
        notifyDataSetChanged()
    }

    inner class AppViewHolderWithButton(val binding: ItemLockerRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(itemModel: DigitalLockerItemModel) {
            binding.apply {
                imgListWithButton.loadImage(
                    image = itemModel.thumbnail,
                    allowCaching = true,
                    onStart = {},
                    onSuccess = { _, _ -> },
                    onError = { _, _ -> })
                btnPlayOnDevice.setOnClickListener { onPlayClick(itemModel) }
                btnBeamToTv.setOnClickListener { mClickListener.onBeamToTvClick() }
                if (itemModel.mediaType == MediaType.FILES || itemModel.mediaType == MediaType.PHOTOS) {
                    btnPlayOnDevice.setText(R.string.view_on_device)
                } else {
                    btnPlayOnDevice.setText(R.string.play_on_device)
                }
                imgListWithButton.setOnClickListener { onPlayClick(itemModel) }
            }
        }
    }

    inner class AppViewHolderWithoutButtons(val binding: ItemLockerWithoutButtonsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(itemModel: DigitalLockerItemModel) {
            binding.apply {
                //imgList.setImageURI(Uri.parse(itemModel.thumbnail))
                imgList.loadImage(image = itemModel.thumbnail, onStart = {}, onSuccess = { _, _ -> }, onError = { _, _ -> })
                imgList.setOnClickListener { onPlayClick(itemModel) }
                tvImage.text = itemModel.itemName.ifEmpty { "" }
            }
        }
    }

    interface ClickListener{
        fun onBeamToTvClick()
    }
}
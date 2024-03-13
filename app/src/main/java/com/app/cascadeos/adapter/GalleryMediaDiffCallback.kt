package com.app.cascadeos.adapter

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.app.cascadeos.model.GalleryMedia
import com.app.cascadeos.model.Media

class GalleryMediaDiffCallback : DiffUtil.ItemCallback<GalleryMedia>() {
    override fun areItemsTheSame(oldItem: GalleryMedia, newItem: GalleryMedia): Boolean =
        oldItem.uriImage == newItem.uriImage


    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: GalleryMedia, newItem: GalleryMedia): Boolean =
        oldItem == newItem


}

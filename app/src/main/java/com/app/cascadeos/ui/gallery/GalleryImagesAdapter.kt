package com.app.cascadeos.ui.gallery

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import com.app.cascadeos.databinding.ItemGalleryImageListBinding
import com.app.cascadeos.model.GalleryMedia


class GalleryImagesAdapter(
    private val galleryImageClickListener: GalleryImageClickListener
) :
    RecyclerView.Adapter<GalleryImagesAdapter.ViewHolder>() {
    var galleryImageList = ArrayList<GalleryMedia?>()
    var galleryImageId = ArrayList<Long?>()
    lateinit var appContext: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBindingUtil =
            ItemGalleryImageListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBindingUtil)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        appContext = holder.itemView.context
        val data: GalleryMedia? = galleryImageList[holder.bindingAdapterPosition]
        val uri = galleryImageList[holder.bindingAdapterPosition]?.uriImage

        if (data?.mimeType == "video/mp4") {
            holder.item.ivPlay.visibility = View.VISIBLE
            holder.item.ivGalleryImage.load(data.imagePath) {
                decoderFactory { result, options, _ ->
                    VideoFrameDecoder(
                        result.source,
                        options
                    )
                }
            }
            holder.item.ivGalleryImage.visibility = View.VISIBLE
        } else {
            holder.item.ivGalleryImage.load(data?.uriImage) {
                holder.item.ivPlay.visibility = View.GONE
                /*  holder.item.ivGalleryImage.loadImageProfile(
                      uri,
                      R.drawable.ic_photo
                  )*/
            }
        }
        /* holder.item.ivGalleryImage.load(data?.uriImage) {
             if (data?.mimeType == "video/mp4") {
                 holder.item.ivPlay.visibility = View.VISIBLE
                 val filePath = data.imagePath
 //                Glide
 //                    .with(appContext)
 //                    .asBitmap()
 //                    .load(Uri.fromFile(File(filePath)))
 //                    .into(holder.item.ivGalleryImage)
                 holder.item.ivGalleryImage.load(data.uriImage) {
                     decoderFactory { result, options, _ ->
                         VideoFrameDecoder(
                             result.source,
                             options
                         )
                     }
                 }
                 holder.item.ivGalleryImage.visibility = View.VISIBLE
             } else {
                 holder.item.ivPlay.visibility = View.GONE
                 holder.item.ivGalleryImage.loadImageProfile(
                     uri,
                     R.drawable.ic_photo
                 )
             }
         }*/

        holder.item.cardHostImage.setCardBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                android.R.color.transparent
            )
        )

        holder.itemView.setOnClickListener {
            if (uri != null) {
                galleryImageClickListener.onGalleryImageClick(uri, holder.bindingAdapterPosition)
            } else {
                galleryImageClickListener.onCameraClick()
            }
        }
    }

    override fun getItemCount(): Int {
        return galleryImageList.size
    }

    class ViewHolder(view: ItemGalleryImageListBinding) : RecyclerView.ViewHolder(view.root) {
        val item = view
    }

    interface GalleryImageClickListener {
        fun onGalleryImageClick(uri: Uri, position: Int)
        fun onCameraClick()
    }
}
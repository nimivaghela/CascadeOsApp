package com.app.cascadeos.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import com.app.cascadeos.R
import com.app.cascadeos.model.GalleryMedia
import com.app.cascadeos.utility.layoutInflater
import kotlinx.coroutines.launch


/**
 * This is an adapter to preview taken photos or videos
 * */
class GalleryMediaAdapter(
    private val onItemClick: (Boolean, Uri) -> Unit,
    private val onDeleteClick: (Boolean, Uri) -> Unit,
) : ListAdapter<GalleryMedia, GalleryMediaAdapter.PicturesViewHolder>(GalleryMediaDiffCallback()) {

    private lateinit var appContext: Context
    lateinit var player : ExoPlayer

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        player = ExoPlayer.Builder(recyclerView.context).build()
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PicturesViewHolder(
            parent.context.layoutInflater.inflate(
                R.layout.item_picture,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: PicturesViewHolder, position: Int) {
        appContext = holder.itemView.context
        holder.bind(getItem(position))
    }

    fun shareImage(currentPage: Int, action: (GalleryMedia) -> Unit) {
        if (currentPage < itemCount) {
            action(getItem(currentPage))
        }
    }

    fun deleteImage(currentPage: Int) {
        if (currentPage < itemCount) {
            val media = getItem(currentPage)
            val allMedia = currentList.toMutableList()
            allMedia.removeAt(currentPage)
            submitList(allMedia)
            onDeleteClick(allMedia.size == 0, media.uriImage)
        }
    }

    inner class PicturesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
         val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
         val imagePlay: ImageView = itemView.findViewById(R.id.imagePlay)
         val videoView: PlayerView = itemView.findViewById(R.id.video_viewer)

        @SuppressLint("UnsafeOptInUsageError")
        fun bind(item: GalleryMedia) {
            imagePlay.visibility = if (item.mimeType == "video/mp4") View.VISIBLE else View.GONE
            if (item.mimeType == "video/mp4") {
                imagePlay.visibility = View.VISIBLE
                imagePreview.load(item.imagePath) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(
                            result.source,
                            options
                        )
                    }
                }
                videoView.visibility = View.GONE
                imagePreview.visibility = View.VISIBLE
            } else {
                imagePreview.load(item.uriImage) {
                    videoView.visibility = View.GONE
                    imagePreview.visibility = View.VISIBLE
                }
            }

            videoView.setShowNextButton(false)
            videoView.setShowPreviousButton(false)

            imagePreview.setOnClickListener {
                if (item.mimeType != "video/mp4") {
                    onItemClick(false, item.uriImage)
                } else {
                    imagePlay.visibility = View.GONE
                    videoView.visibility = View.VISIBLE
                    imagePreview.visibility = View.GONE
                    playVideo(item.uriImage, videoView, item.imagePath)
                    onItemClick(true, item.uriImage)
                }
            }


            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if(playbackState == Player.STATE_ENDED){
                        imagePlay.visibility = View.VISIBLE
                        imagePreview.load(item.imagePath) {
                            decoderFactory { result, options, _ ->
                                VideoFrameDecoder(
                                    result.source,
                                    options
                                )
                            }
                        }
                        videoView.visibility = View.GONE
                        imagePreview.visibility = View.VISIBLE
                    }
                    super.onPlaybackStateChanged(playbackState)
                }
            })
        }
    }

    private fun playVideo(fileUri: Uri, videoView: PlayerView, path: String) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            showVideo(videoView, path)
        } else {
            // force MediaScanner to re-scan the media file.
            val filePath = getAbsolutePathFromUri(fileUri) ?: return
            MediaScannerConnection.scanFile(
                appContext, arrayOf(filePath), null
            ) { _, uri ->
                // playback video on main thread with VideoView
                if (uri != null) {
                    (appContext as AppCompatActivity).lifecycleScope.launch {
                        showVideo(videoView, path)
                    }
                }
            }
        }
    }

    private fun getAbsolutePathFromUri(contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = appContext.contentResolver
                .query(contentUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            if (cursor == null) {
                return null
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: RuntimeException) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun showVideo(videoView: PlayerView, path: String) {
        videoView.player = player
        val mediaItem = MediaItem.fromUri(path)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }


    override fun onViewDetachedFromWindow(holder: PicturesViewHolder) {
        val item = getItem(holder.absoluteAdapterPosition)
        if (item.mimeType == "video/mp4") {
            holder.imagePlay.visibility = View.VISIBLE
            holder.imagePreview.load(getItem(holder.absoluteAdapterPosition).imagePath) {
                decoderFactory { result, options, _ ->
                    VideoFrameDecoder(
                        result.source,
                        options
                    )
                }
            }
            holder.videoView.visibility = View.GONE
            holder.imagePreview.visibility = View.VISIBLE
        }
        player.stop()
        player.removeMediaItem(0)
        super.onViewDetachedFromWindow(holder)
    }


    fun releasePlayer(){
        player.release()
    }
}

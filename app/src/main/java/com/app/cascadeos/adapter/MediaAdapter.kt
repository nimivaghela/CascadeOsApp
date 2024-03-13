package com.app.cascadeos.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.app.cascadeos.model.Media
import com.app.cascadeos.utility.layoutInflater
import kotlinx.coroutines.launch


/**
 * This is an adapter to preview taken photos or videos
 * */
class MediaAdapter(
    private val onItemClick: (Boolean, Uri) -> Unit,
    private val onDeleteClick: (Boolean, Uri) -> Unit,
) : ListAdapter<Media, MediaAdapter.PicturesViewHolder>(MediaDiffCallback()) {

    lateinit var appContext: Context
    lateinit var player: ExoPlayer

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

    fun shareImage(currentPage: Int, action: (Media) -> Unit) {
        if (currentPage < itemCount) {
            action(getItem(currentPage))
        }
    }

    private fun getImageSize(): Int {
        val density: Int = appContext.resources.displayMetrics.densityDpi
        var size = 100
        when (density) {
            DisplayMetrics.DENSITY_LOW -> size = 100
            DisplayMetrics.DENSITY_MEDIUM -> size = 100
            DisplayMetrics.DENSITY_HIGH -> size = 150
        }
        return size
    }

    fun deleteImage(currentPage: Int) {
        if (currentPage < itemCount) {
            val media = getItem(currentPage)
            val allMedia = currentList.toMutableList()
            allMedia.removeAt(currentPage)
            submitList(allMedia)
            onDeleteClick(allMedia.size == 0, media.uri)
        }
    }

    inner class PicturesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        val imagePlay: ImageView = itemView.findViewById(R.id.imagePlay)
        val videoView: PlayerView = itemView.findViewById(R.id.video_viewer)

        @SuppressLint("UnsafeOptInUsageError")
        fun bind(item: Media) {
            imagePlay.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            if (item.isVideo) {
                imagePlay.visibility = View.VISIBLE
                imagePreview.load(item.uri) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(
                            result.source,
                            options
                        )
                    }
                }
            } else {
                var size = getImageSize()

                imagePreview.load(item.uri) {
                    videoView.visibility = View.GONE
                    imagePreview.visibility = View.VISIBLE
                }
            }

            videoView.setShowNextButton(false)
            videoView.setShowPreviousButton(false)

            imagePreview.setOnClickListener {
                if (!item.isVideo) {
                    onItemClick(item.isVideo, item.uri)
                } else {
                    videoView.visibility = View.VISIBLE
                    imagePreview.visibility = View.GONE
                    imagePlay.visibility = View.GONE
                    playVideo(item.uri, videoView)

                }
            }


            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        imagePlay.visibility = View.VISIBLE
                        imagePreview.load(item.uri) {
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

    private fun playVideo(uri: Uri, videoView: PlayerView) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            showVideo(uri, videoView)
        } else {
            // force MediaScanner to re-scan the media file.
            val path = getAbsolutePathFromUri(uri) ?: return
            MediaScannerConnection.scanFile(
                appContext, arrayOf(path), null
            ) { _, uri ->
                // playback video on main thread with VideoView
                if (uri != null) {
                    (appContext as AppCompatActivity).lifecycleScope.launch {
                        showVideo(uri, videoView)
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
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    private fun showVideo(uri: Uri, videoView: PlayerView) {
        /*   val fileSize = getFileSizeFromUri(uri)
           if (fileSize == null || fileSize <= 0) {
               return
           }*/

        val filePath = getAbsolutePathFromUri(uri) ?: return


        videoView.player = player
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()


    }

    private fun getFileSizeFromUri(contentUri: Uri): Long? {
        val cursor = appContext.contentResolver
            .query(contentUri, null, null, null, null)
            ?: return null

        val sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        cursor.moveToFirst()

        cursor.use {
            return it.getLong(sizeIndex)
        }
    }

    override fun onViewDetachedFromWindow(holder: PicturesViewHolder) {

        val item = getItem(holder.absoluteAdapterPosition)
        if (item.isVideo) {
            holder.imagePlay.visibility = View.VISIBLE
            holder.imagePreview.load(getItem(holder.absoluteAdapterPosition).uri) {
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


    fun releasePlayer() {
        player.release()
    }


}

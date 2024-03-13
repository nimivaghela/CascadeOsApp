package com.app.cascadeos.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

class GalleryMedia() : Parcelable{
    var id: Long = 0
    var displayName: String = ""
    var imagePath: String = ""
    var dateAdded: String = ""
    var mimeType: String = ""
    lateinit var uriImage: Uri
    var isSelected: Boolean = false

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        displayName = parcel.readString().toString()
        imagePath = parcel.readString().toString()
        dateAdded = parcel.readString().toString()
        mimeType = parcel.readString().toString()
        uriImage = parcel.readParcelable(Uri::class.java.classLoader)!!
        isSelected = parcel.readByte() != 0.toByte()
    }

    constructor(
        id: Long,
        displayName: String,
        imagePath: String,
        dateAdded: String,
        mimeType: String,
        uriImage: Uri
    ) : this() {
        this.id = id
        this.displayName = displayName
        this.imagePath = imagePath
        this.dateAdded = dateAdded
        this.mimeType = mimeType
        this.uriImage = uriImage

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(displayName)
        parcel.writeString(imagePath)
        parcel.writeString(dateAdded)
        parcel.writeString(mimeType)
        parcel.writeParcelable(uriImage, flags)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GalleryMedia> {
        override fun createFromParcel(parcel: Parcel): GalleryMedia {
            return GalleryMedia(parcel)
        }

        override fun newArray(size: Int): Array<GalleryMedia?> {
            return arrayOfNulls(size)
        }
    }
}


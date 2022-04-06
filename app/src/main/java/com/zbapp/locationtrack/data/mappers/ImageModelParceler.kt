package com.zbapp.locationtrack.data.mappers

import android.os.Parcel
import com.zbapp.core.models.user.ImageModel
import kotlinx.parcelize.Parceler

object ImageModelParceler : Parceler<ImageModel> {
    override fun create(parcel: Parcel) = ImageModel(parcel.readString() ?: "")

    override fun ImageModel.write(parcel: Parcel, flags: Int) {
        parcel.writeString(image)
    }
}
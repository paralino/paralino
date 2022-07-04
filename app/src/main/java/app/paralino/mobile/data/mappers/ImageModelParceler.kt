package app.paralino.mobile.data.mappers

import android.os.Parcel
import app.paralino.core.models.base.ImageModel
import kotlinx.parcelize.Parceler

object ImageModelParceler : Parceler<ImageModel> {
    override fun create(parcel: Parcel) = ImageModel(parcel.readString() ?: "")

    override fun ImageModel.write(parcel: Parcel, flags: Int) {
        parcel.writeString(image)
    }
}
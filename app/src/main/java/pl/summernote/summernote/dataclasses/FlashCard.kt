package pl.summernote.summernote.dataclasses

import android.os.Parcel
import android.os.Parcelable

data class FlashCard(var front: String, var back: String) : Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(front)
        parcel.writeString(back)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FlashCard> {
        override fun createFromParcel(parcel: Parcel): FlashCard {
            return FlashCard(parcel)
        }

        override fun newArray(size: Int): Array<FlashCard?> {
            return arrayOfNulls(size)
        }
    }
}

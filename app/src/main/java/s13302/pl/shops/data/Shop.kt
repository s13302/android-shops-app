package s13302.pl.shops.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Shop(val shopId: String?, val userId: String, val name: String, val type: String, val photoUri: String?, val lat: Double?, val lng: Double?): Parcelable {
    constructor(): this(null, "", "", "", null, null, null)

}
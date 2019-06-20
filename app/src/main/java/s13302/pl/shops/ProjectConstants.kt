package s13302.pl.shops

import com.google.android.gms.maps.model.LatLng

class ProjectConstants {

    companion object {
        const val SELECT_PHOTO_REQUEST_ID_EDIT_SHOP = 34
        const val MAKE_PHOTO_REQUEST_ID_EDIT_SHOP = 578
        const val SELECT_PHOTO_RESULT_ID_REGISTRATION = 3
        const val IMAGES_LOCATION = "/images/"
        const val SHOPS_NODE_NAME = "/shops/"
        const val USERS_NODE_NAME = "/users/"
        const val SHOP_EXTRA = "SHOP_DATA"
        const val IMAGE_MIME_TYPE = "image/*"

        const val DEFAULT_MAP_ZOOM = 10f
        const val WARSAW_LAT = 52.267129
        const val WARSAW_LNG = 20.909832
        val WARSAW_LATLNG = LatLng(WARSAW_LAT, WARSAW_LNG)
        const val DEFAULT_GEOFENCE_RADIUS = 500f

        const val NOTIFICATION_CHANNEL_ID = "shopsChannel"
    }

}
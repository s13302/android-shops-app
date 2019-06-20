package s13302.pl.shops.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.maps.model.LatLng

class LocationListener(private val activity: Activity) : LocationListener {

    companion object {
        private const val MIN_TIME = 10L
        private const val MIN_DISTANCE = 10f
    }

    private var location: Location? = null
    private val context: Context = activity
    private lateinit var locationManager: LocationManager

    init {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 113)
        } else {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME,
                    MIN_DISTANCE,
                    this
                )
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else {

            }
        }
    }

    fun getLocation(): LatLng {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 113)
        }
        val immutableLocation = location
        if (immutableLocation != null) {
            return LatLng(immutableLocation.latitude, immutableLocation.longitude)
        }
        return LatLng(0.0, 0.0)
    }

    override fun onLocationChanged(location: Location?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 113)
        }
        if (this.location == null) {
            this.location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        this.location?.let {
        if (location != null && this.location != null) {
            if (location.accuracy > it.accuracy) {
                /*
                Wykrzykniki mogą dać NPE. Po reboot telefonu tracony jest cache lokalizacji
                 */
                this.location = it
            }
        }


        }
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

}
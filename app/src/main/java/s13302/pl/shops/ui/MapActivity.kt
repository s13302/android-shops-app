package s13302.pl.shops.ui

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_favourite_shops.*
import kotlinx.android.synthetic.main.activity_map.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R
import s13302.pl.shops.data.Shop
import s13302.pl.shops.item.ShopItem
import java.util.jar.Manifest

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MapActivity"
    }

    private lateinit var map: GoogleMap

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = fMap as SupportMapFragment
        mapFragment.getMapAsync(this)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        fetchShops()
    }

    override fun onMapReady(map: GoogleMap?) {

        if (map != null) {
            this.map = map
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ProjectConstants.WARSAW_LATLNG, ProjectConstants.DEFAULT_MAP_ZOOM))
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            }
        }
    }

    private fun fetchShops() {
        val userId = auth.uid
        val shops = database.getReference("${ProjectConstants.SHOPS_NODE_NAME}$userId")
        shops.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { dataSnapshot ->
                    dataSnapshot.getValue(Shop::class.java)?.let {
                        if (it.lat != null && it.lng != null) {
                            val position = LatLng(it.lat, it.lng)
                            map.addMarker(MarkerOptions()
                                .position(position)
                                .title(it.name))
                            map.addCircle(CircleOptions()
                                .center(position)
                                .radius(ProjectConstants.DEFAULT_GEOFENCE_RADIUS.toDouble())
                                .fillColor(R.color.colorAccent))
                        }
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "Error occured $p0")
            }

        })
    }
}

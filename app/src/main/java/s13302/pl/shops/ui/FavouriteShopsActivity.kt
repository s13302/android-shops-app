package s13302.pl.shops.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_favourite_shops.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R
import s13302.pl.shops.data.Shop
import s13302.pl.shops.item.ShopItem
import s13302.pl.shops.repository.DataListener
import s13302.pl.shops.repository.RepositoryRegister
import s13302.pl.shops.repository.impl.ShopRepositoryImpl
import s13302.pl.shops.service.GeofenceTransitionsIntentService

class FavouriteShopsActivity : AppCompatActivity(), DataListener<Shop> {

    companion object {
        private const val TAG = "FavouriteShopsActivity"
    }

    private lateinit var auth: FirebaseAuth
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favourite_shops)

        auth = FirebaseAuth.getInstance()
        RepositoryRegister.shopRepository = ShopRepositoryImpl(auth.uid ?: "")
        verifyUserIsLoggedIn()
        attachSwipeActions()

        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    override fun onResume() {
        super.onResume()
        RepositoryRegister.shopRepository.registerDataListener(this)
    }

    override fun onPause() {
        super.onPause()
        RepositoryRegister.shopRepository.unregisterDataListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.mnSignOut -> {
                auth.signOut()
                startRegisterActivity()
            }
            R.id.mnAddShop -> {
                startShopEditActivity(null)
            }
            R.id.mnMap -> {
                startMapActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDataChange(data: List<Shop>) {
        val adapter = GroupAdapter<ViewHolder>()
        data.forEach {shop ->
            val shopItem = ShopItem(shop)
            adapter.add(shopItem)
            add(shop, {
                Log.d(TAG, "Geofence ${shop.name} successfully registered")
            }, {
                Log.e(TAG, "Error when registering the Geofence for ${shop.name}: $it")
            })
        }
        rvShopsList.adapter = adapter
    }

    private fun verifyUserIsLoggedIn() {
        if (auth.uid == null) {
            startRegisterActivity()
        }
    }

    private fun startRegisterActivity() {
        val intent = Intent(this, RegistrationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun startMapActivity() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    private fun startShopEditActivity(shop: Shop?) {
        val intent = Intent(this@FavouriteShopsActivity, EditShopActivity::class.java)
        intent.putExtra(ProjectConstants.SHOP_EXTRA, shop)
        startActivity(intent)
    }

    private fun attachSwipeActions() {
        val itemTouchHelperCallback = object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                val adapter = rvShopsList.adapter
                if (adapter is GroupAdapter<*>) {
                    val adapterPosition = viewHolder.adapterPosition
                    val item = adapter.getItem(adapterPosition) as ShopItem

                    RepositoryRegister.shopRepository.removeShop(item.shop, OnSuccessListener {
                        Log.d(TAG, "Item ${item.shop.name} removed")
                        val removedMessage = applicationContext.getString(R.string.shop_removed, item.shop.name)
                        Snackbar
                            .make(rvShopsList, removedMessage, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_undo) {
                                Log.d(TAG, "Adding ${item.shop.name} again")
                                RepositoryRegister.shopRepository.addShop(item.shop, OnSuccessListener {
                                    adapter.add(adapterPosition, item)
                                })
                            }.show()
                    })
                    adapter.remove(item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(rvShopsList)
    }

    private fun add(shop: Shop, success: () -> Unit, failure: (error: String) -> Unit) {
        val geofence = buildGeofence(shop)
        if (geofence != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                geofencingClient
                    .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                    .addOnSuccessListener {
                        success()
                    }
                    .addOnFailureListener {
                        if (it is ApiException) {
                            val message = GeofenceStatusCodes.getStatusCodeString(it.statusCode)
                            failure("$message, number: ${it.statusCode}")
                        } else {
                            Log.e(TAG, "Error:", it)
                        }
                    }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 8349)
            }
        }
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(listOf(geofence))
            .build()
    }

    private fun buildGeofence(shop: Shop): Geofence? {
        if (shop.lat != null && shop.lng != null) {
            return Geofence.Builder()
                .setRequestId(shop.shopId)
                .setCircularRegion(shop.lat, shop.lng, ProjectConstants.DEFAULT_GEOFENCE_RADIUS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }
        return null
    }

}

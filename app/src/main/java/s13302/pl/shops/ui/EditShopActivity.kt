package s13302.pl.shops.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_shop.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R
import s13302.pl.shops.data.Shop
import s13302.pl.shops.repository.RepositoryRegister
import java.io.ByteArrayOutputStream
import java.util.*

class EditShopActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val TAG = "EditShopActivity"
    }

    private var bitmap: Bitmap? = null
    private var isPhotoChanged = false
    private var googleMap: GoogleMap? = null
    private var shopId: String? = null
    private var position: LatLng? = null
    private var oldPhotoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_shop)

        (mMap as SupportMapFragment).getMapAsync(this)
        init()
        btnSelectPhoto.setOnClickListener {
            addPhotoToShop()
        }
        btnSave.setOnClickListener {
            saveShop()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.apply {
            googleMap = this
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ProjectConstants.WARSAW_LATLNG, ProjectConstants.DEFAULT_MAP_ZOOM))
            position?.also {
                map.addMarker(MarkerOptions().position(it))
                map.addCircle(CircleOptions().center(it).radius(ProjectConstants.DEFAULT_GEOFENCE_RADIUS.toDouble()))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, ProjectConstants.DEFAULT_MAP_ZOOM))
            }
            map.setOnMapClickListener {
                map.clear()
                position = it
                map.addMarker(MarkerOptions().position(it))
                map.addCircle(CircleOptions().center(it).radius(ProjectConstants.DEFAULT_GEOFENCE_RADIUS.toDouble()))
            }
            if (ActivityCompat.checkSelfPermission(this@EditShopActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ProjectConstants.MAKE_PHOTO_REQUEST_ID_EDIT_SHOP -> handleResultFromCamera(data)
                ProjectConstants.SELECT_PHOTO_REQUEST_ID_EDIT_SHOP -> handleResultFromGallery(data)
            }
        }
    }

    private fun init() {
        val shop = intent.getParcelableExtra<Shop>(ProjectConstants.SHOP_EXTRA)
        if (shop != null) {
            shopId = shop.shopId
            etShopName.setText(shop.name)
            etShopType.setText(shop.type)
            oldPhotoUri = shop.photoUri
            Picasso.get().load(shop.photoUri).into(ciShopPhoto)
            btnSelectPhoto.alpha = 0f
            if (shop.lat != null && shop.lng != null) {
                position = LatLng(shop.lat, shop.lng)
            }
        }
    }

    private fun obtainShopLocation(): LatLng {
        val position = this.position
        if (position != null) {
            return position
        }
        val googleMap = this.googleMap
        if (googleMap != null) {
            return LatLng(googleMap.myLocation.latitude, googleMap.myLocation.longitude)
        }
        return LatLng(0.0, 0.0)
    }

    private fun saveShop() {
        val shopId = this.shopId ?: UUID.randomUUID().toString()
        val userId = FirebaseAuth.getInstance().uid
        val shopName = etShopName.text.toString()
        val shopType = etShopType.text.toString()
        val shopLocation = obtainShopLocation()

        if (userId != null) {
            uploadPhoto { photoUri ->
                val shopToSave = Shop(shopId, userId, shopName, shopType, photoUri, shopLocation.latitude, shopLocation.longitude)
                Log.d(TAG, "Trying to save shop: $shopToSave")
                RepositoryRegister.shopRepository.addShop(shopToSave, OnSuccessListener {
                    Log.d(TAG, "Successfully saved shop $shopName")
                    finish()
                })
            }
        }
    }

    private fun uploadPhoto(success: (photoUri: String) -> Unit) {
        if (isPhotoChanged) {
            val fileName = UUID.randomUUID().toString()
            val imageReference =
                FirebaseStorage.getInstance().getReference("${ProjectConstants.IMAGES_LOCATION}$fileName")
            val byteArrayOutputStream = ByteArrayOutputStream()
            val resizedBitmap = scaleBitmap(bitmap)
            resizedBitmap?.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
            val bitmapBytes = byteArrayOutputStream.toByteArray()
            imageReference.putBytes(bitmapBytes).addOnSuccessListener {
                imageReference.downloadUrl.addOnSuccessListener {
                    success(it.toString())
                }
            }
        } else {
            val photoUri = oldPhotoUri
            if (photoUri != null) {
                success(photoUri)
            }
        }
    }

    private fun scaleBitmap(srcBitmap: Bitmap?): Bitmap? {
        if (srcBitmap != null) {
            return Bitmap.createScaledBitmap(
                srcBitmap,
                ProjectConstants.DEFAULT_BITMAP_WIDTH,
                ProjectConstants.DEFAULT_BITMAP_HEIGHT,
                true
            )
        }
        return null
    }

    private fun handleResultFromCamera(data: Intent?) {
        Log.d(TAG, "Handle result from camera")
        val extras = data?.extras
        bitmap = extras?.get("data") as Bitmap
        showSelectedImage()
        isPhotoChanged = true
    }

    private fun handleResultFromGallery(data: Intent?) {
        Log.d(TAG, "Handle result from gallery")
        val uri = data?.data as Uri
        bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        showSelectedImage()
        isPhotoChanged = true
    }

    private fun showSelectedImage() {
        if (bitmap != null) {
            btnSelectPhoto.alpha = 0f
            ciShopPhoto.setImageBitmap(bitmap)
        }
    }

    private fun addPhotoToShop() {
        val actions = arrayOf(
            resources.getString(R.string.action_photo),
            resources.getString(R.string.action_gallery),
            resources.getString(R.string.action_cancel)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.select_photo)
            .setItems(actions) { dialogInterface, i ->
                handleAddPhotoToShopSelection(dialogInterface, i, actions)
            }
            .show()
    }

    private fun handleAddPhotoToShopSelection(dialogInterface: DialogInterface, i: Int, actions: Array<String>) {
        when (actions[i]) {
            actions[0] -> handleAddPhotoFromCamera()
            actions[1] -> handleAddPhotoFromGallery()
            actions[2] -> dialogInterface.dismiss()
        }
    }

    private fun handleAddPhotoFromCamera() {
        Log.d(TAG, "Make a brand new photo for a shop")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, ProjectConstants.MAKE_PHOTO_REQUEST_ID_EDIT_SHOP)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }
    }

    private fun handleAddPhotoFromGallery() {
        Log.d(TAG, "Select photo from gallery")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, ProjectConstants.SELECT_PHOTO_REQUEST_ID_EDIT_SHOP)
    }

}

package s13302.pl.shops.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_shop.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R
import s13302.pl.shops.data.Shop
import s13302.pl.shops.location.LocationListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class EditShopActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val TAG = "EditShopActivity"
    }

    private var photoUri: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var shop: Shop

    private lateinit var locationListener: LocationListener
    private var shopPosition: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_shop)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        locationListener = LocationListener(this)

        shop = intent.getParcelableExtra(ProjectConstants.SHOP_EXTRA) ?: Shop()
        initValues()
        (mMap as SupportMapFragment).getMapAsync(this)

        btnSave.setOnClickListener {
            saveShop()
        }
        btnSelectPhoto.setOnClickListener {
            pickPhoto()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        val lat = shop.lat
        val lng = shop.lng
        if (map != null) {
            if (lat != null && lng != null) {
                val position = LatLng(lat, lng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, ProjectConstants.DEFAULT_MAP_ZOOM * 1.5f))
                map.addMarker(MarkerOptions().position(position))
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(ProjectConstants.WARSAW_LATLNG, ProjectConstants.DEFAULT_MAP_ZOOM))
            }
            map.setOnMapClickListener {
                map.clear()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, ProjectConstants.DEFAULT_MAP_ZOOM * 1.5f))
                map.addMarker(MarkerOptions().position(it))
                shopPosition = it
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ProjectConstants.SELECT_PHOTO_REQUEST_ID_EDIT_SHOP -> {
                    photoUri = data?.data.toString()
                    hidePickPhotoButton()
                    Picasso.get().load(photoUri).into(ciShopPhoto)
                }
                ProjectConstants.MAKE_PHOTO_REQUEST_ID_EDIT_SHOP -> {
                    photoUri = intent.getStringExtra(MediaStore.EXTRA_OUTPUT)
//                    photoUri = intent.getParcelableExtra<File>(MediaStore.EXTRA_OUTPUT).toURI().toString()
                    hidePickPhotoButton()
                    Picasso.get().load(photoUri).into(ciShopPhoto)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun hidePickPhotoButton() {
        if (photoUri != null) {
            btnSelectPhoto.alpha = 0f
        }
    }

    private fun initValues() {
        etShopName.setText(shop.name)
        etShopType.setText(shop.type)
        photoUri = shop.photoUri
        hidePickPhotoButton()
        Picasso.get().load(photoUri).into(ciShopPhoto)

        val lat = shop.lat
        val lng = shop.lng
        if (lat != null && lng != null) {
            shopPosition = LatLng(lat, lng)
        }
    }

    private fun pickPhoto() {
        val options = arrayOf("Camera", "Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Select Action")
            .setItems(options) { dialogInterface, i ->
                when(options[i]) {
                    options[0] -> makePhotoActivityStart()
                    options[1] -> pickPhotoActivityStart()
                    options[2] -> dialogInterface.dismiss()
                }
            }.show()
    }

    private fun makePhotoActivityStart() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val imageFile = createImageFile()
            photoUri = imageFile.toURI().toString()
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, ProjectConstants.MAKE_PHOTO_REQUEST_ID_EDIT_SHOP)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 38)
        }
    }

    private fun pickPhotoActivityStart() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = ProjectConstants.IMAGE_MIME_TYPE
        startActivityForResult(intent, ProjectConstants.SELECT_PHOTO_REQUEST_ID_EDIT_SHOP)
    }

    private fun saveShop() {
        val userId = auth.uid
        if (userId != null) {
            val shopId = shop.shopId ?: UUID.randomUUID().toString()
            val shopName = etShopName.text.toString()
            val shopType = etShopType.text.toString()
            val uri = shop.photoUri ?: photoUri
            val location = shopPosition ?: locationListener.getLocation()
            shop = Shop(shopId, userId, shopName, shopType, uri, location.latitude, location.longitude)
            Log.d(TAG, "Trying to save shop: $shop")
            uploadShopPhoto {
                saveShopMetadata()
            }
        }
    }

    private fun uploadShopPhoto(listener: (Uri) -> Unit) {
        val photoUri = shop.photoUri
        if (photoUri?.startsWith("content://") == true) {
            val fileName = UUID.randomUUID().toString()
            val fileReference = storage.getReference("${ProjectConstants.IMAGES_LOCATION}$fileName")
            val localFile = Uri.parse(photoUri)
            fileReference.putFile(localFile).addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener {
                    shop = Shop(shop.shopId, shop.userId, shop.name, shop.type, it.toString(), shop.lat, shop.lng)
                }.addOnSuccessListener(listener)
            }
        } else {
            listener.invoke(Uri.EMPTY)
        }
    }

    private fun saveShopMetadata() {
        val userUid = auth.uid
        val shopId = shop.shopId
        val dbReference = database.getReference("${ProjectConstants.SHOPS_NODE_NAME}$userUid/$shopId")
        dbReference.setValue(shop).addOnSuccessListener {
            finish()
        }
    }

}

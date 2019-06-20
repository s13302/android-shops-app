package s13302.pl.shops.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_registration.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R
import s13302.pl.shops.data.User
import java.util.*

class RegistrationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegistrationActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        btnRegister.setOnClickListener {
            performRegister()
        }
        tvLoginScreenLink.setOnClickListener {
            val intent = Intent(this@RegistrationActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        btnSelectPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = ProjectConstants.IMAGE_MIME_TYPE
            startActivityForResult(intent, ProjectConstants.SELECT_PHOTO_RESULT_ID_REGISTRATION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ProjectConstants.SELECT_PHOTO_RESULT_ID_REGISTRATION && resultCode == Activity.RESULT_OK) {
            photoUri = data?.data
            btnSelectPhoto.alpha = 0f
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
            ivSelectedPhoto.setImageBitmap(bitmap)
        }
    }

    private fun performRegister() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        if (email.isNotBlank() and password.isNotBlank()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (! it.isSuccessful) return@addOnCompleteListener
                    val userUid = it.result?.user?.uid
                    uploadImageToFirebaseStorage(userUid)
                    Toast.makeText(this@RegistrationActivity, "Pomyślnie zarejestrowano: $userUid", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this@RegistrationActivity, "Błąd: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadImageToFirebaseStorage(userUid: String?) {
        val uri = photoUri
        if (userUid != null && uri != null) {
            val fileName = UUID.randomUUID().toString()
            val fileReference = storage.getReference(ProjectConstants.IMAGES_LOCATION + fileName)
            fileReference.putFile(uri).addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener {
                    saveUserDataToFirebaseDatabase(userUid, it.toString())
                }
            }
        }
    }

    private fun saveUserDataToFirebaseDatabase(userUid: String?, profileImageUrl: String) {
        if (userUid != null) {
            val username = etUsername.text.toString()
            val user = User(userUid, username, profileImageUrl)
            var userReference = database.getReference(ProjectConstants.USERS_NODE_NAME + userUid)
            userReference.setValue(user).addOnSuccessListener {
                Log.d(TAG, "Successfully saved user with data: ${user.uid}")
                val intent = Intent(this, FavouriteShopsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }
}

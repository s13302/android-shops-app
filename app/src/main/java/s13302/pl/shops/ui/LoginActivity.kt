package s13302.pl.shops.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import s13302.pl.shops.R

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        tvHasNoAccount.setOnClickListener {
            finish()
        }

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        if (email.isNotBlank() and password.isNotBlank()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    val intent = Intent(this@LoginActivity, FavouriteShopsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
        }
    }

}

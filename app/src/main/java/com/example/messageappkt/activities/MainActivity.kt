package com.example.messageappkt.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.messageappkt.databinding.ActivityMainBinding
import com.example.messageappkt.utils.Constants
import com.example.messageappkt.utils.PreferenceManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var preferenceManager: PreferenceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        preferenceManager = PreferenceManager()
        loadUserDetails()
        getToken()
        setListener()
    }

    private fun setListener() {
        binding!!.imageSignout.setOnClickListener { v -> signOut() }
    }

    private fun signOut() {
        showToast("Signing out.....")
        val database = FirebaseFirestore.getInstance()

        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferenceManager!!.getString(Constants.KEY_USER_ID)!!
        )

        val updates = HashMap<String, Any>()
        updates[Constants.KEY_FCM_TOKEN] = FieldValue.delete()
        documentReference.update(updates)
            .addOnSuccessListener { unused: Void? ->
                preferenceManager!!.clear()
                startActivity(Intent(applicationContext, SignInActivity::class.java))
                finish()
            }
            .addOnFailureListener { e: java.lang.Exception? -> showToast("Unable to sign out") }
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String? ->
            if (token != null) {
                this.updateToken(
                    token
                )
            }
        }
    }

    private fun updateToken(token: String) {
        val database = FirebaseFirestore.getInstance()
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferenceManager?.getString(Constants.KEY_USER_ID)!!
        )
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnSuccessListener { unused: Void? -> showToast("Token updated successfully") }
            .addOnFailureListener { e: Exception? -> showToast("Unable to update token") }
    }

    private fun loadUserDetails() {
        binding?.textName?.text = preferenceManager?.getString(Constants.KEY_NAME)
        val bytes =
            Base64.decode(preferenceManager?.getString(Constants.KEY_IMAGE), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding!!.imageProfile.setImageBitmap(bitmap)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
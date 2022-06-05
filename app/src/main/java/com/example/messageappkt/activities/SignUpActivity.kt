package com.example.messageappkt.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.example.messageappkt.databinding.ActivitySignUpBinding
import com.example.messageappkt.utils.Constants
import com.example.messageappkt.utils.PreferenceManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class SignUpActivity : AppCompatActivity() {

    private var binding: ActivitySignUpBinding? = null
    private var encodedImage: String? = null
    private var preferenceManager: PreferenceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        preferenceManager = PreferenceManager()
        setListeners()
    }

    private fun setListeners() {
        binding?.textSignIn?.setOnClickListener {
            onBackPressed()
        }
        binding?.buttonSignUp?.setOnClickListener {
            if (isValidSignUpDetails()) {
                signUp()
            }
        }
        binding?.layoutImage?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun signUp() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        val user = HashMap<String, Any>()

        user[Constants.KEY_NAME] = binding!!.inputName.text.toString()
        user[Constants.KEY_EMAIL] = binding!!.inputEmail.text.toString()
        user[Constants.KEY_PASSWORD] = binding!!.inputPassword.text.toString()
        user[Constants.KEY_IMAGE] = encodedImage!!

        database.collection(Constants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener { documentReference ->
                loading(false)

                preferenceManager?.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                preferenceManager?.putString(Constants.KEY_USER_ID, documentReference.id)
                preferenceManager?.putString(Constants.KEY_NAME, binding!!.inputName.text.toString())
                preferenceManager?.putString(Constants.KEY_IMAGE, encodedImage)

                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .addOnFailureListener { exception: Exception ->
                loading(false)
                showToast(exception.message!!)
            }
    }

    private fun encodedImage(bitmap: Bitmap): String? {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private val pickImage = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            if (result.data != null) {
                val imageUri = result.data!!.data
                try {
                    val inputStream =
                        contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding!!.imageProfile.setImageBitmap(bitmap)
                    binding!!.textAddImage.visibility = View.GONE
                    encodedImage = encodedImage(bitmap)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isValidSignUpDetails(): Boolean {
        return if (encodedImage == null) {
            showToast("Select profile image")
            false
        } else if (binding!!.inputName.text.toString().trim().isEmpty()) {
            showToast("Enter Name")
            false
        } else if (binding!!.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter Email")
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding!!.inputEmail.text.toString()).matches()) {
            showToast("Enter valid Email")
            false
        } else if (binding!!.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Enter Password")
            false
        } else if (binding!!.inputConfirmPassword.text.toString().trim().isEmpty()) {
            showToast("Confirm your password")
            false
        } else if (binding!!.inputPassword.text.toString() != binding!!.inputConfirmPassword.text.toString()) {
            showToast("Password & Confirm Password must be same")
            false
        } else {
            true
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding!!.buttonSignUp.visibility = View.INVISIBLE
            binding!!.progressBar.visibility = View.VISIBLE
        } else {
            binding!!.buttonSignUp.visibility = View.VISIBLE
            binding!!.progressBar.visibility = View.INVISIBLE
        }
    }
}
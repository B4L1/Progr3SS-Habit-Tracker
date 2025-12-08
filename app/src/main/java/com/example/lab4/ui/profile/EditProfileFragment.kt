package com.example.lab4.ui.profile

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.model.UpdateProfileDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentEditProfileBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    
    private var selectedImageUri: Uri? = null
    private val TAG = "EditProfileFragment"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                selectedImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchCurrentProfile()

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveChanges()
        }
        
        binding.changePhotoTextView.setOnClickListener {
            openGallery()
        }
        
        binding.profileImageView.setOnClickListener {
            openGallery()
        }
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun fetchCurrentProfile() {
        RetrofitClient.authService.getProfile().enqueue(object : Callback<ProfileResponseDto> {
            override fun onResponse(
                call: Call<ProfileResponseDto>,
                response: Response<ProfileResponseDto>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    binding.usernameEditText.setText(profile.username)
                    binding.emailTextView.text = profile.email
                    
                    // Load existing image if available and no new image selected
                    if (selectedImageUri == null && !profile.profileImageUrl.isNullOrEmpty()) {
                        var imageUrl = profile.profileImageUrl ?: ""
                        if (imageUrl.contains("localhost")) {
                            val currentBaseUrl = RetrofitClient.BASE_URL
                            // Extract just the host (IP and port)
                            val host = currentBaseUrl.removePrefix("http://").removePrefix("https://").substringBefore("/")
                            imageUrl = imageUrl.replace("localhost:3000", host).replace("localhost", host.substringBefore(":"))
                        }
                        val finalUrl = if (imageUrl.startsWith("http")) {
                            imageUrl
                        } else {
                            val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                            val relativeUrl = imageUrl.removePrefix("/")
                            "$baseUrl/$relativeUrl"
                        }

                        binding.profileImageView.load(finalUrl) {
                            placeholder(android.R.drawable.sym_def_app_icon)
                            error(android.R.drawable.sym_def_app_icon)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ProfileResponseDto>, t: Throwable) {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveChanges() {
        val newUsername = binding.usernameEditText.text.toString().trim()
        
        if (newUsername.isEmpty()) {
            binding.usernameInputLayout.error = "Username cannot be empty"
            return
        }
        
        // 1. Update Profile Info
        val request = UpdateProfileDto(username = newUsername)
        RetrofitClient.authService.updateProfile(request).enqueue(object : Callback<ProfileResponseDto> {
            override fun onResponse(
                call: Call<ProfileResponseDto>,
                response: Response<ProfileResponseDto>
            ) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Profile info updated. Response: ${response.body()}")
                    // 2. If image selected, upload it now
                    if (selectedImageUri != null) {
                        uploadImage(selectedImageUri!!)
                    } else {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                } else {
                    Toast.makeText(context, "Failed to update profile info: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error updating info: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun uploadImage(uri: Uri) {
        // Use getCompressedFile instead of raw getFileFromUri
        val file = getCompressedFile(uri) ?: run {
            Toast.makeText(context, "Failed to process image file", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Uploading image: ${file.name}, size: ${file.length()}")

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        // Ensure the filename has an extension
        val body = MultipartBody.Part.createFormData("profileImage", "profile.jpg", requestFile)
        
        RetrofitClient.authService.uploadProfileImage(body).enqueue(object : Callback<ProfileResponseDto> {
            override fun onResponse(call: Call<ProfileResponseDto>, response: Response<ProfileResponseDto>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "Upload success. Result URL: ${result?.profileImageUrl}")
                    
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Log.e(TAG, "Upload failed: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Info saved, but failed to upload image: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponseDto>, t: Throwable) {
                Log.e(TAG, "Upload error", t)
                Toast.makeText(context, "Info saved, but error uploading image: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun getCompressedFile(uri: Uri): File? {
        try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return null

            val tempFile = File(requireContext().cacheDir, "temp_upload.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            // Compress to JPEG, 80% quality
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

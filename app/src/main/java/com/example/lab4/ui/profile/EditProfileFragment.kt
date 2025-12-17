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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.model.UpdateProfileDto
import com.example.lab4.data.remote.AuthService
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.repository.ProfileRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    
    private var selectedImageUri: Uri? = null
    private val TAG = "EditProfileFragment"
    
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            ProfileRepository(
                RetrofitClient.createService(AuthService::class.java)
            )
        )
    }

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

        observeViewModel()
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
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        loadProfileData(state.data)
                    }
                    is UiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateProfileState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.saveButton.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.saveButton.isEnabled = true
                        if (selectedImageUri != null) {
                            uploadImage(selectedImageUri!!)
                        } else {
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            viewModel.resetUpdateState()
                            findNavController().navigateUp()
                        }
                    }
                    is UiState.Error -> {
                        binding.saveButton.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.saveButton.isEnabled = true
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uploadImageState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetUploadState()
                        findNavController().navigateUp()
                    }
                    is UiState.Error -> {
                        Toast.makeText(context, "Info saved, but failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun fetchCurrentProfile() {
        viewModel.fetchProfile()
    }
    
    private fun loadProfileData(profile: ProfileResponseDto) {
        binding.usernameEditText.setText(profile.username)
        binding.emailTextView.text = profile.email
        
        // Load existing image if available and no new image selected
        if (selectedImageUri == null && !profile.profileImageUrl.isNullOrEmpty()) {
            var imageUrl = profile.profileImageUrl ?: ""
            if (imageUrl.contains("localhost")) {
                val currentBaseUrl = RetrofitClient.BASE_URL
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

    private fun saveChanges() {
        val newUsername = binding.usernameEditText.text.toString().trim()
        
        if (newUsername.isEmpty()) {
            binding.usernameInputLayout.error = "Username cannot be empty"
            return
        }
        
        val request = UpdateProfileDto(username = newUsername)
        viewModel.updateProfile(request)
    }
    
    private fun uploadImage(uri: Uri) {
        val file = getCompressedFile(uri) ?: run {
            Toast.makeText(context, "Failed to process image file", Toast.LENGTH_SHORT).show()
            return
        }
        
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("profileImage", "profile.jpg", requestFile)
        
        viewModel.uploadProfileImage(body)
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

package com.bignerdranch.android.criminalintent

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.bignerdranch.android.criminalintent.databinding.ZoomedPhotoBinding
import java.io.File

class ZoomedPhotoDialogFragment : DialogFragment() {
    private lateinit var binding: ZoomedPhotoBinding
    private val args: ZoomedPhotoDialogFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ZoomedPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePhoto(args.photoName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            binding.root.accessibilityPaneTitle = getString(R.string.crime_photo_zoomed_in)
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.zoomedPhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }
            if (photoFile?.exists() == true) {

                val scaledBitmap = BitmapFactory.decodeFile(photoFile.path)
                binding.zoomedPhoto.setImageBitmap(scaledBitmap)
                binding.zoomedPhoto.tag = photoFileName


            } else {
                binding.zoomedPhoto.setImageBitmap(null)
                binding.zoomedPhoto.tag = null
            }
        }
    }
}
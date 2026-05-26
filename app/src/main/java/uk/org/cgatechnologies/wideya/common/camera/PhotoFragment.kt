/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.cgatechnologies.wideya.common.camera

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.interfaces.IViewModel
import uk.org.cgatechnologies.wideya.common.utils.Extensions.rotateBitmap
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.viewmodels.IViewModelFactory
import uk.org.cgatechnologies.wideya.databinding.FragmentPhotoBinding
import java.io.File

/**
 * Created by Mohamad Abuzaid on 12/16/2022.
 */

/** Fragment used for each individual page showing a photo */
class PhotoFragment internal constructor() : Fragment() {

    private var _binding: FragmentPhotoBinding? = null

    private val binding get() = _binding!!

    private lateinit var args: PhotoFragmentArgs

    private lateinit var viewModel: IViewModel.ICameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = requireArguments()
        args = PhotoFragmentArgs.fromBundle(bundle)

        viewModel = IViewModelFactory.getCameraViewModel(this, args.imageType)

        val photoPath = args.imagePath
        photoPath?.let {
            val bitmap = Utils.decodeScaledBitmap(
                Uri.fromFile(File(it)),
                requireContext(), PORTRAIT_WIDTH
            )?.rotateBitmap(Utils.getOrientation(it))
            binding.ivPhotoView.setImageBitmap(bitmap)
            viewModel.tempPhotoPath = it
        }

        binding.btRetake.setOnClickListener {
            viewModel.deletePhotoCache(Utils.getTempPhotoPath(requireContext()))
            findNavController().popBackStack()
        }

        binding.btConfirm.setOnClickListener {
            findNavController().popBackStack(R.id.CameraFragment, true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (args.title.isNotEmpty()) {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = args.title
        }
    }

    companion object {
        const val TITLE = "title"
        const val IMAGE_PATH = "image_path"
        const val IMAGE_TYPE = "image_type"
        private const val PORTRAIT_WIDTH = 600

        fun create(image: File) = PhotoFragment().apply {
            arguments = Bundle().apply {
                putString(IMAGE_PATH, image.absolutePath)
            }
        }
    }
}
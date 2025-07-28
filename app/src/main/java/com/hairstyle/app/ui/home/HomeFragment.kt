package com.hairstyle.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hairstyle.app.databinding.FragmentHomeBinding
import com.hairstyle.app.viewmodel.DrawingViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: DrawingViewModel
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[DrawingViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupViews()
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun setupViews() {
        // Check if binding is still valid
        _binding?.let { binding ->
            // Show camera preview by default
            binding.cameraPreview.visibility = View.VISIBLE
            binding.drawingView.visibility = View.VISIBLE // Show drawing view over camera
            
            // Setup drawing view with ViewModel
            binding.drawingView.setViewModel(viewModel)
        }
    }

    private fun startCamera() {
        // Check if fragment is still attached before starting camera
        if (!isAdded || context == null) {
            return
        }
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Double-check if the fragment is still attached and binding is not null
            if (!isAdded || _binding == null || context == null) {
                return@addListener
            }
            
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        // Safe access to binding
                        _binding?.let { binding ->
                            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (exc: Exception) {
                // Handle exception silently
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            }
        }
    }

    fun setDrawingMode(mode: DrawingViewModel.DrawingMode) {
        viewModel.setDrawingMode(mode)
    }
    
    fun setDrawingColor(color: Int) {
        viewModel.setDrawingColor(color)
    }
    
    fun clearDrawing() {
        _binding?.drawingView?.clearDrawing()
        viewModel.clearCanvas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
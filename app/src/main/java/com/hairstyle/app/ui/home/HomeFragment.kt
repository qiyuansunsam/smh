package com.hairstyle.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import java.nio.ByteBuffer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hairstyle.app.databinding.FragmentHomeBinding
import com.hairstyle.app.viewmodel.DrawingViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: DrawingViewModel
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isImageCaptured = false
    private var latestCameraFrame: Bitmap? = null
    private lateinit var mainViewModel: com.hairstyle.app.viewmodel.MainViewModel

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
        mainViewModel = ViewModelProvider(requireActivity())[com.hairstyle.app.viewmodel.MainViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupViews()
        
        // Restore snapshot state
        if (mainViewModel.isCanvasSnapshotted()) {
            val snapshotBitmap = mainViewModel.getSnapshotImage()
            if (snapshotBitmap != null) {
                showCapturedImage(snapshotBitmap, false) // Don't store again
            }
        }
        
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
            
            // Restore drawing state if available
            val mainViewModel = ViewModelProvider(requireActivity())[com.hairstyle.app.viewmodel.MainViewModel::class.java]
            // Drawing state is preserved in the DrawingView itself via paths list
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

                imageCapture = ImageCapture.Builder().build()
                
                // Setup image analysis to capture current frames
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            // Convert current frame to bitmap and store it
                            val bitmap = imageProxyToBitmap(imageProxy)
                            if (bitmap != null) {
                                latestCameraFrame = bitmap
                                android.util.Log.d("HomeFragment", "Camera frame captured: ${bitmap.width}x${bitmap.height}")
                            } else {
                                android.util.Log.w("HomeFragment", "Failed to convert camera frame to bitmap")
                            }
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis
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
    
    fun createMainInput() {
        _binding?.let { binding ->
            val currentViewBitmap: Bitmap?
            
            if (isImageCaptured) {
                // Use the captured snapshot image
                currentViewBitmap = mainViewModel.getSnapshotImage()
                android.util.Log.d("HomeFragment", "Using snapshot image for main input")
            } else {
                // Use the latest camera frame or capture one now
                currentViewBitmap = latestCameraFrame ?: captureCurrentFrame()
                android.util.Log.d("HomeFragment", "Using camera frame for main input: ${currentViewBitmap != null}")
            }
            
            if (currentViewBitmap != null) {
                // Create a combined bitmap with current view + drawing
                val combinedBitmap = Bitmap.createBitmap(
                    currentViewBitmap.width, 
                    currentViewBitmap.height, 
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(combinedBitmap)
                
                // Draw the current view first (snapshot or live camera frame)
                canvas.drawBitmap(currentViewBitmap, 0f, 0f, null)
                
                // Draw the drawing overlay
                binding.drawingView?.let { drawingView ->
                    val drawingBitmap = drawingView.getDrawingBitmap()
                    // Scale drawing to match current view size
                    val scaledDrawing = Bitmap.createScaledBitmap(
                        drawingBitmap, 
                        currentViewBitmap.width, 
                        currentViewBitmap.height, 
                        true
                    )
                    canvas.drawBitmap(scaledDrawing, 0f, 0f, null)
                }
                
                // Store as main input
                mainViewModel.setMainInput(combinedBitmap)
                android.util.Log.d("HomeFragment", "Main input created successfully")
            } else {
                android.util.Log.w("HomeFragment", "No current view bitmap available for main input")
            }
        }
    }
    
    private fun captureCurrentFrame(): Bitmap? {
        // If we don't have a recent frame, try to capture one immediately
        return imageCapture?.let { capture ->
            try {
                // This is a synchronous approach - in production you'd want async
                // For now, just return the latest frame we have
                latestCameraFrame
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun takePhoto() {
        if (isImageCaptured) {
            // Reset to camera preview
            resetToCamera()
            return
        }
        
        // Use the latest camera frame for immediate freezing
        val currentFrame = latestCameraFrame
        if (currentFrame != null) {
            showCapturedImage(currentFrame)
            android.util.Log.d("HomeFragment", "Snapshot taken using latest camera frame")
        } else {
            android.util.Log.w("HomeFragment", "No camera frame available for snapshot")
        }
    }
    
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            // Use YUV_420_888 format conversion
            val yBuffer = image.planes[0].buffer // Y
            val uBuffer = image.planes[1].buffer // U  
            val vBuffer = image.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
            val imageBytes = out.toByteArray()
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // Check if bitmap creation was successful
            if (bitmap == null) {
                return null
            }
            
            // Apply rotation and flip to match camera preview orientation
            val matrix = Matrix()
            matrix.postRotate(270f) // Rotate 270 degrees for front camera
            matrix.postScale(-1f, 1f) // Flip horizontally for front camera mirror effect
            
            Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
    
    private fun showCapturedImage(bitmap: Bitmap, storeInViewModel: Boolean = true) {
        _binding?.let { binding ->
            binding.capturedImageView.setImageBitmap(bitmap)
            binding.capturedImageView.visibility = View.VISIBLE
            binding.cameraPreview.visibility = View.GONE
            isImageCaptured = true
            
            // Store snapshot and state in shared ViewModel
            if (storeInViewModel) {
                mainViewModel.setSnapshotImage(bitmap)
                mainViewModel.setCanvasSnapshotted(true)
            }
        }
    }
    
    private fun resetToCamera() {
        _binding?.let { binding ->
            binding.capturedImageView.visibility = View.GONE
            binding.cameraPreview.visibility = View.VISIBLE
            isImageCaptured = false
            mainViewModel.setCanvasSnapshotted(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
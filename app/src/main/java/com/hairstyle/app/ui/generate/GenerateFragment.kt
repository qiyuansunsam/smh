package com.hairstyle.app.ui.generate

import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.hairstyle.app.R
import com.hairstyle.app.databinding.FragmentGenerateBinding
import com.hairstyle.app.viewmodel.MainViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class GenerateFragment : Fragment() {

    private var _binding: FragmentGenerateBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private var isGenerating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        animateEntrance()
    }

    private fun setupUI() {
        // Hide the original generate button since it's now in the bottom bar
        binding.generateButton.visibility = View.GONE

        // Set placeholder gradient background
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF6200EE.toInt(), 0xFF03DAC6.toInt())
        )
        gradientDrawable.cornerRadius = resources.getDimension(R.dimen.corner_radius_large)
        binding.generatedImage.background = gradientDrawable
    }

    private fun animateEntrance() {
        // Fade in animation
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        // Scale animation for generate button
        binding.generateButton.apply {
            scaleX = 0f
            scaleY = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Animate image placeholder
        binding.generatedImage.apply {
            translationY = -100f
            alpha = 0f
            animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    fun generateHairStyle() {
        isGenerating = true

        // Animate button press
        binding.generateButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.generateButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        // Show progress
        binding.generateProgress.visibility = View.VISIBLE
        binding.generateButton.isEnabled = false
        binding.generateButton.text = "Generating..."

        // Animate progress appearance
        binding.generateProgress.apply {
            scaleX = 0f
            scaleY = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
        }

        // Pulse animation on image while generating
        startPulseAnimation()

        // Simulate API call
        lifecycleScope.launch {
            delay(3000) // Simulate generation time

            // Stop animations
            stopPulseAnimation()

            // Hide progress
            binding.generateProgress.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(300)
                .withEndAction {
                    binding.generateProgress.visibility = View.GONE
                }
                .start()

            // Update UI
            binding.generateButton.isEnabled = true
            binding.generateButton.text = "Generate New Style"
            isGenerating = false

            // Show result with animation
            showGeneratedResult()
        }
    }

    private fun startPulseAnimation() {
        binding.generatedImage.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (isGenerating) {
                    binding.generatedImage.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(1000)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            if (isGenerating) {
                                startPulseAnimation()
                            }
                        }
                        .start()
                }
            }
            .start()
    }

    private fun stopPulseAnimation() {
        binding.generatedImage.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
    }

    private fun showGeneratedResult() {
        // Get main input from ViewModel (mock generate - just return main input)
        val mainInputBitmap = viewModel.getMainInput()
        
        // Animate the transition
        binding.generatedImage.apply {
            animate()
                .rotationY(90f)
                .setDuration(300)
                .withEndAction {
                    if (mainInputBitmap != null) {
                        // Show the main input image as the "generated" result
                        setImageBitmap(mainInputBitmap)
                        background = null
                    } else {
                        // Fallback to gradient if no main input available
                        val resultGradient = GradientDrawable(
                            GradientDrawable.Orientation.BR_TL,
                            intArrayOf(0xFFFF00FF.toInt(), 0xFF00FFFF.toInt())
                        )
                        resultGradient.cornerRadius = resources.getDimension(R.dimen.corner_radius_large)
                        background = resultGradient
                    }
                    rotationY = -90f
                    animate()
                        .rotationY(0f)
                        .setDuration(300)
                        .start()
                }
                .start()
        }

        // Show success message
        Snackbar.make(
            binding.root,
            "Hair style generated successfully!",
            Snackbar.LENGTH_LONG
        ).setAction("Save") {
            saveGeneratedImage()
        }.show()
    }

    private fun saveGeneratedImage() {
        // TODO: Implement save functionality
        Snackbar.make(
            binding.root,
            "Image saved to gallery!",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
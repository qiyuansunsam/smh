package com.hairstyle.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.hairstyle.app.R
import com.hairstyle.app.databinding.ComponentNavigationBarBinding

class NavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentNavigationBarBinding
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var currentTab = 1 // Canvas is default (index 1)
    private var isInitialSetup = true

    companion object {
        const val TAB_REFERENCE = 0
        const val TAB_CANVAS = 1
        const val TAB_GENERATE = 2
    }

    init {
        binding = ComponentNavigationBarBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        
        // Position the indicator after layout is complete
        if (isInitialSetup && changed) {
            // Use ViewTreeObserver to ensure all views are fully laid out
            viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (isInitialSetup) {
                        selectTab(TAB_CANVAS) // Default to canvas
                    }
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.btnReference.setOnClickListener { selectTab(TAB_REFERENCE) }
        binding.btnCanvas.setOnClickListener { selectTab(TAB_CANVAS) }
        binding.btnGenerate.setOnClickListener { selectTab(TAB_GENERATE) }
    }

    fun selectTab(tabIndex: Int) {
        if (currentTab == tabIndex && !isInitialSetup) return
        
        currentTab = tabIndex
        updateTabAppearance()
        animateIndicator(tabIndex)
        onTabSelectedListener?.invoke(tabIndex)
    }

    private fun updateTabAppearance() {
        // Reset all tabs
        binding.btnReference.alpha = 0.5f
        binding.btnCanvas.alpha = 0.5f
        binding.btnGenerate.alpha = 0.5f

        // Highlight selected tab
        when (currentTab) {
            TAB_REFERENCE -> binding.btnReference.alpha = 1.0f
            TAB_CANVAS -> binding.btnCanvas.alpha = 1.0f
            TAB_GENERATE -> binding.btnGenerate.alpha = 1.0f
        }
    }

    private fun animateIndicator(tabIndex: Int) {
        val targetButton = when (tabIndex) {
            TAB_REFERENCE -> binding.btnReference
            TAB_CANVAS -> binding.btnCanvas
            TAB_GENERATE -> binding.btnGenerate
            else -> binding.btnCanvas
        }

        // Calculate proper centered position
        post {
            try {
                val buttonWidth = targetButton.width
                val indicatorWidth = binding.activeIndicator.width
                val targetX = targetButton.x + (buttonWidth - indicatorWidth) / 2f
                
                if (isInitialSetup) {
                    // Initial positioning - no animation
                    binding.activeIndicator.x = targetX
                    isInitialSetup = false
                } else {
                    // Animated positioning for tab changes
                    binding.activeIndicator.animate()
                        .x(targetX)
                        .setDuration(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            } catch (e: Exception) {
                // Fallback positioning
                val buttonWidth = targetButton.width
                val indicatorWidth = binding.activeIndicator.width
                val targetX = targetButton.x + (buttonWidth - indicatorWidth) / 2f
                binding.activeIndicator.x = targetX
            }
        }
    }
    

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun getCurrentTab(): Int = currentTab

    fun ensureProperInitialPosition() {
        if (isInitialSetup) {
            post {
                selectTab(TAB_CANVAS)
            }
        }
    }
}
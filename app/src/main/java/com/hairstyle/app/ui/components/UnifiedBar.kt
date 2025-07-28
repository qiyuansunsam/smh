package com.hairstyle.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.hairstyle.app.R
import com.hairstyle.app.databinding.ComponentUnifiedBarBinding

class UnifiedBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentUnifiedBarBinding
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var onSnapshotClickListener: ((Boolean) -> Unit)? = null
    private var onGenerateClickListener: (() -> Unit)? = null
    private var onDrawingToolSelectedListener: ((DrawingTools.Tool) -> Unit)? = null
    private var currentTab = 1 // Canvas is default (index 1)
    private var isInitialSetup = true
    private var isSnapshotActive = false

    companion object {
        const val TAB_REFERENCE = 0
        const val TAB_CANVAS = 1
        const val TAB_GENERATE = 2
    }

    init {
        binding = ComponentUnifiedBarBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
        setupColorPicker()
        setupDrawingTools()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        
        if (isInitialSetup && changed) {
            viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (isInitialSetup) {
                        selectTab(TAB_CANVAS)
                    }
                }
            })
        }
    }

    private fun setupClickListeners() {
        // Navigation clicks
        binding.btnReference.setOnClickListener { selectTab(TAB_REFERENCE) }
        binding.btnCanvas.setOnClickListener { selectTab(TAB_CANVAS) }
        binding.btnGenerate.setOnClickListener { selectTab(TAB_GENERATE) }
        
        // Tool clicks
        binding.btnSnapshot.setOnClickListener {
            toggleSnapshot()
        }
        
        binding.btnGenerateAction.setOnClickListener {
            animateGenerateButton()
            onGenerateClickListener?.invoke()
        }
    }

    private fun setupColorPicker() {
        // ColorPicker initializes its own gradient colors
    }

    private fun setupDrawingTools() {
        val drawingTools = binding.drawingTools
        drawingTools?.setOnToolSelectedListener { tool ->
            onDrawingToolSelectedListener?.invoke(tool)
        }
    }

    private fun toggleSnapshot() {
        isSnapshotActive = !isSnapshotActive
        animateSnapshotToggle()
        onSnapshotClickListener?.invoke(isSnapshotActive)
    }

    private fun animateSnapshotToggle() {
        val targetBackground = if (isSnapshotActive) {
            R.drawable.bg_snapshot_button_active
        } else {
            R.drawable.bg_snapshot_button
        }

        binding.btnSnapshot.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.btnSnapshot.background = ContextCompat.getDrawable(context, targetBackground)
                binding.btnSnapshot.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun animateGenerateButton() {
        binding.btnGenerateAction.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.btnGenerateAction.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    fun selectTab(tabIndex: Int) {
        if (currentTab == tabIndex && !isInitialSetup) return
        
        currentTab = tabIndex
        updateTabAppearance()
        animateIndicator(tabIndex)
        showTabContent(tabIndex)
        onTabSelectedListener?.invoke(tabIndex)
    }

    private fun updateTabAppearance() {
        // Reset all tabs
        binding.btnReference.alpha = 0.6f
        binding.btnCanvas.alpha = 0.6f
        binding.btnGenerate.alpha = 0.6f

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

        post {
            try {
                val buttonWidth = targetButton.width
                val indicatorWidth = binding.activeIndicator.width
                val targetX = targetButton.x + (buttonWidth - indicatorWidth) / 2f
                
                if (isInitialSetup) {
                    binding.activeIndicator.x = targetX
                    isInitialSetup = false
                } else {
                    binding.activeIndicator.animate()
                        .x(targetX)
                        .setDuration(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            } catch (e: Exception) {
                val buttonWidth = targetButton.width
                val indicatorWidth = binding.activeIndicator.width
                val targetX = targetButton.x + (buttonWidth - indicatorWidth) / 2f
                binding.activeIndicator.x = targetX
            }
        }
    }

    private fun showTabContent(tabIndex: Int) {
        when (tabIndex) {
            TAB_CANVAS -> showCanvasContent()
            TAB_GENERATE -> showGenerateContent()
            TAB_REFERENCE -> showReferenceContent()
        }
    }

    private fun showCanvasContent() {
        animateContainerTransition(binding.canvasContainer, true)
        animateContainerTransition(binding.generateContainer, false)
        animateContainerTransition(binding.referenceContainer, false)
    }

    private fun showGenerateContent() {
        animateContainerTransition(binding.canvasContainer, false)
        animateContainerTransition(binding.generateContainer, true)
        animateContainerTransition(binding.referenceContainer, false)
    }
    
    private fun showReferenceContent() {
        animateContainerTransition(binding.canvasContainer, false)
        animateContainerTransition(binding.generateContainer, false)
        animateContainerTransition(binding.referenceContainer, true)
    }
    
    private fun animateContainerTransition(container: View, show: Boolean) {
        if (show && container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            container.alpha = 0f
            container.scaleY = 0.7f
            container.animate()
                .alpha(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else if (!show && container.visibility == View.VISIBLE) {
            container.animate()
                .alpha(0f)
                .scaleY(0.7f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    container.visibility = View.GONE
                    container.alpha = 1f
                    container.scaleY = 1f
                }
                .start()
        }
    }

    // Public getters and setters
    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setOnSnapshotClickListener(listener: (Boolean) -> Unit) {
        onSnapshotClickListener = listener
    }

    fun setOnGenerateClickListener(listener: () -> Unit) {
        onGenerateClickListener = listener
    }

    fun setOnDrawingToolSelectedListener(listener: (DrawingTools.Tool) -> Unit) {
        onDrawingToolSelectedListener = listener
    }

    fun getCurrentTab(): Int = currentTab

    fun isSnapshotToggled(): Boolean = isSnapshotActive

    fun getColorPicker(): ColorPicker? {
        return binding.colorPicker
    }

    fun getDrawingTools(): DrawingTools? {
        return binding.drawingTools
    }

    fun setGenerateButtonState(isGenerating: Boolean, text: String = "Generate New Style") {
        binding.btnGenerateAction.isEnabled = !isGenerating
        binding.btnGenerateAction.text = text
        
        if (isGenerating) {
            binding.btnGenerateAction.text = "Generating..."
        }
    }
}
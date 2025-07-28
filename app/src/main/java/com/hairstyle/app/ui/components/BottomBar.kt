package com.hairstyle.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.hairstyle.app.R
import com.hairstyle.app.databinding.ComponentBottomBarBinding

class BottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentBottomBarBinding
    private var isSnapshotActive = false
    private var onSnapshotClickListener: ((Boolean) -> Unit)? = null
    private var onGenerateClickListener: (() -> Unit)? = null

    init {
        binding = ComponentBottomBarBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
        setupColorPicker()
        setupDrawingTools()
    }

    private fun setupClickListeners() {
        binding.btnSnapshot.setOnClickListener {
            toggleSnapshot()
        }
        
        binding.btnGenerate.setOnClickListener {
            animateGenerateButton()
            onGenerateClickListener?.invoke()
        }
    }

    private fun setupColorPicker() {
        // ColorPicker now uses gradient-based selection, no setup needed
        // The color picker initializes its own gradient colors
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

        // Scale animation
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

    fun setOnSnapshotClickListener(listener: (Boolean) -> Unit) {
        onSnapshotClickListener = listener
    }

    fun getColorPicker(): ColorPicker? {
        return findViewById<ColorPicker>(R.id.colorPicker)
    }

    private fun setupDrawingTools() {
        val drawingTools = findViewById<DrawingTools>(R.id.drawingTools)
        drawingTools?.setOnToolSelectedListener { tool ->
            onDrawingToolSelectedListener?.invoke(tool)
        }
    }

    private var onDrawingToolSelectedListener: ((DrawingTools.Tool) -> Unit)? = null

    fun setOnDrawingToolSelectedListener(listener: (DrawingTools.Tool) -> Unit) {
        onDrawingToolSelectedListener = listener
    }

    fun getDrawingTools(): DrawingTools? {
        return findViewById<DrawingTools>(R.id.drawingTools)
    }

    fun isSnapshotToggled(): Boolean = isSnapshotActive

    enum class BottomBarState {
        CANVAS, GENERATE, REFERENCE
    }

    fun setBottomBarState(state: BottomBarState) {
        when (state) {
            BottomBarState.CANVAS -> showCanvasContent()
            BottomBarState.GENERATE -> showGenerateContent()
            BottomBarState.REFERENCE -> showReferenceContent()
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

    private fun hideAllContent() {
        animateContainerTransition(binding.canvasContainer, false)
        animateContainerTransition(binding.generateContainer, false)
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
            container.scaleX = 0.8f
            container.scaleY = 0.8f
            container.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } else if (!show && container.visibility == View.VISIBLE) {
            container.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    container.visibility = View.GONE
                    container.alpha = 1f
                    container.scaleX = 1f
                    container.scaleY = 1f
                }
                .start()
        }
    }

    fun setOnGenerateClickListener(listener: () -> Unit) {
        onGenerateClickListener = listener
    }

    private fun animateGenerateButton() {
        // Animate button press similar to original GenerateFragment
        binding.btnGenerate.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.btnGenerate.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    fun setGenerateButtonState(isGenerating: Boolean, text: String = "Generate New Style") {
        binding.btnGenerate.isEnabled = !isGenerating
        binding.btnGenerate.text = text
        
        if (isGenerating) {
            // Show generating state
            binding.btnGenerate.text = "Generating..."
        }
    }
}
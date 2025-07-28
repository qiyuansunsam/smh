package com.hairstyle.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.hairstyle.app.R
import com.hairstyle.app.databinding.ComponentDrawingToolsBinding

class DrawingTools @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentDrawingToolsBinding
    private var currentTool = Tool.PEN
    private var onToolSelectedListener: ((Tool) -> Unit)? = null

    enum class Tool {
        PEN, ERASER, CLEAR, SETTINGS
    }

    init {
        binding = ComponentDrawingToolsBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
        selectTool(Tool.PEN) // Default to pen
    }

    private fun setupClickListeners() {
        binding.btnPen.setOnClickListener { selectTool(Tool.PEN) }
        binding.btnEraser.setOnClickListener { selectTool(Tool.ERASER) }
        binding.btnClear.setOnClickListener { selectTool(Tool.CLEAR) }
        binding.btnSettings.setOnClickListener { selectTool(Tool.SETTINGS) }
    }

    private fun selectTool(tool: Tool) {
        currentTool = tool
        updateToolAppearance()
        onToolSelectedListener?.invoke(tool)
    }

    private fun updateToolAppearance() {
        // Reset all tools to default state
        binding.btnPen.alpha = 0.5f
        binding.btnEraser.alpha = 0.5f
        binding.btnClear.alpha = 0.5f
        binding.btnSettings.alpha = 0.5f

        // Highlight selected tool
        when (currentTool) {
            Tool.PEN -> binding.btnPen.alpha = 1.0f
            Tool.ERASER -> binding.btnEraser.alpha = 1.0f
            Tool.CLEAR -> binding.btnClear.alpha = 1.0f
            Tool.SETTINGS -> binding.btnSettings.alpha = 1.0f
        }
    }

    fun setOnToolSelectedListener(listener: (Tool) -> Unit) {
        onToolSelectedListener = listener
    }

    fun getCurrentTool(): Tool = currentTool
}
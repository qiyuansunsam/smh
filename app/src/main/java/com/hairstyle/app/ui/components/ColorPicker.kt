package com.hairstyle.app.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import com.hairstyle.app.R
import kotlin.math.max
import kotlin.math.min

class ColorPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val LINE_HEIGHT = 10f // Height of each color line in dp (much smaller to fit 5 lines in 75dp)
        private const val LINE_SPACING = 2f // Spacing between lines in dp (smaller)
        private const val SELECTOR_WIDTH = 2f // Width of the vertical selector line in dp (smaller)
        private const val SELECTOR_PADDING = 2f // Extra height for selector above/below lines (smaller)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = dpToPx(SELECTOR_WIDTH)
        strokeCap = Paint.Cap.SQUARE // Sharp edges instead of round
        // Add shadow for better visibility
        setShadowLayer(dpToPx(4f), 0f, 0f, Color.argb(128, 0, 0, 0))
    }

    // Gradient definitions for 6 lines - full color spectrum split into 6 parts
    private val gradientColors = listOf(
        // Line 1: Red to Orange (first sixth of spectrum)
        intArrayOf(
            Color.RED,
            Color.rgb(255, 32, 0),
            Color.rgb(255, 64, 0),
            Color.rgb(255, 96, 0),
            Color.rgb(255, 128, 0) // Orange
        ),
        // Line 2: Orange to Yellow (second sixth of spectrum)
        intArrayOf(
            Color.rgb(255, 128, 0), // Orange
            Color.rgb(255, 160, 0),
            Color.rgb(255, 192, 0),
            Color.rgb(255, 224, 0),
            Color.rgb(255, 255, 0) // Yellow
        ),
        // Line 3: Yellow to Green (third sixth of spectrum)
        intArrayOf(
            Color.rgb(255, 255, 0), // Yellow
            Color.rgb(192, 255, 0),
            Color.rgb(128, 255, 0),
            Color.rgb(64, 255, 0),
            Color.GREEN
        ),
        // Line 4: Green to Cyan (fourth sixth of spectrum)
        intArrayOf(
            Color.GREEN,
            Color.rgb(0, 255, 64),
            Color.rgb(0, 255, 128),
            Color.rgb(0, 255, 192),
            Color.CYAN
        ),
        // Line 5: Cyan to Blue to Purple to Magenta to Red (fifth sixth of spectrum)
        intArrayOf(
            Color.CYAN,
            Color.rgb(0, 128, 255),
            Color.BLUE,
            Color.rgb(64, 0, 255),
            Color.rgb(128, 0, 255), // Purple
            Color.rgb(192, 0, 255),
            Color.MAGENTA,
            Color.rgb(255, 0, 128),
            Color.RED
        ),
        // Line 6: Black to White grayscale (full line dedicated to grayscale)
        intArrayOf(
            Color.BLACK,
            Color.rgb(32, 32, 32),   // Very Dark Gray
            Color.rgb(64, 64, 64),   // Dark Gray
            Color.rgb(96, 96, 96),   // Medium Dark Gray
            Color.rgb(128, 128, 128), // Gray
            Color.rgb(160, 160, 160), // Medium Light Gray
            Color.rgb(192, 192, 192), // Light Gray
            Color.rgb(224, 224, 224), // Very Light Gray
            Color.WHITE
        )
    )

    private var gradientShaders = mutableListOf<LinearGradient>()
    private var selectorX = 0f
    private var selectedLineIndex = 5 // Default to line 6 (black/white line)
    private var selectedColor = Color.BLACK // Default to black
    private var colorSelectedListener: ((Int) -> Unit)? = null

    private val lineHeight = dpToPx(LINE_HEIGHT)
    private val lineSpacing = dpToPx(LINE_SPACING)
    private val selectorPadding = dpToPx(SELECTOR_PADDING)


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Create gradient shaders for each line
        gradientShaders.clear()
        gradientColors.forEach { colors ->
            gradientShaders.add(
                LinearGradient(
                    0f, 0f, w.toFloat(), 0f,
                    colors, null,
                    Shader.TileMode.CLAMP
                )
            )
        }

        // Set initial selector position to start (black)
        if (selectorX == 0f) {
            selectorX = 0f // Start at leftmost position (black)
            updateSelectedColor()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startY = selectorPadding

        // Draw exactly 6 gradient lines - ensure they fit within bounds
        for (index in 0 until minOf(6, gradientShaders.size)) {
            val shader = gradientShaders[index]
            paint.shader = shader
            val y = startY + index * (lineHeight + lineSpacing)
            // Make sure the line doesn't exceed canvas bounds
            if (y + lineHeight <= height) {
                canvas.drawRect(
                    0f, y, width.toFloat(), y + lineHeight,
                    paint
                )
            }
        }

        // Draw vertical selector line on the selected line
        val selectedLineY = startY + selectedLineIndex * (lineHeight + lineSpacing)
        canvas.drawLine(
            selectorX,
            selectedLineY,
            selectorX,
            selectedLineY + lineHeight,
            selectorPaint
        )

        // Draw square handle at selector position with sharp edges
        val handleSize = dpToPx(6f)
        canvas.drawRect(
            selectorX - handleSize / 2,
            selectedLineY - handleSize / 2,
            selectorX + handleSize / 2,
            selectedLineY + lineHeight + handleSize / 2,
            selectorPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                // Update selector position
                selectorX = max(0f, min(width.toFloat(), event.x))
                
                // Determine which line was touched
                val touchY = event.y
                val startY = selectorPadding
                selectedLineIndex = when {
                    touchY < startY + lineHeight -> 0 // First line
                    touchY < startY + lineHeight + lineSpacing + lineHeight -> 1 // Second line
                    touchY < startY + 2 * (lineHeight + lineSpacing) + lineHeight -> 2 // Third line
                    touchY < startY + 3 * (lineHeight + lineSpacing) + lineHeight -> 3 // Fourth line
                    touchY < startY + 4 * (lineHeight + lineSpacing) + lineHeight -> 4 // Fifth line
                    else -> 5 // Sixth line
                }
                selectedLineIndex = max(0, min(5, selectedLineIndex))
                
                updateSelectedColor()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // No animation on release
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateSelectedColor() {
        // Get color from the selected gradient line
        val position = selectorX / width.toFloat()
        selectedColor = interpolateColor(gradientColors[selectedLineIndex], position)
        colorSelectedListener?.invoke(selectedColor)
    }

    private fun interpolateColor(colors: IntArray, position: Float): Int {
        val scaledPosition = position * (colors.size - 1)
        val index = scaledPosition.toInt()
        val fraction = scaledPosition - index

        if (index >= colors.size - 1) {
            return colors.last()
        }

        val startColor = colors[index]
        val endColor = colors[index + 1]

        val startA = Color.alpha(startColor)
        val startR = Color.red(startColor)
        val startG = Color.green(startColor)
        val startB = Color.blue(startColor)

        val endA = Color.alpha(endColor)
        val endR = Color.red(endColor)
        val endG = Color.green(endColor)
        val endB = Color.blue(endColor)

        val a = (startA + fraction * (endA - startA)).toInt()
        val r = (startR + fraction * (endR - startR)).toInt()
        val g = (startG + fraction * (endG - startG)).toInt()
        val b = (startB + fraction * (endB - startB)).toInt()

        return Color.argb(a, r, g, b)
    }

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        colorSelectedListener = listener
    }

    fun setSelectedColor(color: Int) {
        selectedColor = color
        // Try to find approximate position for this color
        // This is a simplified approach - you might want to improve this
        val colors = gradientColors[0]
        var minDistance = Float.MAX_VALUE
        var bestPosition = 0f

        for (i in 0..100) {
            val pos = i / 100f
            val testColor = interpolateColor(colors, pos)
            val distance = colorDistance(testColor, color)
            if (distance < minDistance) {
                minDistance = distance
                bestPosition = pos
            }
        }

        selectorX = bestPosition * width
        invalidate()
    }

    private fun colorDistance(color1: Int, color2: Int): Float {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return ((r1 - r2) * (r1 - r2) +
                (g1 - g2) * (g1 - g2) +
                (b1 - b2) * (b1 - b2)).toFloat()
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Use the exact layout dimensions (114dp x 75dp) from XML
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        setMeasuredDimension(width, height)
    }
}
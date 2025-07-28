package com.hairstyle.app.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.FragmentActivity
import com.hairstyle.app.viewmodel.DrawingViewModel
import com.hairstyle.app.viewmodel.MainViewModel
import kotlin.math.abs

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawPath = Path()
    private var drawPaint = Paint()
    private var canvasPaint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null

    private var brushSize = 10f
    private var lastBrushSize = brushSize
    private var paintColor = 0xFF000000.toInt()
    private var isEraserMode = false

    private lateinit var viewModel: DrawingViewModel
    private var mainViewModel: MainViewModel? = null

    private val paths = mutableListOf<Pair<Path, Paint>>()
    private var currentPath: Path? = null

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.apply {
            color = paintColor
            isAntiAlias = true
            strokeWidth = brushSize
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            pathEffect = CornerPathEffect(10f)
        }
    }

    fun setViewModel(vm: DrawingViewModel) {
        viewModel = vm
        
        // Get MainViewModel for state preservation
        (context as? FragmentActivity)?.let { activity ->
            mainViewModel = ViewModelProvider(activity)[MainViewModel::class.java]
            // Restore drawing paths if available
            restoreDrawingPaths()
        }

        // Observe drawing color changes
        viewModel.drawingColor.observeForever { color ->
            if (!isEraserMode) {
                paintColor = color
                drawPaint.color = color
            }
        }

        // Observe drawing mode changes
        viewModel.drawingMode.observeForever { mode ->
            when (mode) {
                DrawingViewModel.DrawingMode.PEN -> {
                    isEraserMode = false
                    drawPaint.apply {
                        xfermode = null
                        color = paintColor
                        strokeWidth = brushSize
                    }
                }
                DrawingViewModel.DrawingMode.ERASER -> {
                    isEraserMode = true
                    drawPaint.apply {
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        strokeWidth = brushSize * 2
                    }
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all saved paths
        paths.forEach { (path, paint) ->
            canvas.drawPath(path, paint)
        }

        // Draw current path
        currentPath?.let {
            canvas.drawPath(it, drawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath = Path()
                currentPath = drawPath

                // Create new paint for this path
                val pathPaint = Paint(drawPaint)
                paths.add(Pair(drawPath, pathPaint))

                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                currentPath = null
                // Save paths after each stroke
                saveDrawingPaths()
            }
            else -> return false
        }

        invalidate()
        return true
    }


    fun clearDrawing() {
        paths.clear()
        mainViewModel?.clearDrawingPaths()
        invalidate()
    }
    
    private fun restoreDrawingPaths() {
        mainViewModel?.let { vm ->
            val savedPaths = vm.getDrawingPaths()
            paths.clear()
            paths.addAll(savedPaths)
            invalidate()
        }
    }
    
    fun saveDrawingPaths() {
        mainViewModel?.setDrawingPaths(paths.toMutableList())
    }

    fun setBrushSize(newSize: Float) {
        brushSize = newSize
        drawPaint.strokeWidth = brushSize
    }

    fun setLastBrushSize(lastSize: Float) {
        lastBrushSize = lastSize
    }

    fun getLastBrushSize(): Float {
        return lastBrushSize
    }

    fun getDrawingBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
}
package com.hairstyle.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import android.graphics.Color

class DrawingViewModel : ViewModel() {
    
    enum class DrawingMode {
        PEN, ERASER
    }
    
    private val _drawingColor = MutableLiveData<Int>(Color.BLACK)
    val drawingColor: LiveData<Int> = _drawingColor
    
    private val _drawingMode = MutableLiveData<DrawingMode>(DrawingMode.PEN)
    val drawingMode: LiveData<DrawingMode> = _drawingMode
    
    private val _strokeWidth = MutableLiveData<Float>(5f)
    val strokeWidth: LiveData<Float> = _strokeWidth
    
    private val _canUndo = MutableLiveData<Boolean>(false)
    val canUndo: LiveData<Boolean> = _canUndo
    
    private val _canRedo = MutableLiveData<Boolean>(false)
    val canRedo: LiveData<Boolean> = _canRedo
    
    fun setDrawingColor(color: Int) {
        _drawingColor.value = color
    }
    
    fun setDrawingMode(mode: DrawingMode) {
        _drawingMode.value = mode
    }
    
    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }
    
    fun setCanUndo(canUndo: Boolean) {
        _canUndo.value = canUndo
    }
    
    fun setCanRedo(canRedo: Boolean) {
        _canRedo.value = canRedo
    }
    
    fun clearCanvas() {
        _canUndo.value = false
        _canRedo.value = false
    }
}
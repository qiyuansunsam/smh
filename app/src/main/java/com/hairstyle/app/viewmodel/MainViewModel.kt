package com.hairstyle.app.viewmodel

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

class MainViewModel : ViewModel() {
    
    private val _currentFragment = MutableLiveData<String>("canvas")
    val currentFragment: LiveData<String> = _currentFragment
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _isSnapshotMode = MutableLiveData<Boolean>(false)
    val isSnapshotMode: LiveData<Boolean> = _isSnapshotMode
    
    // Reference image selection state - preserved across tab switches
    private val _selectedReferenceImages = MutableLiveData<MutableSet<String>>(mutableSetOf())
    val selectedReferenceImages: LiveData<MutableSet<String>> = _selectedReferenceImages
    
    // Reference image data cache
    private val _referenceImageData = MutableLiveData<MutableMap<String, ReferenceImage>>(mutableMapOf())
    val referenceImageData: LiveData<MutableMap<String, ReferenceImage>> = _referenceImageData
    
    // Snapshot image storage
    private val _snapshotImage = MutableLiveData<Bitmap?>(null)
    val snapshotImage: LiveData<Bitmap?> = _snapshotImage
    
    // Main input storage (snapshot + drawing combined)
    private val _mainInput = MutableLiveData<Bitmap?>(null)
    val mainInput: LiveData<Bitmap?> = _mainInput
    
    // Canvas state preservation
    private val _isCanvasSnapshotted = MutableLiveData<Boolean>(false)
    val isCanvasSnapshotted: LiveData<Boolean> = _isCanvasSnapshotted
    
    // Drawing paths preservation
    private val _drawingPaths = MutableLiveData<MutableList<Pair<Path, Paint>>>(mutableListOf())
    val drawingPaths: LiveData<MutableList<Pair<Path, Paint>>> = _drawingPaths
    
    fun setCurrentFragment(fragment: String) {
        _currentFragment.value = fragment
    }
    
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    fun setSnapshotMode(isSnapshot: Boolean) {
        _isSnapshotMode.value = isSnapshot
    }
    
    // Reference image selection methods
    fun addSelectedImage(imageId: String, imageData: ReferenceImage) {
        val currentSelected = _selectedReferenceImages.value ?: mutableSetOf()
        val currentData = _referenceImageData.value ?: mutableMapOf()
        
        currentSelected.add(imageId)
        currentData[imageId] = imageData
        
        _selectedReferenceImages.value = currentSelected
        _referenceImageData.value = currentData
    }
    
    fun removeSelectedImage(imageId: String) {
        val currentSelected = _selectedReferenceImages.value ?: mutableSetOf()
        val currentData = _referenceImageData.value ?: mutableMapOf()
        
        currentSelected.remove(imageId)
        currentData.remove(imageId)
        
        _selectedReferenceImages.value = currentSelected
        _referenceImageData.value = currentData
    }
    
    fun isImageSelected(imageId: String): Boolean {
        return _selectedReferenceImages.value?.contains(imageId) ?: false
    }
    
    fun getSelectedImagesData(): List<ReferenceImage> {
        val selected = _selectedReferenceImages.value ?: mutableSetOf()
        val data = _referenceImageData.value ?: mutableMapOf()
        return selected.mapNotNull { data[it] }
    }
    
    fun clearSelectedImages() {
        _selectedReferenceImages.value = mutableSetOf()
        _referenceImageData.value = mutableMapOf()
    }
    
    // Snapshot image methods
    fun setSnapshotImage(bitmap: Bitmap?) {
        _snapshotImage.value = bitmap
    }
    
    fun getSnapshotImage(): Bitmap? {
        return _snapshotImage.value
    }
    
    // Main input methods
    fun setMainInput(bitmap: Bitmap?) {
        _mainInput.value = bitmap
    }
    
    fun getMainInput(): Bitmap? {
        return _mainInput.value
    }
    
    // Canvas state methods
    fun setCanvasSnapshotted(isSnapshotted: Boolean) {
        _isCanvasSnapshotted.value = isSnapshotted
    }
    
    fun isCanvasSnapshotted(): Boolean {
        return _isCanvasSnapshotted.value ?: false
    }
    
    // Drawing paths methods
    fun setDrawingPaths(paths: MutableList<Pair<Path, Paint>>) {
        _drawingPaths.value = paths
    }
    
    fun getDrawingPaths(): MutableList<Pair<Path, Paint>> {
        return _drawingPaths.value ?: mutableListOf()
    }
    
    fun clearDrawingPaths() {
        _drawingPaths.value = mutableListOf()
    }
    
    // Data class for reference images (moved from fragment)
    data class ReferenceImage(
        val id: String,
        val assetPath: String,
        val thumbnail: String
    )
}
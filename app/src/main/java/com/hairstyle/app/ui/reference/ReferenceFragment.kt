package com.hairstyle.app.ui.reference

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hairstyle.app.R
import com.hairstyle.app.databinding.FragmentReferenceBinding
import com.hairstyle.app.databinding.ItemReferenceImageBinding
import com.hairstyle.app.viewmodel.MainViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.IOException
import com.hairstyle.app.databinding.ItemSelectedImageBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Observer

class ReferenceFragment : Fragment() {

    private var _binding: FragmentReferenceBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MainViewModel
    private lateinit var referenceAdapter: ReferenceImageAdapter
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReferenceBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        loadReferenceImages()
        animateEntrance()
    }
    
    private fun observeViewModel() {
        viewModel.selectedReferenceImages.observe(viewLifecycleOwner, Observer {
            updateBottomBar()
            referenceAdapter.notifyDataSetChanged()
        })
    }

    private fun setupRecyclerView() {
        referenceAdapter = ReferenceImageAdapter { imageId, isSelected ->
            // Use ViewModel for state management
            if (isSelected) {
                val imageData = getImageDataById(imageId)
                imageData?.let { viewModel.addSelectedImage(imageId, it) }
            } else {
                viewModel.removeSelectedImage(imageId)
            }
            updateBottomBar()
        }

        binding.referenceRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = referenceAdapter
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
            }
        }
        
        setupSelectedImagesRecycler()
    }
    
    private fun setupSelectedImagesRecycler() {
        selectedImagesAdapter = SelectedImagesAdapter { imageId ->
            viewModel.removeSelectedImage(imageId)
            referenceAdapter.notifyDataSetChanged()
            updateBottomBar()
        }
        
        val unifiedBar = requireActivity().findViewById<View>(R.id.unifiedBar)
        // Try both recycler views (generate and reference containers)
        val selectedRecycler = unifiedBar.findViewById<RecyclerView>(R.id.selectedImagesRecycler)
        val selectedRecyclerRef = unifiedBar.findViewById<RecyclerView>(R.id.selectedImagesRecyclerRef)
        
        selectedRecycler?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedImagesAdapter
        }
        
        selectedRecyclerRef?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = selectedImagesAdapter
        }
    }

    private fun loadReferenceImages() {
        // Load 6 random images from assets/all
        binding.loadingProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            delay(500) // Brief loading delay

            // Get 6 random images from assets/all (CM_1.png to CM_10.png)
            val allImageNames = (1..10).map { "CM_$it.png" }
            val randomImages = allImageNames.shuffled().take(6)
            
            val images = randomImages.map { imageName ->
                MainViewModel.ReferenceImage(
                    id = imageName.substringBefore('.'),
                    assetPath = "all/$imageName",
                    thumbnail = "all/$imageName"
                )
            }

            referenceAdapter.submitList(images)
            binding.loadingProgress.visibility = View.GONE

            animateItems()
        }
    }

    private fun animateEntrance() {
        binding.root.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(400)
                .start()
        }
    }

    private fun animateItems() {
        val layoutManager = binding.referenceRecyclerView.layoutManager as GridLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        for (i in firstVisible..lastVisible) {
            val view = layoutManager.findViewByPosition(i)
            view?.let {
                it.alpha = 0f
                it.translationY = 100f
                it.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((i - firstVisible) * 50L)
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    private fun updateBottomBar() {
        val unifiedBar = requireActivity().findViewById<View>(R.id.unifiedBar)
        val selectedRecycler = unifiedBar.findViewById<RecyclerView>(R.id.selectedImagesRecycler)
        val selectedRecyclerRef = unifiedBar.findViewById<RecyclerView>(R.id.selectedImagesRecyclerRef)
        val referenceContainer = unifiedBar.findViewById<View>(R.id.referenceContainer)

        // Update adapter with selected images from ViewModel
        val selectedImagesList = viewModel.getSelectedImagesData()
        selectedImagesAdapter.submitList(selectedImagesList)

        // Show/hide floating reference container based on selections
        if (selectedImagesList.isNotEmpty()) {
            referenceContainer?.visibility = View.VISIBLE
            referenceContainer?.alpha = 0f
            referenceContainer?.scaleX = 0.9f
            referenceContainer?.scaleY = 0.9f
            referenceContainer?.animate()
                ?.alpha(1f)
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setDuration(300)
                ?.start()
        } else {
            referenceContainer?.animate()
                ?.alpha(0f)
                ?.scaleX(0.9f)
                ?.scaleY(0.9f)
                ?.setDuration(200)
                ?.withEndAction {
                    referenceContainer.visibility = View.GONE
                    referenceContainer.alpha = 1f
                    referenceContainer.scaleX = 1f
                    referenceContainer.scaleY = 1f
                }
                ?.start()
        }
    }

    fun refreshReferences() {
        if (::referenceAdapter.isInitialized) {
            // Don't clear selections - preserve them across tab switches
            loadReferenceImages()
        }
    }
    
    private fun getImageDataById(imageId: String): MainViewModel.ReferenceImage? {
        // Find image data from current loaded images
        return referenceAdapter.getImageById(imageId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // Adapter for reference images
    inner class ReferenceImageAdapter(
        private val onItemClick: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<ReferenceImageAdapter.ViewHolder>() {

        private val images = mutableListOf<MainViewModel.ReferenceImage>()

        inner class ViewHolder(
            private val binding: ItemReferenceImageBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            private var currentImage: MainViewModel.ReferenceImage? = null

            fun bind(image: MainViewModel.ReferenceImage, position: Int) {
                currentImage = image
                val isSelected = viewModel.isImageSelected(image.id)
                
                updateSelectionState(isSelected, animate = false)

                binding.cardView.setOnClickListener {
                    currentImage?.let { img ->
                        val currentlySelected = viewModel.isImageSelected(img.id)
                        val newState = !currentlySelected
                        updateSelectionState(newState, animate = true)
                        onItemClick(img.id, newState)
                    }
                }

                loadImageFromAssets(image.assetPath)
            }
            
            private fun updateSelectionState(isSelected: Boolean, animate: Boolean) {
                binding.cardView.isChecked = isSelected
                
                if (animate) {
                    if (isSelected) {
                        animateSelection()
                    } else {
                        animateDeselection()
                    }
                } else {
                    binding.checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
                    binding.checkIcon.scaleX = if (isSelected) 1f else 0f
                    binding.checkIcon.scaleY = if (isSelected) 1f else 0f
                }
            }
            
            private fun loadImageFromAssets(assetPath: String) {
                try {
                    val inputStream = binding.root.context.assets.open(assetPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageView.setImageBitmap(bitmap)
                    inputStream.close()
                } catch (e: IOException) {
                    binding.imageView.setBackgroundColor(0xFF6200EE.toInt())
                }
            }

            private fun animateSelection() {
                binding.root.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(120)
                    .withEndAction {
                        binding.root.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(80)
                            .withEndAction {
                                binding.root.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start()
                            }
                            .start()
                    }
                    .start()

                binding.checkIcon.apply {
                    visibility = View.VISIBLE
                    scaleX = 0f
                    scaleY = 0f
                    alpha = 0f
                    animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .alpha(1f)
                        .setDuration(150)
                        .withEndAction {
                            animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
            }

            private fun animateDeselection() {
                binding.root.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(80)
                    .withEndAction {
                        binding.root.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start()
                    }
                    .start()
                    
                binding.checkIcon.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        binding.checkIcon.visibility = View.GONE
                        binding.checkIcon.alpha = 1f
                    }
                    .start()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemReferenceImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(images[position], position)
        }

        override fun getItemCount() = images.size

        fun submitList(newImages: List<MainViewModel.ReferenceImage>) {
            images.clear()
            images.addAll(newImages)
            notifyDataSetChanged()
        }
        
        fun getImageById(imageId: String): MainViewModel.ReferenceImage? {
            return images.find { it.id == imageId }
        }
        
        fun clearSelections() {
            notifyDataSetChanged()
        }
    }
    
    // Adapter for selected images in bottom bar
    inner class SelectedImagesAdapter(
        private val onRemoveClick: (String) -> Unit
    ) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {
        
        private val images = mutableListOf<MainViewModel.ReferenceImage>()
        
        inner class ViewHolder(
            private val binding: ItemSelectedImageBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind(image: MainViewModel.ReferenceImage) {
                loadImageFromAssets(image.assetPath)
                
                binding.removeIcon.setOnClickListener {
                    onRemoveClick(image.id)
                }
            }
            
            private fun loadImageFromAssets(assetPath: String) {
                try {
                    val inputStream = binding.root.context.assets.open(assetPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageView.setImageBitmap(bitmap)
                    inputStream.close()
                } catch (e: IOException) {
                    binding.imageView.setBackgroundColor(0xFF6200EE.toInt())
                }
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSelectedImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(images[position])
        }
        
        override fun getItemCount() = images.size
        
        fun submitList(newImages: List<MainViewModel.ReferenceImage>) {
            images.clear()
            images.addAll(newImages)
            notifyDataSetChanged()
        }
    }
}
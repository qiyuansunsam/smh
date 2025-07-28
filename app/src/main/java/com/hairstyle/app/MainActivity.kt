package com.hairstyle.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hairstyle.app.databinding.ActivityMainBinding
import com.hairstyle.app.ui.components.UnifiedBar
import com.hairstyle.app.ui.components.DrawingTools
import com.hairstyle.app.ui.generate.GenerateFragment
import com.hairstyle.app.ui.home.HomeFragment
import com.hairstyle.app.ui.reference.ReferenceFragment
import com.hairstyle.app.viewmodel.MainViewModel
import com.hairstyle.app.viewmodel.DrawingViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            viewModel = ViewModelProvider(this)[MainViewModel::class.java]

            setupNavigation()

            // Start with home fragment
            if (savedInstanceState == null) {
                switchFragment(HomeFragment(), "canvas")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: show a simple layout if data binding fails
            setContentView(android.R.layout.simple_list_item_1)
        }
    }

    private fun setupNavigation() {
        try {
            // Setup UnifiedBar custom component
            val unifiedBar = findViewById<UnifiedBar>(R.id.unifiedBar)
            unifiedBar.setOnTabSelectedListener { tabIndex ->
                when (tabIndex) {
                    UnifiedBar.TAB_REFERENCE -> {
                        // Create main input when leaving canvas
                        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        if (currentFragment is HomeFragment) {
                            currentFragment.createMainInput()
                        }
                        val referenceFragment = ReferenceFragment()
                        switchFragment(referenceFragment, "reference")
                    }
                    UnifiedBar.TAB_CANVAS -> {
                        switchFragment(HomeFragment(), "canvas")
                    }
                    UnifiedBar.TAB_GENERATE -> {
                        // Create main input when leaving canvas
                        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        if (currentFragment is HomeFragment) {
                            currentFragment.createMainInput()
                        }
                        switchFragment(GenerateFragment(), "generate")
                    }
                }
            }
            
            // Automatically trigger canvas selection after a short delay to ensure layout is ready
            unifiedBar.post {
                unifiedBar.postDelayed({
                    // Force canvas selection which will trigger the positioning
                    unifiedBar.selectTab(UnifiedBar.TAB_CANVAS)
                }, 100)
            }

            unifiedBar.setOnSnapshotClickListener { isActive ->
                // Handle snapshot click - trigger camera capture
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is HomeFragment) {
                    currentFragment.takePhoto()
                }
            }
            
            unifiedBar.setOnGenerateClickListener {
                // Handle generate button click - trigger generation for GenerateFragment
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is GenerateFragment) {
                    currentFragment.generateHairStyle()
                }
            }
            
            // Connect drawing tools to ViewModel
            unifiedBar.setOnDrawingToolSelectedListener { tool ->
                // Get the current fragment and update drawing mode if it's HomeFragment
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is HomeFragment) {
                    when (tool) {
                        DrawingTools.Tool.PEN -> {
                            currentFragment.setDrawingMode(DrawingViewModel.DrawingMode.PEN)
                        }
                        DrawingTools.Tool.ERASER -> {
                            currentFragment.setDrawingMode(DrawingViewModel.DrawingMode.ERASER)
                        }
                        DrawingTools.Tool.CLEAR -> {
                            currentFragment.clearDrawing()
                        }
                        DrawingTools.Tool.SETTINGS -> {
                            // Open drawing settings - could show brush size picker, etc.
                        }
                    }
                }
            }
            
            // Connect color picker to drawing
            unifiedBar.getColorPicker()?.setOnColorSelectedListener { color ->
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is HomeFragment) {
                    currentFragment.setDrawingColor(color)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchFragment(fragment: Fragment, tag: String) {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment, tag)
            transaction.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
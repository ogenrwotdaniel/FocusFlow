package com.focusflow.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.focusflow.data.preference.UserPreferences
import com.focusflow.databinding.DialogVolumeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Helper class for creating and managing the volume control dialog.
 * Handles the UI interactions and updates volume settings in UserPreferences.
 */
class VolumeDialogHelper @Inject constructor(
    private val userPreferences: UserPreferences
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * Shows a volume control dialog.
     * @param context Context for creating the dialog
     * @param lifecycleOwner LifecycleOwner to observe volume changes
     * @param onVolumeChanged Callback for when the volume changes
     */
    fun showVolumeDialog(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onVolumeChanged: (Int) -> Unit
    ) {
        val binding = DialogVolumeBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Adjust Volume")
            .setView(binding.root)
            .setPositiveButton("Done") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        // Initialize volume slider with current value from preferences
        coroutineScope.launch {
            val initialVolume = (userPreferences.getVolume().first()).toFloat()
            binding.sliderVolume.value = initialVolume
            
            // Report initial volume to callback
            onVolumeChanged(initialVolume.toInt())
        }
        
        // Set up slider change listener
        binding.sliderVolume.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val volumeValue = value.toInt()
                // Save to preferences
                coroutineScope.launch {
                    userPreferences.updateVolume(volumeValue)
                }
                // Report change to callback
                onVolumeChanged(volumeValue)
            }
        }
        
        dialog.show()
    }
}

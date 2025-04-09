package com.focusflow.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.setPadding
import com.focusflow.R
import com.focusflow.data.model.AudioTrack
import com.focusflow.data.model.AudioTracks
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Helper class for creating and managing the audio track selection dialog.
 * Allows users to choose which background sound to play during focus sessions.
 */
class AudioTrackDialogHelper {
    
    /**
     * Shows a dialog to select audio track for background sound
     * @param context Context for creating the dialog
     * @param currentTrackId ID of the currently selected track
     * @param onTrackSelected Callback for when a track is selected
     */
    fun showAudioTrackDialog(
        context: Context,
        currentTrackId: String,
        onTrackSelected: (String) -> Unit
    ) {
        // Create the radio group for track selection
        val radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(context.resources.getDimensionPixelSize(R.dimen.material_default_spacing))
        }
        
        // Get all available audio tracks
        val tracks = AudioTracks.getAll()
        
        // Add radio buttons for each track
        tracks.forEachIndexed { index, track ->
            val radioButton = RadioButton(context).apply {
                id = index
                text = track.name
                isChecked = track.id == currentTrackId
                setPadding(8, 16, 8, 16)
            }
            radioGroup.addView(radioButton)
        }
        
        // Create and show the dialog
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_background_sound)
            .setView(radioGroup)
            .setPositiveButton(R.string.confirm) { _, _ ->
                // Get the selected track ID
                val selectedIndex = radioGroup.checkedRadioButtonId
                if (selectedIndex != -1) {
                    val selectedTrack = tracks[selectedIndex]
                    onTrackSelected(selectedTrack.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
